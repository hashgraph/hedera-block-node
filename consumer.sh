#!/bin/bash

echo "Starting consumer..."
starting_block=$1
account_id=$2

payload="{\"id\": $starting_block, \"accountId\": \"$account_id\"}"

grpcurl -vv -plaintext -proto ./protos/src/main/protobuf/blockstream.proto -d "$payload" localhost:8080 BlockStreamGrpc/StreamSource

echo "Finished"

