# PowerPoint Parser

A Java application that extracts content from PowerPoint (.pptx) files and converts it to structured JSON format. Built with Apache POI, this tool can parse slides, extract text content, images, and speaker notes, making PowerPoint data easily accessible for further processing.

## Features

- **Complete Content Extraction**: Extracts titles, paragraphs, bullet points, and speaker notes
- **Image Processing**: Saves embedded images to files and references them in JSON output
- **Smart Content Detection**: Automatically identifies titles, bullet points, and regular text
- **Structured JSON Output**: Clean, well-organized JSON format for easy integration
- **Group Shape Support**: Handles nested content within grouped shapes
- **Multiple Deployment Options**: Run natively with Java/Maven or use Docker containers
- **Batch Processing Ready**: Easy to integrate into automated workflows

## Sample Output

```json
{
  "title": "My Presentation",
  "slides": [
    {
      "index": 1,
      "title": "Welcome to Our Product",
      "content": [
        {
          "type": "title",
          "text": "Welcome to Our Product"
        },
        {
          "type": "bullet",
          "text": "Feature 1: Easy to use"
        },
        {
          "type": "bullet",
          "text": "Feature 2: Powerful capabilities"
        }
      ],
      "images": [
        {
          "id": "logo",
          "filename": "slide_1_image_1.png",
          "path": "images/slide_1_image_1.png"
        }
      ],
      "notes": "Remember to emphasize the ease of use when presenting this slide."
    }
  ]
}
```

## Quick Start

### Option 1: Docker (Recommended)

```bash
# Setup the project to use
chmod +x ./setup.sh
./setup.sh

# build
./build.sh
cp your-presentation.pptx input/

# Run parser
./run.sh your-presentation.pptx
```

### Option 2: Native Java

```bash
# Build with Maven
mvn clean package

# Run application
java -jar target/powerpoint-parser-1.0.0-jar-with-dependencies.jar presentation.pptx ./output
```

## Installation & Setup

### Prerequisites

- **Java 21 or higher** (for native execution)

## Usage

### Command Line Arguments

```bash
java -jar powerpoint-parser.jar <input-file> [output-directory]
```

- `<input-file>`: Path to the PowerPoint (.pptx) file to parse
- `[output-directory]`: Optional. Directory where output files will be saved (default: `./out`)

### Docker Usage

#### Basic Docker Run

```bash
# Using helper script (recommended)
./run.sh presentation.pptx

# Direct docker command
docker run --rm \
    -v $(pwd)/input:/app/input:ro \
    -v $(pwd)/output:/app/output:rw \
    powerpoint-parser:latest \
    /app/input/presentation.pptx /app/output
```

## Output Structure

The parser creates the following output structure:

```file
output/
├── presentation.json          # Main JSON output with all extracted content
└── images/                   # Directory containing extracted images
    ├── slide_1_image_1.png
    ├── slide_2_image_1.jpg
    └── slide_3_image_1.png
```

### JSON Schema

```json
{
  "title": "string",              // Presentation title
  "slides": [
    {
      "index": "number",          // Slide number (1-based)
      "title": "string",          // Slide title
      "content": [
        {
          "type": "string",       // "title", "paragraph", or "bullet"
          "text": "string"        // Actual text content
        }
      ],
      "images": [
        {
          "id": "string",         // Image identifier
          "filename": "string",   // Generated filename
          "path": "string"        // Relative path to image file
        }
      ],
      "notes": "string"           // Speaker notes for the slide
    }
  ]
}
```

## Supported Features

### Content Types

- ✅ Slide titles
- ✅ Text paragraphs
- ✅ Bullet points and lists
- ✅ Speaker notes
- ✅ Embedded images (PNG, JPG, GIF, BMP, TIFF)
- ✅ Grouped shapes and nested content
- ⚠️ Tables (basic text extraction)
- ❌ Charts (not yet supported)
- ❌ SmartArt (not yet supported)
