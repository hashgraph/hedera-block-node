#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# Checks that the TSS block proofs a Block Node serves carry WRAPS proofs.
#
# Scans blocks every STEP, waits if the network hasn't produced them yet, and
# reports the first signed block whose signature has the WRAPS layout. A signed
# block with a signature of any other size fails the check: since the TSS library
# embeds its WRAPS parameters, every block proof, including block 0's, carries a
# WRAPS proof and there is no pre-WRAPS signature layout any more.
#
# Proof size (TssSignedBlockProof.block_signature):
#   WRAPS: 13,712 bytes = hinTS verification key (1,096) + hinTS signature (1,248)
#          + compressed WRAPS proof (11,368)
# Blocks proven indirectly (block state proofs) carry no signature and are skipped.
#
# Usage:
#   ./monitor-block-proofs.sh <proto-path> [grpc-endpoint] [max-block] [port-forward-cmd]
#
# Arguments:
#   proto-path        Path to extracted protobuf files (required)
#   grpc-endpoint     Block Node gRPC endpoint (default: localhost:40840)
#   max-block         Stop and fail if this block is reached without a WRAPS proof.
#                     Blocks are produced every ~2s; the script waits patiently
#                     for each block to become available. Also enforces a hard
#                     wall-clock timeout of max_block*2 seconds. (default: 0 = no limit)
#   port-forward-cmd  Optional shell command to restart port-forwards on connection error.
#                     When provided, a connection error triggers this command and retries
#                     instead of failing immediately (max 3 restarts).
#
# Exit codes:
#   0  a WRAPS proof was observed
#   1  max-block reached without a WRAPS proof, a signature of another size seen, or error
#   2  connection error with no port-forward-cmd provided
#   3  hard wall-clock timeout (max_block*2) hit before a WRAPS proof was observed

set -euo pipefail

PROTO_DIR="${1:?Usage: $0 <proto-path> [grpc-endpoint] [max-block] [port-forward-cmd]}"
BN_ENDPOINT="${2:-localhost:40840}"
MAX_BLOCK="${3:-0}"
PORT_FORWARD_CMD="${4:-}"

# Signature length of a TSS block proof carrying a WRAPS proof.
WRAPS_SIGNATURE_BYTES=13712

# Scan stride. Env-overridable so the unit suite can pair a tiny max_block with a
# stride small enough to enter the scan loop at all; production always uses 50.
STEP="${MONITOR_BLOCK_STEP:-50}"
# Max gRPC message size to accept. Normal blocks are ~40KB; the TSS genesis
# block can be ~100MB. Downloading 100MB through kubectl port-forward crashes
# the SPDY tunnel. A 10MB limit rejects oversized blocks early (via RST_STREAM)
# before the data flood kills the port-forward. Rejected blocks are treated as
# NOT_AVAILABLE and skipped.
MAX_MSG_SIZE=10000000
# Per-call wall-clock ceiling for grpcurl. A Service whose pods are gone still
# accepts a kubectl port-forward connection and then stalls forever, so without
# this a single call can block the whole script past the max_block*2
# deadline before check_hard_timeout gets a chance to run. 60s is well clear of a legitimate
# fetch: observed blocks are ~40KB, the largest seen 983.9KB, and MAX_MSG_SIZE
# caps a download at 10MB. On expiry grpcurl reports "context deadline
# exceeded", which the CONNECTION_ERROR pattern below already handles.
MAX_CALL_SECONDS=60

# Hard timeout: max_block * 2s (block production interval).
# If max_block=1000, timeout=2000s (~33 min). 0 means no timeout.
if [[ "$MAX_BLOCK" -gt 0 ]]; then
  HARD_TIMEOUT=$(( MAX_BLOCK * 2 ))
else
  HARD_TIMEOUT=0
fi
START_TIME=$(date +%s)

# Without a trap the TERM from check_hard_timeout exits 143, indistinguishable
# from any other abnormal death. 3 names the self-imposed deadline.
function on_hard_timeout {
  exit 3
}
trap on_hard_timeout TERM

# Checked from get_sig_type_safe (every gRPC call site) rather than only between
# scan iterations, so no call can run unbounded past the deadline.
function check_hard_timeout {
  [[ "$HARD_TIMEOUT" -gt 0 ]] || return 0
  local elapsed=$(( $(date +%s) - START_TIME ))
  if [[ "$elapsed" -ge "$HARD_TIMEOUT" ]]; then
    echo "" >&2
    echo "=== WRAPS Proof Check ===" >&2
    echo "  Status: WRAPS PROOF NOT DETECTED" >&2
    echo "  Hard timeout: ${HARD_TIMEOUT}s elapsed (max_block=${MAX_BLOCK} * 2s)" >&2
    # set -e doesn't reliably cross nested $(...) layers; signal the top-level script.
    # The exit below lets the enclosing $(...) finish so the parent can run its trap.
    kill -TERM "$$"
    exit 1
  fi
}

if ! command -v grpcurl &>/dev/null; then
  echo "ERROR: grpcurl not found" >&2
  exit 1
fi
if [[ ! -d "${PROTO_DIR}" ]]; then
  echo "ERROR: Proto path not found: ${PROTO_DIR}" >&2
  exit 1
fi

# Returns "<TYPE> <sig_bytes> <block_size>"
# e.g. "WRAPS 13712 95432", "STATE_PROOF 0 94521320", "UNKNOWN 2920 95432", "NOT_AVAILABLE 0 0"
# block_size is the approximate binary protobuf size (sum of base64-decoded fields)
function get_sig_type {
  local block_number="$1"
  local raw
  raw=$(cd "${PROTO_DIR}" && grpcurl -plaintext -import-path . \
    -proto "block-node/api/block_access_service.proto" \
    -max-msg-sz "${MAX_MSG_SIZE}" \
    -max-time "${MAX_CALL_SECONDS}" \
    -d "{\"block_number\": ${block_number}}" \
    "${BN_ENDPOINT}" org.hiero.block.api.BlockAccessService/getBlock 2>&1) || true

  if echo "${raw}" | grep -q '"NOT_FOUND"\|"NOT_AVAILABLE"'; then
    echo "NOT_AVAILABLE 0 0"
    return
  fi

  # Connection errors (port-forward down, refused, timeout) -- fail fast
  if echo "${raw}" | grep -qiE "connection refused|failed to dial|context deadline|transport:"; then
    echo "CONNECTION_ERROR 0 0"
    return
  fi

  # Oversized block -- grpcurl rejected it before downloading (port-forward survives)
  if echo "${raw}" | grep -qiE "received message larger than max"; then
    echo "NOT_AVAILABLE 0 0"
    return
  fi

  # If grpcurl returned an error (not valid JSON), report as NOT_AVAILABLE
  if ! echo "${raw}" | python3 -c "import sys,json; json.loads(sys.stdin.read())" 2>/dev/null; then
    echo "NOT_AVAILABLE 0 0"
    return
  fi

  local parsed
  parsed=$(echo "${raw}" | python3 -c "
import sys, json, base64, re
raw_text = sys.stdin.read()
d = json.loads(raw_text)
# Estimate binary protobuf size: decode all base64 values, sum their lengths
# plus a small overhead per field for protobuf framing
b64_pattern = re.compile(r'\"([A-Za-z0-9+/]{4,}={0,2})\"')
binary_size = 0
for match in b64_pattern.finditer(raw_text):
    try:
        binary_size += len(base64.b64decode(match.group(1)))
    except Exception:
        pass
# Add ~10% for protobuf field tags, varints, and non-binary fields
binary_size = int(binary_size * 1.1)
proof_kind = 'NONE'
sig_bytes = 0
for item in d.get('block',{}).get('items',[]):
    if 'blockProof' in item:
        bp = item['blockProof']
        sbp = bp.get('signedBlockProof',{})
        if sbp and sbp.get('blockSignature'):
            proof_kind = 'SIGNED'
            sig_bytes = len(base64.b64decode(sbp['blockSignature']))
        elif bp.get('blockStateProof'):
            proof_kind = 'STATE_PROOF'
        elif bp.get('signedRecordFileProof'):
            proof_kind = 'RECORD_FILE'
        break
print(f'{proof_kind} {sig_bytes} {binary_size}')
" 2>/dev/null) || parsed="NONE 0 0"

  local proof_kind sig_bytes block_size
  read -r proof_kind sig_bytes block_size <<< "${parsed}"

  local sig_type
  if [[ "${proof_kind}" == "SIGNED" ]]; then
    if [[ "${sig_bytes}" -eq "${WRAPS_SIGNATURE_BYTES}" ]]; then
      sig_type="WRAPS"
    else
      sig_type="UNKNOWN"
    fi
  else
    # STATE_PROOF, RECORD_FILE or NONE: no TSS signature to classify on this block
    sig_type="${proof_kind}"
  fi

  echo "${sig_type} ${sig_bytes} ${block_size}"
}

# Wrapper around get_sig_type that recovers from port-forward crashes.
# If a CONNECTION_ERROR is detected and PORT_FORWARD_CMD is set:
#   1. Restarts port-forward
#   2. Retries the request once
#   3. If retry also fails (oversized block crashing port-forward), returns NOT_AVAILABLE
# Without PORT_FORWARD_CMD, CONNECTION_ERROR is returned as-is for the caller to handle.
function get_sig_type_safe {
  check_hard_timeout
  local block_number="$1"
  local result
  result=$(get_sig_type "${block_number}")
  local sig_type="${result%% *}"

  if [[ "${sig_type}" == "CONNECTION_ERROR" && -n "${PORT_FORWARD_CMD}" ]]; then
    echo "  Port-forward lost at block ${block_number}, restarting..." >&2
    eval "${PORT_FORWARD_CMD}" >&2 2>/dev/null || true
    sleep 3
    result=$(get_sig_type "${block_number}")
    sig_type="${result%% *}"
    if [[ "${sig_type}" == "CONNECTION_ERROR" ]]; then
      # Block itself is crashing the port-forward (likely oversized); recover and skip
      echo "  Block ${block_number} crashes port-forward (likely oversized), recovering..." >&2
      eval "${PORT_FORWARD_CMD}" >&2 2>/dev/null || true
      sleep 3
      echo "NOT_AVAILABLE 0 0"
      return
    fi
  fi

  echo "${result}"
}

function format_size {
  local bytes="$1"
  if [[ "$bytes" -ge 1048576 ]]; then
    printf "%.1fMB" "$(echo "$bytes" | awk '{printf "%.1f", $1/1048576}')"
  elif [[ "$bytes" -ge 1024 ]]; then
    printf "%.1fKB" "$(echo "$bytes" | awk '{printf "%.1f", $1/1024}')"
  else
    echo "${bytes}B"
  fi
}

# Main: scan every STEP blocks until a signed block with a WRAPS proof is seen
echo "Scanning for a WRAPS block proof (every ${STEP} blocks)..."
block=0
while true; do
  block=$(( block + STEP ))

  # Enforce max-block limit
  if [[ "$MAX_BLOCK" -gt 0 && "$block" -gt "$MAX_BLOCK" ]]; then
    echo ""
    echo "=== WRAPS Proof Check ==="
    echo "  Status: WRAPS PROOF NOT DETECTED"
    echo "  Max block limit: ${MAX_BLOCK}"
    exit 1
  fi

  result=$(get_sig_type_safe "$block")
  sig_type="${result%% *}"
  sig_bytes=$(echo "$result" | awk '{print $2}')
  block_size=$(echo "$result" | awk '{print $3}')

  # get_sig_type_safe handles port-forward recovery when PORT_FORWARD_CMD is set.
  # If it still returns CONNECTION_ERROR, there's nothing more we can do.
  if [[ "$sig_type" == "CONNECTION_ERROR" ]]; then
    echo ""
    echo "ERROR: Cannot connect to Block Node at ${BN_ENDPOINT} (port-forward may be down)" >&2
    exit 2
  fi

  if [[ "$sig_type" == "NOT_AVAILABLE" ]]; then
    if [[ "${waiting_for:-0}" -ne "$block" ]]; then
      waiting_for=$block
      wait_count=0
      printf "  Block %d: waiting" "$block"
    fi
    (( wait_count++ )) || true
    printf "."
    sleep 5
    # Block production is every ~2s. Wait patiently -- the block will be produced.
    # max_block and the hard wall-clock timeout are what bound the wait.
    block=$(( block - STEP ))
    continue
  fi

  # End the waiting dots line if we were waiting
  if [[ "${waiting_for:-0}" -gt 0 ]]; then
    printf " (available after %ds)\n" $((wait_count * 5))
    waiting_for=0
  fi

  echo "  Block ${block}: ${sig_type} (sig: ${sig_bytes} bytes, block: $(format_size "$block_size"))"

  if [[ "$sig_type" == "WRAPS" ]]; then
    echo ""
    echo "=== WRAPS Proof Check ==="
    echo "  Status: WRAPS PROOF DETECTED"
    echo "  First WRAPS block: ${block}"
    exit 0
  elif [[ "$sig_type" == "UNKNOWN" ]]; then
    echo ""
    echo "=== WRAPS Proof Check ==="
    echo "  Status: UNEXPECTED SIGNATURE LAYOUT"
    echo "  Block ${block} carries a ${sig_bytes} byte TSS signature, expected ${WRAPS_SIGNATURE_BYTES} bytes"
    exit 1
  fi
  # STATE_PROOF, RECORD_FILE or NONE: nothing to classify on this block, keep scanning
done
