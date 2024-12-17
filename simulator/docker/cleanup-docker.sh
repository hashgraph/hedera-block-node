#!/bin/bash

# Exit on any error
set -e

echo "🧹 Cleaning up Docker build artifacts..."

# Get the script's directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Remove simulator tar file
echo "🗑️  Removing simulator distribution tar..."
rm -f "$SCRIPT_DIR"/simulator-*.tar

# Remove block data tar
echo "🗑️  Removing block data tar..."
rm -f "$SCRIPT_DIR"/block-0.0.3.tar.gz

# Remove logging properties
echo "🗑️  Removing logging properties..."
rm -f "$SCRIPT_DIR"/logging.properties

echo "✨ Cleanup completed successfully!"
