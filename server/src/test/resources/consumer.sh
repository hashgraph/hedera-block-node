#!/bin/bash

usage_error() {
  echo "Usage: $0 <integer>"
  exit 1
}

# An integer is expected as the first parameter
if [ "$#" -lt 1 ] || ! [[ "$1" =~ ^[0-9]+$ ]]; then
  usage_error
fi

# If the script reaches here, the parameters are valid
echo "Param is: $1"

# Use environment variables or default values
GRPC_SERVER=${GRPC_SERVER:-"localhost:8080"}
GRPC_METHOD=${GRPC_METHOD:-"BlockStreamGrpcService/subscribeBlockStream"}
PATH_TO_PROTO=${PATH_TO_PROTO:-"../../../../protos/src/main/protobuf/blockstream.proto"}
PROTO_IMPORT_PATH=${PROTO_IMPORT_PATH:-"../../../../protos/src/main/protobuf"}

echo "Starting consumer..."

# Response block messages from the gRPC server are printed to stdout.
echo "{\"start_block_number\": $1}" | grpcurl -plaintext -import-path $PROTO_IMPORT_PATH -proto $PATH_TO_PROTO -d @ $GRPC_SERVER $GRPC_METHOD
