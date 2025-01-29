#!/bin/bash

# Check if the script received an integer argument
if [ -z "$1" ] || ! [[ "$1" =~ ^[0-9]+$ ]]; then
    echo "Usage: $0 <number_of_consumer_containers>"
    exit 1
fi

function stop_containers() {
  # List all running containers with names starting with "block-node-consumer"
  running_containers=$(docker ps --filter "name=^block-node-consumer" --format "{{.ID}} {{.Names}}")

  # Check if any matching containers exist
  if [[ -z "$running_containers" ]]; then
    echo "No running containers found with names starting with 'block-node-consumer'."
  else
    echo "Stopping and removing block-node-consumer containers:"
    echo "$running_containers"

    # Loop through each container and stop/remove it
    while IFS= read -r running_container; do
      container_id=$(echo "$running_container" | awk '{print $1}')
      container_name=$(echo "$running_container" | awk '{print $2}')

      echo "Stopping container: $container_name ($container_id)"
      docker stop "$container_id"
    done <<< "$running_containers"
  fi
}

function remove_containers() {
  stopped_containers=$(docker ps -a --filter "name=^block-node-consumer" --filter "status=exited" --format "{{.ID}} {{.Names}}")
  if [[ -z "$stopped_containers" ]]; then
    echo "No stopped containers found with names starting with 'block-node-consumer'."
  else
    echo "Removing stopped block-node-consumer containers:"
    echo "$stopped_containers"

    while IFS= read -r stopped_container; do
      container_id=$(echo "$stopped_container" | awk '{print $1}')
      container_name=$(echo "$stopped_container" | awk '{print $2}')

      echo "Removing container: $container_name ($container_id)"
      docker rm "$container_id"
    done <<< "$stopped_containers"
  fi
}


stop_containers
remove_containers

echo "Starting $1 consumer container(s)..."
container_count=$1
for ((i = 0; i < container_count; i++)); do
  docker run -d \
             --name block-node-consumer-$i \
             --network block-node_default \
             --env BLOCK_STREAM_SIMULATOR_MODE=CONSUMER \
             --env GRPC_SERVER_ADDRESS=172.19.0.1 \
             hedera-block-simulator
done
