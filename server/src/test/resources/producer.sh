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

generate_header() {
    local number=$1

    # Read the JSON template from the file
    local header_template=$(cat "header_template.json")

    # Interpolate the integer parameter into the JSON template
    local result=$(echo "$header_template" | jq --argjson block_number "$number" '.block_item.header.block_number = $id')

    echo "$result"
}

GRPC_SERVER="localhost:8080"
GRPC_METHOD="BlockStreamGrpcService/publishBlockStream"
#PATH_TO_PROTO="../../../../protos/src/main/protobuf/blockstream.proto"
PATH_TO_PROTO="./block_service.proto"

echo "Starting producer..."

# Signal handler to handle SIGINT (Ctrl+C)
function cleanup {
  echo "Received SIGINT, stopping..."
  kill $GRPC_PID
  exit 0
}

# Trap SIGINT
trap cleanup SIGINT

# Generate and push messages to the gRPC server as a producer.
# Response messages from the gRPC server are printed to stdout.
(
  iter=$1
  block_items=1
  while true; do

    # Generate 10 BlockItems per Block
    for ((i=1; i<=$block_items; i++))
    do

      if [[ $i -eq 1 ]]; then
        result=$(generate_header "$i")
        echo result
      elif [[ $i -eq $block_items ]]; then
        echo "{\"block_item\": {\"state_proof\": {\"block\": $iter},\"value\": \"Payload[...]\"}}"
      else
        echo "{\"block_item\": {\"start_event\": {\"creator_id\": $i},\"value\": \"Payload[...]\"}}"
      fi

      sleep 1.0
    done

    if [ $iter -eq $2 ]; then
      exit 0
    fi
    ((iter++))

  done
) | grpcurl -vv -plaintext -proto $PATH_TO_PROTO -d @ $GRPC_SERVER $GRPC_METHOD &

GRPC_PID=$!

# Wait for grpcurl to finish
wait $GRPC_PID

echo "Finished"
