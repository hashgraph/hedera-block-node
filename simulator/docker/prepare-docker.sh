#!/bin/bash

# Exit on any error
set -e

echo "🚀 Preparing Docker build environment..."

# Get the script's directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/../.."

# Clean and build new distribution
echo "📦 Building simulator distribution..."
cd "$PROJECT_ROOT"
./gradlew clean :simulator:assemble

# Create docker directory if it doesn't exist
mkdir -p "$SCRIPT_DIR"

# Copy simulator distribution
echo "📋 Copying simulator distribution..."
cp simulator/build/distributions/simulator-*.tar "$SCRIPT_DIR/"

# Copy block data
echo "📋 Copying block data..."
cp simulator/src/main/resources/block-0.0.3.tar.gz "$SCRIPT_DIR/"

# Copy logging properties
echo "📋 Copying logging properties..."
cp simulator/src/main/resources/logging.properties "$SCRIPT_DIR/logging.properties"

echo "✅ Docker build environment prepared successfully!"