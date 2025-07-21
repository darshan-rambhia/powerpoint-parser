#!/bin/bash
set -e


echo "Processing PowerPoint directory: $1"
echo "Output directory: ${2:-$OUTPUT_DIR}"

# Run the Java application
exec java $JAVA_OPTS -jar /app/powerpoint-parser.jar "$@"