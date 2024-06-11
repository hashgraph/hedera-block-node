#!/bin/bash


# Function to display usage error
usage_error() {
  echo "Usage: $0 <integer>"
  exit 1
}

# Check if exactly one argument is provided
if [ "$#" -ne 1 ]; then
  usage_error
fi

# Check if the provided argument is an integer
if ! [[ "$1" =~ ^-?[0-9]+$ ]]; then
  usage_error
fi

echo "Generating data..."
limit=${1}

# Initialize iteration number
iteration=0

# Infinite loop
while true; do
    # Increment iteration number
    ((iteration++))
    
    echo "{\"id\": $iteration, \"value\": \"block-stream-$iteration\"}" >> data.txt

    if [ $iteration -eq $limit ]; then
        # Break out of the loop
        break
    fi
    
done


echo "Finished"
