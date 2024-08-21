#!/bin/bash

usage_error() {
  echo "Usage: $0 <integer>"
  exit 1
}

cleanup() {
    echo "Caught SIGINT signal! Terminating the background process..."
    kill "$grp_pid"
    exit 0
}

# An integer is expected as the first parameter
if [ "$#" -lt 1 ] || ! [[ "$1" =~ ^[0-9]+$ ]]; then
  usage_error
fi

trap cleanup SIGINT
trap cleanup SIGTERM

# If the script reaches here, the parameters are valid
echo "Param is: $1"

# Use environment variables or default values
GRPC_SERVER=${GRPC_SERVER:-"localhost:8080"}
GRPC_METHOD=${GRPC_METHOD:-"com.hedera.hapi.block.BlockStreamService/subscribeBlockStream"}
PATH_TO_PROTO="./block_service.proto"

echo "Starting consumer..."

# Response block messages from the gRPC server are printed to stdout.
echo "{\"start_block_number\": $1}" | grpcurl -plaintext -proto $PATH_TO_PROTO -d @ $GRPC_SERVER $GRPC_METHOD &
grp_pid=$!
echo "Started consumer with PID: $grp_pid"

# Wait for the background process to complete
wait "$grp_pid"
