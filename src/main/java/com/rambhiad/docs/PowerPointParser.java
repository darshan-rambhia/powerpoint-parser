package com.rambhiad.docs;

import java.awt.geom.Rectangle2D;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PowerPointParser {

  private final String imageDir;
  private final ObjectMapper objectMapper;
  private static final String DEFAULT_OUTPUT_DIR = "./output";
  private static final String DEFAULT_INPUT_DIR = "./input";

  public PowerPointParser(String outputDir) {
    this.imageDir = Paths.get(outputDir, "images").toString();
    this.objectMapper = new ObjectMapper();
  }

  public static void main(String[] args) {

    PowerPointParser parser = new PowerPointParser(DEFAULT_OUTPUT_DIR);

    try {
      Files.createDirectories(Paths.get(DEFAULT_OUTPUT_DIR));
      Files.list(Paths.get(DEFAULT_INPUT_DIR))
          .filter(
              path -> Files.isRegularFile(path) && path.toString().toLowerCase().endsWith(".pptx"))
          .forEach(pptxPath -> {
            try {
              System.out.println("Processing: " + pptxPath.getFileName());
              PresentationData data = parser.parsePowerPoint(pptxPath.toString());
              String jsonOutput = parser.toJson(data);

              // Write JSON to file named after pptx
              String baseName = pptxPath.getFileName().toString().replaceAll("\\.pptx$", "");
              Path outputPath = Paths.get(DEFAULT_OUTPUT_DIR, baseName + ".json");
              Files.write(outputPath, jsonOutput.getBytes());

              System.out.println("Successfully parsed: " + pptxPath.getFileName());
              System.out.println("Output written to: " + outputPath.toAbsolutePath());
              System.out
                  .println("Images extracted to: " + Paths.get(parser.imageDir).toAbsolutePath());
            } catch (Exception e) {
              System.err.println(
                  "Error parsing file: " + pptxPath.getFileName() + " - " + e.getMessage());
              e.printStackTrace();
            }
          });
    } catch (Exception e) {
      System.err.println("Error processing input directory: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }

  public PresentationData parsePowerPoint(String filePath) throws IOException {
    // Create output directories
    Files.createDirectories(Paths.get(imageDir));

    try (FileInputStream fis = new FileInputStream(filePath);
        XMLSlideShow ppt = new XMLSlideShow(fis)) {

      PresentationData presentation = new PresentationData();

      // Get presentation title (try to extract from slide master or first slide)
      presentation.title = extractPresentationTitle(ppt);

      List<XSLFSlide> slides = ppt.getSlides();
      presentation.slides = new ArrayList<>();

      for (int i = 0; i < slides.size(); i++) {
        XSLFSlide slide = slides.get(i);
        SlideData slideData = parseSlide(slide, i + 1);
        presentation.slides.add(slideData);
      }

      return presentation;
    }
  }

  private String extractPresentationTitle(XMLSlideShow ppt) {
    // Try to get title from document properties
    try {
      if (ppt.getProperties() != null && ppt.getProperties().getCoreProperties() != null
          && ppt.getProperties().getCoreProperties().getTitle() != null) {
        return ppt.getProperties().getCoreProperties().getTitle();
      }
    } catch (Exception e) {
      // Ignore and try other methods
    }

    // Try to get title from first slide
    if (!ppt.getSlides().isEmpty()) {
      XSLFSlide firstSlide = ppt.getSlides().get(0);
      for (XSLFShape shape : firstSlide.getShapes()) {
        if (shape instanceof XSLFTextShape) {
          XSLFTextShape textShape = (XSLFTextShape) shape;
          if (textShape.getShapeType() == ShapeType.TEXT_BOX
              || textShape.getTextType() == Placeholder.TITLE) {
            String text = textShape.getText();
            if (text != null && !text.trim().isEmpty()) {
              return text.trim();
            }
          }
        }
      }
    }

    return "Untitled Presentation";
  }

  private SlideData parseSlide(XSLFSlide slide, int index) throws IOException {
    SlideData slideData = new SlideData();
    slideData.index = index;
    slideData.content = new ArrayList<>();
    slideData.images = new ArrayList<>();

    // Parse shapes on the slide
    for (XSLFShape shape : slide.getShapes()) {
      if (shape instanceof XSLFTextShape) {
        parseTextShape((XSLFTextShape) shape, slideData);
      } else if (shape instanceof XSLFPictureShape) {
        parsePictureShape((XSLFPictureShape) shape, slideData, index);
      } else if (shape instanceof XSLFGroupShape) {
        parseGroupShape((XSLFGroupShape) shape, slideData, index);
      }
    }

    // Parse slide notes
    slideData.notes = parseSlideNotes(slide);

    return slideData;
  }

  private void parseTextShape(XSLFTextShape textShape, SlideData slideData) {
    String text = textShape.getText();
    if (text == null || text.trim().isEmpty()) {
      return;
    }

    // Determine if this is a title based on placeholder type or position
    boolean isTitle = isTitle(textShape);

    if (isTitle && slideData.title == null) {
      slideData.title = text.trim();
    } else {
      // Parse paragraphs within the text shape
      for (XSLFTextParagraph paragraph : textShape.getTextParagraphs()) {
        String paragraphText = paragraph.getText();
        if (paragraphText != null && !paragraphText.trim().isEmpty()) {
          TextContent content = new TextContent();
          content.text = paragraphText.trim();

          // Determine content type based on bullet and indentation
          if (paragraph.isBullet()) {
            content.type = "bullet";
          } else if (isTitle) {
            content.type = "title";
          } else {
            content.type = "paragraph";
          }

          slideData.content.add(content);
        }
      }
    }
  }

  private boolean isTitle(XSLFTextShape textShape) {
    // Check placeholder type
    if (textShape.getTextType() == Placeholder.TITLE
        || textShape.getTextType() == Placeholder.CENTERED_TITLE) {
      return true;
    }

    // Check shape name
    String shapeName = textShape.getShapeName();
    if (shapeName != null && (shapeName.toLowerCase().contains("title")
        || shapeName.toLowerCase().contains("heading"))) {
      return true;
    }

    // Check position (titles are usually at the top)
    Rectangle2D anchor = textShape.getAnchor();
    return anchor.getY() < 100; // Assume titles are in top 100 points
  }

  private void parsePictureShape(XSLFPictureShape pictureShape, SlideData slideData, int slideIndex)
      throws IOException {
    XSLFPictureData pictureData = pictureShape.getPictureData();
    if (pictureData != null) {
      String extension = getImageExtension(pictureData.getContentType());
      String filename =
          String.format("slide_%d_image_%d.%s", slideIndex, slideData.images.size() + 1, extension);

      Path imagePath = Paths.get(imageDir, filename);

      // Save image to file
      try (FileOutputStream fos = new FileOutputStream(imagePath.toFile())) {
        fos.write(pictureData.getData());
      }

      ImageRef imageRef = new ImageRef();
      imageRef.id = pictureShape.getShapeName() != null ? pictureShape.getShapeName()
          : "image_" + (slideData.images.size() + 1);
      imageRef.filename = filename;
      imageRef.path = "images/" + filename;

      slideData.images.add(imageRef);
    }
  }

  private void parseGroupShape(XSLFGroupShape groupShape, SlideData slideData, int slideIndex)
      throws IOException {
    // Recursively parse shapes in the group
    for (XSLFShape shape : groupShape.getShapes()) {
      if (shape instanceof XSLFTextShape) {
        parseTextShape((XSLFTextShape) shape, slideData);
      } else if (shape instanceof XSLFPictureShape) {
        parsePictureShape((XSLFPictureShape) shape, slideData, slideIndex);
      } else if (shape instanceof XSLFGroupShape) {
        parseGroupShape((XSLFGroupShape) shape, slideData, slideIndex);
      }
    }
  }

  private String parseSlideNotes(XSLFSlide slide) {
    XSLFNotes notes = slide.getNotes();
    if (notes == null) {
      return "";
    }

    StringBuilder notesText = new StringBuilder();
    for (XSLFShape shape : notes.getShapes()) {
      if (shape instanceof XSLFTextShape) {
        XSLFTextShape textShape = (XSLFTextShape) shape;
        // Skip the slide image placeholder in notes
        if (textShape.getTextType() != Placeholder.SLIDE_IMAGE) {
          String text = textShape.getText();
          if (text != null && !text.trim().isEmpty()) {
            if (notesText.length() > 0) {
              notesText.append("\n");
            }
            notesText.append(text.trim());
          }
        }
      }
    }

    return notesText.toString();
  }

  private String getImageExtension(String contentType) {
    switch (contentType.toLowerCase()) {
      case "image/png":
        return "png";
      case "image/jpeg":
      case "image/jpg":
        return "jpg";
      case "image/gif":
        return "gif";
      case "image/bmp":
        return "bmp";
      case "image/tiff":
        return "tiff";
      default:
        return "png"; // Default to PNG
    }
  }

  public String toJson(PresentationData data) throws IOException {
    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
  }

  // Data classes for JSON output
  public static class PresentationData {
    @JsonProperty("title")
    public String title;

    @JsonProperty("slides")
    public List<SlideData> slides;
  }

  public static class SlideData {
    @JsonProperty("index")
    public int index;

    @JsonProperty("title")
    public String title;

    @JsonProperty("content")
    public List<TextContent> content;

    @JsonProperty("images")
    public List<ImageRef> images;

    @JsonProperty("notes")
    public String notes;
  }

  public static class TextContent {
    @JsonProperty("type")
    public String type; // "paragraph", "bullet", "title"

    @JsonProperty("text")
    public String text;
  }

  public static class ImageRef {
    @JsonProperty("id")
    public String id;

    @JsonProperty("filename")
    public String filename;

    @JsonProperty("path")
    public String path;
  }
}
