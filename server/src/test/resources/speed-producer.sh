#!/bin/bash

echo "Starting producer..."

GRPC_SERVER="localhost:8080"
GRPC_METHOD="BlockStreamGrpc/StreamSink"
PATH_TO_PROTO="../../../../protos/src/main/protobuf/blockstream.proto"

grpcurl -plaintext -proto $PATH_TO_PROTO -d @ $GRPC_SERVER $GRPC_METHOD < data.txt

echo "Finished"

