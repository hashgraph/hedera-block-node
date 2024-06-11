#!/bin/bash

echo "Starting producer..."

grpcurl -vv -plaintext -d @ -proto ./protos/src/main/protobuf/blockstream.proto localhost:8080 BlockStreamGrpc/StreamSink < data.txt

echo "Finished"

