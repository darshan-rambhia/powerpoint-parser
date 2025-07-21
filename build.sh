#!/bin/bash
# build.sh - Build the PowerPoint Parser Docker image

set -e

echo "Building PowerPoint Parser Docker image..."

# Build the image
docker-compose build powerpoint-parser

echo "Build completed successfully!"
echo "Image: powerpoint-parser:latest"

# Show image size
echo ""
echo "Image information:"
docker images powerpoint-parser:latest
