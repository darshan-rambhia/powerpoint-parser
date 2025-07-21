#!/bin/bash
# compose-run.sh - Run with Docker Compose

set -e

# Create directories
mkdir -p input output

echo "Running PowerPoint parser with Docker Compose..."


# Run with compose
docker-compose run --remove-orphans powerpoint-parser /app/input /app/output

echo "Processing completed! Check the ./output directory"
