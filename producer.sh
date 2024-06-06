#!/bin/bash

echo "Starting producer..."

# Initialize iteration number
iteration=0
limit=50

# Infinite loop
while true; do
    # Increment iteration number
    ((iteration++))
    
    # Echo iteration number
    echo "Iteration: $iteration"

    payload="{\"id\": $iteration, \"value\": \"block-stream-$iteration\"}"
    echo "payload: $payload"

    grpcurl -vv -plaintext -proto ./protos/src/main/protobuf/blockstream.proto -d "$payload" localhost:8080 BlockStreamGrpc/StreamSink
    if [ $iteration -eq $limit ]; then
        # Break out of the loop
        break
    fi
    
    # Wait for 1 second
    sleep 1
done


echo "Finished"
