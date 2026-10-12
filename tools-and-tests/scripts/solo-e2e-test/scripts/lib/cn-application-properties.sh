#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# Shared generator for the CN application.properties override file.
# Sourced by solo-deploy-network.sh (deploy-time) and
# wrb-distribution/cn-upgrade-tss.sh (upgrade-time) so both paths
# produce identical properties from a single source of truth.
#
# Reads:
#   TSS_ENABLED  : "true" enables tss.hintsEnabled/historyEnabled (and, for a CN
#                  that still needs the WRAPS proving key, tss.wrapsEnabled and
#                  tss.wrapsProvingKeyDownloadEnabled)
#   WRB_RSA      : "true" appends blockStream.streamWrappedRecordBlocks=true
#
# Usage: generate_cn_application_properties <output-file> [<cn-version>]
#   The optional CN version decides whether the WRAPS proving key properties are
#   written (see cn_needs_wraps_proving_key); without it they are written.

# The first Consensus Node release whose TSS library (hedera-cryptography 3.18.0)
# embeds the WRAPS parameters: such a node stages no proving key and no longer
# defines the tss.wrapsEnabled / tss.wrapsProvingKey* properties.
# TODO(#3689): confirm once the Consensus Node release adopting the library is cut.
CN_EMBEDDED_WRAPS_MIN_VERSION="${CN_EMBEDDED_WRAPS_MIN_VERSION:-0.80.0}"

# cn_needs_wraps_proving_key <cn-version>
# Returns 0 (true) when the given Consensus Node version predates the embedded
# WRAPS parameters and so still needs the proving key staged via Solo and the
# tss.wraps* properties. A version that is not a plain tag (empty, a keyword
# such as "rc", or a -SNAPSHOT) is treated as needing the key, which keeps the
# legacy behaviour for anything the caller could not resolve.
cn_needs_wraps_proving_key() {
  local cn_version="${1:-}"
  cn_version="${cn_version#v}"
  local base="${cn_version%%-*}"
  if [[ ! "${base}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    return 0
  fi
  local a_major a_minor a_patch b_major b_minor b_patch
  IFS='.' read -r a_major a_minor a_patch <<< "${base}"
  IFS='.' read -r b_major b_minor b_patch <<< "${CN_EMBEDDED_WRAPS_MIN_VERSION}"
  if (( a_major != b_major )); then
    (( a_major < b_major ))
  elif (( a_minor != b_minor )); then
    (( a_minor < b_minor ))
  else
    (( a_patch < b_patch ))
  fi
}

generate_cn_application_properties() {
  local output_file="${1}"
  local cn_version="${2:-}"
  cat > "${output_file}" << 'EOF'
hedera.config.version=0
ledger.id=0x01
netty.mode=TEST
contracts.chainId=298
hedera.recordStream.logPeriod=1
balances.exportPeriodSecs=400
files.maxSizeKb=2048
hedera.recordStream.compressFilesOnCreation=true
balances.compressOnCreation=true
contracts.maxNumWithHapiSigsAccess=0
autoRenew.targetTypes=
nodes.gossipFqdnRestricted=false
hedera.profiles.active=TEST
nodes.updateAccountIdAllowed=true
blockStream.streamMode=BOTH
# TODO: we can remove this after we no longer need less than v0.59.x
networkAdmin.exportCandidateRoster=true
# for v0.59+, write the network.json file when you freeze the network
networkAdmin.diskNetworkExport=ONLY_FREEZE_BLOCK
hedera.realm=0
hedera.shard=0
nodes.webProxyEndpointsEnabled=true
nodes.nodeRewardsEnabled=false

blockStream.writerMode=FILE_AND_GRPC

blockNode.connectionStallThresholdMillis=5000
EOF

  # TSS on/off is independent of WRB streaming: rsa-wrb (WRB_RSA) always
  # implies TSS_ENABLED=false (see load_topology) and additionally wants WRB
  # streaming instead of the normal record stream, whereas a plain
  # --tss-enabled false deploy (e.g. to test a mid-life TSS cutover via
  # cn-upgrade-tss.sh, which is what actually turns TSS on for that scenario —
  # issue #3125 step 11) wants TSS off at genesis but the normal record stream
  # still on, since steps 1-10 consume record files, not WRB.
  if [[ "${TSS_ENABLED}" != "true" ]]; then
    cat >> "${output_file}" << 'EOF'

tss.hintsEnabled=false
tss.historyEnabled=false
tss.forceMockSignatures=false
tss.wrapsEnabled=false
EOF
  else
    cat >> "${output_file}" << 'EOF'

tss.hintsEnabled=true
tss.historyEnabled=true
tss.forceMockSignatures=false
EOF
    # A CN whose TSS library embeds the WRAPS parameters no longer defines these
    # properties; older CNs need them to load the proving key Solo stages.
    if cn_needs_wraps_proving_key "${cn_version}"; then
      cat >> "${output_file}" << 'EOF'
tss.wrapsEnabled=true
# CN <= v0.77.x defaults this to false, which also skips verifying and
# extracting the tarball Solo pre-stages at data/keys/wraps.tar.gz, so WRAPS
# never starts. Its hash matches tss.wrapsProvingKeyHash, so nothing is downloaded.
tss.wrapsProvingKeyDownloadEnabled=true
EOF
    fi
  fi

  if [[ "${WRB_RSA}" == "true" ]]; then
    cat >> "${output_file}" << 'EOF'

blockStream.streamWrappedRecordBlocks=true
EOF
  else
    cat >> "${output_file}" << 'EOF'

blockStream.streamWrappedRecordBlocks=false
EOF
  fi
}
