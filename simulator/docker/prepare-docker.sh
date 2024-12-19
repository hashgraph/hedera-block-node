#!/bin/bash

# Exit on any error
set -e

echo "🚀 Preparing Docker build environment..."

# Get the script's directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/../../.."

# Create docker directory if it doesn't exist
mkdir -p "$SCRIPT_DIR"

# Copy simulator distribution
echo "📋 Copying simulator distribution..."
cp ../distributions/simulator-*.tar "$SCRIPT_DIR/"

# Copy block data
echo "📋 Copying block data..."
cp ../../src/main/resources/block-0.0.3.tar.gz "$SCRIPT_DIR/"

# Copy logging properties
echo "📋 Copying logging properties..."
cp ../../src/main/resources/logging.properties "$SCRIPT_DIR/logging.properties"

sh -c ./update-env.sh

echo "✅ Docker build environment prepared successfully!"

docker compose -p simulator up --build -d

echo "✅ Docker images successfully built and started!"
