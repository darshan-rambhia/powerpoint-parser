# setup.sh - Initial setup script

set -e

echo "Setting up PowerPoint Parser environment..."

# Create directory structure
mkdir -p input output src/main/java

# Create sample input directory structure
if [ ! -d "input" ] || [ -z "$(ls -A input)" ]; then
    echo "Created input directory. Place your .pptx files here."
fi

# Make scripts executable
chmod +x build.sh run.sh

echo ""
echo "Setup completed!"
echo ""
echo "Next steps:"
echo "1. Place your PowerPoint files in the ./input directory"
echo "2. Build the Docker image: ./build.sh"
echo "3. Run the parser: ./run.sh your-file.pptx"
echo ""
echo "Available commands:"
echo "  ./build.sh          - Build Docker image"
echo "  ./run.sh <file>     - Run parser on all files in input directory"