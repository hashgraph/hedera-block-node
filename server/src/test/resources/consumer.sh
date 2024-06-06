#!/bin/bash

usage_error() {
  echo "Usage: $0 <integer> [positive-integer]"
  exit 1
}

# Check if the first argument is provided and is an integer
if [ "$#" -lt 1 ] || ! [[ "$1" =~ ^[0-9]+$ ]]; then
  usage_error
fi

# Check if the second argument is provided and if it's a positive integer
if [ "$#" -eq 2 ] && ! [[ "$2" =~ ^[1-9][0-9]*$ ]]; then
  usage_error
fi

# If the script reaches here, the parameters are valid
echo "The provided integer is: $1"
if [ "$#" -eq 2 ]; then
  echo "The optional positive integer is: $2"
fi

GRPC_SERVER="localhost:8080"
GRPC_METHOD="BlockStreamGrpc/StreamSource"
PATH_TO_PROTO="../../../../protos/src/main/protobuf/blockstream.proto"

echo "Starting consumer..."

# Signal handler to handle SIGINT (Ctrl+C)
function cleanup {
  echo "Received SIGINT, stopping..."
  kill $GRPC_PID
  exit 0
}

# Trap SIGINT
trap cleanup SIGINT

# Generate and push messages to the gRPC server as a consumer.
# Response block messages from the gRPC server are printed to stdout.
(
  iter=$1
  while true; do
    echo "{\"id\": $iter}"

    if [ $iter -eq $2 ]; then
      exit 0
    fi

    ((iter++))

    # Configure the message speed
    sleep 1

  done
) | grpcurl -plaintext -proto $PATH_TO_PROTO -d @ $GRPC_SERVER $GRPC_METHOD

