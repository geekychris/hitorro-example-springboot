package com.hitorro.example.springboot;

import com.hitorro.basedms.transformer.TransformerService;
import com.hitorro.basedms.transformer.TransformMethod;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.basefile.fs.file.FileFile;
import com.hitorro.util.basefile.fs.file.FileFileSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for the new transformer methods.
 * These tests demonstrate how to use each transformer programmatically.
 * Tests are skipped if the required tools are not installed.
 */
@SpringBootTest
public class TransformerIntegrationTest {

    private TransformerService transformerService;
    private File tempDir;

    @BeforeEach
    public void setUp() throws IOException {
        transformerService = TransformerService.getService();
        assertThat(transformerService).isNotNull();
        
        // Create temp directory for test files
        tempDir = Files.createTempDirectory("transformer-test").toFile();
        tempDir.deleteOnExit();
    }


    @Test
    public void testPDFToTextTransformer() throws Exception {
        TransformMethod method = transformerService.getMethod("pdf_to_text");
        assumeTrue(method != null, "PDFToText transformer not available");
        assumeTrue(method.ensureServiceAvailable(), "pdftotext tool not installed");

        // Use test PDF from resources
        File testPdf = new File(getClass().getResource("/test.pdf").toURI());
        
        FileFileSystem ffs = new FileFileSystem(testPdf.getParentFile());
        BaseFile sourceFile = ffs.getFile("test.pdf");

        BaseFile result = method.convert(sourceFile, "test1", "layout=true", null, 5);

        assertThat(result).isNotNull();
        assertThat(result.getName()).endsWith(".txt");
        
        // Verify content was extracted
        File resultFile = ((FileFile) result).getJavaFile();
        String content = new String(Files.readAllBytes(resultFile.toPath()));
        assertThat(content).contains("Hitorro");
    }



    @Test
    public void testPostScriptToPDFTransformer() throws Exception {
        TransformMethod method = transformerService.getMethod("ps_to_pdf");
        assumeTrue(method != null, "PostScriptToPDF transformer not available");
        assumeTrue(method.ensureServiceAvailable(), "Ghostscript not installed");

        // Create a simple PostScript file
        File testPs = new File(tempDir, "test.ps");
        try (FileWriter writer = new FileWriter(testPs)) {
            writer.write("%!PS-Adobe-3.0\n");
            writer.write("%%BoundingBox: 0 0 612 792\n");
            writer.write("/Times-Roman findfont 24 scalefont setfont\n");
            writer.write("100 700 moveto\n");
            writer.write("(Hello, Hitorro Transformers!) show\n");
            writer.write("showpage\n");
        }

        FileFileSystem ffs = new FileFileSystem(tempDir);
        BaseFile sourceFile = ffs.getFile("test.ps");

        BaseFile result = method.convert(sourceFile, "test2", "quality=ebook,compatibility=1.4", null, 5);

        assertThat(result).isNotNull();
        assertThat(result.getName()).endsWith(".pdf");
    }

    @Test
    public void testOCRTransformer() throws Exception {
        TransformMethod method = transformerService.getMethod("ocr");
        assumeTrue(method != null, "OCR transformer not available");
        assumeTrue(method.ensureServiceAvailable(), "Tesseract not installed");

        // Use test image from resources
        File testImage = new File(getClass().getResource("/test-image.jpg").toURI());
        
        FileFileSystem ffs = new FileFileSystem(testImage.getParentFile());
        BaseFile sourceFile = ffs.getFile("test-image.jpg");

        BaseFile result = method.convert(sourceFile, "test3", "lang=eng,output=txt,psm=3", null, 5);

        assertThat(result).isNotNull();
        assertThat(result.getName()).endsWith(".txt");
        
        // Verify OCR extracted text
        File resultFile = ((FileFile) result).getJavaFile();
        String content = new String(Files.readAllBytes(resultFile.toPath()));
        assertThat(content).containsIgnoringCase("Hitorro");
    }


    @Test
    public void testVideoThumbnailTransformer() throws Exception {
        TransformMethod method = transformerService.getMethod("video_thumbnail");
        assumeTrue(method != null, "VideoThumbnail transformer not available");
        assumeTrue(method.ensureServiceAvailable(), "FFmpeg not installed");

        // Use test video from resources
        File testVideo = new File(getClass().getResource("/test-video.mp4").toURI());
        
        FileFileSystem ffs = new FileFileSystem(testVideo.getParentFile());
        BaseFile sourceFile = ffs.getFile("test-video.mp4");

        BaseFile result = method.convert(sourceFile, "test4", "timestamp=00:00:01,width=640,format=jpg", null, 5);

        assertThat(result).isNotNull();
        assertThat(result.getName()).endsWith(".jpg");
        
        // Verify thumbnail was created and has reasonable size
        File resultFile = ((FileFile) result).getJavaFile();
        assertThat(resultFile.length()).isGreaterThan(1000); // At least 1KB
    }


    @Test
    public void testPDFCompressTransformer() throws Exception {
        TransformMethod method = transformerService.getMethod("pdf_compress");
        assumeTrue(method != null, "PDFCompress transformer not available");
        assumeTrue(method.ensureServiceAvailable(), "Ghostscript not installed");

        // Use test PDF from resources
        File testPdf = new File(getClass().getResource("/test.pdf").toURI());
        
        FileFileSystem ffs = new FileFileSystem(testPdf.getParentFile());
        BaseFile sourceFile = ffs.getFile("test.pdf");

        BaseFile result = method.convert(sourceFile, "test5", "quality=screen,grayscale=false", null, 5);

        assertThat(result).isNotNull();
        assertThat(result.getName()).endsWith(".pdf");
        
        // Verify compressed PDF exists
        File resultFile = ((FileFile) result).getJavaFile();
        assertThat(resultFile.exists()).isTrue();
    }


    @Test
    public void testHTMLToTextTransformer() throws Exception {
        TransformMethod method = transformerService.getMethod("html_to_text");
        assumeTrue(method != null, "HTMLToText transformer not available");
        assumeTrue(method.ensureServiceAvailable(), "html2text or lynx not installed");

        // Create a simple HTML file
        File testHtml = new File(tempDir, "test.html");
        try (FileWriter writer = new FileWriter(testHtml)) {
            writer.write("<!DOCTYPE html>\n");
            writer.write("<html>\n");
            writer.write("<head><title>Test Page</title></head>\n");
            writer.write("<body>\n");
            writer.write("<h1>Hitorro Transformers Test</h1>\n");
            writer.write("<p>This is a test of the <strong>HTML to Text</strong> transformer.</p>\n");
            writer.write("<ul>\n");
            writer.write("<li>Feature 1: PDF processing</li>\n");
            writer.write("<li>Feature 2: OCR support</li>\n");
            writer.write("<li>Feature 3: Video thumbnails</li>\n");
            writer.write("</ul>\n");
            writer.write("</body>\n");
            writer.write("</html>\n");
        }

        FileFileSystem ffs = new FileFileSystem(tempDir);
        BaseFile sourceFile = ffs.getFile("test.html");

        BaseFile result = method.convert(sourceFile, "test6", "width=80,links=inline", null, 5);

        assertThat(result).isNotNull();
        assertThat(result.getName()).endsWith(".txt");
        
        // Verify content was extracted
        File resultFile = ((FileFile) result).getJavaFile();
        String content = new String(Files.readAllBytes(resultFile.toPath()));
        assertThat(content).contains("Hitorro Transformers Test");
        assertThat(content).contains("HTML to Text");
    }

    @Test
    public void testMarkdownToHTMLTransformer() throws Exception {
        TransformMethod method = transformerService.getMethod("markdown_to_html");
        assumeTrue(method != null, "MarkdownToHTML transformer not available");
        assumeTrue(method.ensureServiceAvailable(), "Pandoc not installed");

        // Create a Markdown file
        File testMd = new File(tempDir, "test.md");
        try (FileWriter writer = new FileWriter(testMd)) {
            writer.write("# Hitorro Transformers\n\n");
            writer.write("## Overview\n\n");
            writer.write("The Hitorro TransformerService provides **comprehensive** content transformation.\n\n");
            writer.write("### Features\n\n");
            writer.write("1. PDF processing (text extraction, compression)\n");
            writer.write("2. OCR for scanned documents\n");
            writer.write("3. Video thumbnail generation\n");
            writer.write("4. Document format conversion\n\n");
            writer.write("```java\n");
            writer.write("TransformMethod method = transformerService.getMethod(\"markdown_to_html\");\n");
            writer.write("BaseFile result = method.convert(sourceFile, \"id\", \"standalone=true\", null, 5);\n");
            writer.write("```\n\n");
            writer.write("For more information, visit [Hitorro Documentation](https://hitorro.com).\n");
        }

        FileFileSystem ffs = new FileFileSystem(tempDir);
        BaseFile sourceFile = ffs.getFile("test.md");

        BaseFile result = method.convert(sourceFile, "test7", "standalone=true,toc=true,title=Test Document", null, 5);

        assertThat(result).isNotNull();
        assertThat(result.getName()).endsWith(".html");
        
        // Verify HTML was generated
        File resultFile = ((FileFile) result).getJavaFile();
        String content = new String(Files.readAllBytes(resultFile.toPath()));
        assertThat(content).contains("<html");
        assertThat(content).contains("Hitorro Transformers");
        assertThat(content).contains("<code");  // Code block should be highlighted
    }

    @Test
    public void testPDFTemplateTransformerJSON() throws Exception {
        TransformMethod method = transformerService.getMethod("pdf_template");
        assumeTrue(method != null, "PDFTemplate transformer not available");
        assumeTrue(method.ensureServiceAvailable(), "pdftk not installed");

        // Use dynamically created form PDF
        File testPdf = createTestFormPDF();
        
        FileFileSystem ffs = new FileFileSystem(tempDir);
        BaseFile sourceFile = ffs.getFile("test-form.pdf");

        // Parameters with proper JSON syntax
        String params = "{\"variables\":{\"full_name\":\"John Doe\"},\"flatten\":true}";
        BaseFile result = method.convert(sourceFile, "test_json", params, null, 5);

        assertThat(result).isNotNull();
        assertThat(result.getName()).endsWith(".pdf");
        
        File resultFile = ((FileFile) result).getJavaFile();
        assertThat(resultFile.exists()).isTrue();
        assertThat(resultFile.length()).isGreaterThan(0);

        // Verify content by extracting text
        TransformMethod pdfToText = transformerService.getMethod("pdf_to_text");
        if (pdfToText != null && pdfToText.ensureServiceAvailable()) {
            BaseFile textResult = pdfToText.convert(result, "verify_json", "layout=true", null, 5);
            File textFile = ((FileFile) textResult).getJavaFile();
            String content = new String(Files.readAllBytes(textFile.toPath()));
            assertThat(content).as("PDF should contain the filled value").contains("John Doe");
        }
    }

    @Test
    public void testPDFTemplateTransformerCSV() throws Exception {
        TransformMethod method = transformerService.getMethod("pdf_template");
        assumeTrue(method != null, "PDFTemplate transformer not available");
        assumeTrue(method.ensureServiceAvailable(), "pdftk not installed");

        // Use dynamically created form PDF
        File testPdf = createTestFormPDF();
        
        FileFileSystem ffs = new FileFileSystem(tempDir);
        BaseFile sourceFile = ffs.getFile("test-form.pdf");

        // Parameters with legacy CSV syntax: key1=val1,key2=val2
        // Note: PDFTemplateTransformer expects full_name=John Doe to be part of 'variables'
        // In CSV mode, it will pick up full_name if it's passed directly as a param
        String params = "full_name=Jane Doe,flatten=true";
        BaseFile result = method.convert(sourceFile, "test_csv", params, null, 5);

        assertThat(result).isNotNull();
        assertThat(result.getName()).endsWith(".pdf");
        
        File resultFile = ((FileFile) result).getJavaFile();
        assertThat(resultFile.exists()).isTrue();

        // Verify content by extracting text
        TransformMethod pdfToText = transformerService.getMethod("pdf_to_text");
        if (pdfToText != null && pdfToText.ensureServiceAvailable()) {
            BaseFile textResult = pdfToText.convert(result, "verify_csv", "layout=true", null, 5);
            File textFile = ((FileFile) textResult).getJavaFile();
            String content = new String(Files.readAllBytes(textFile.toPath()));
            assertThat(content).as("PDF should contain the filled value (legacy)").contains("Jane Doe");
        }
    }

    @Test
    public void testPDFTemplateTransformerWithExternalTemplate() throws Exception {
        TransformMethod method = transformerService.getMethod("pdf_template");
        assumeTrue(method != null, "PDFTemplate transformer not available");
        assumeTrue(method.ensureServiceAvailable(), "pdftk not installed");

        // Create a template PDF
        File templatePdf = createTestFormPDF();
        
        // Create a JSON data file
        File jsonData = new File(tempDir, "data.json");
        try (FileWriter writer = new FileWriter(jsonData)) {
            writer.write("{\"full_name\":\"External Template Doe\"}");
        }

        FileFileSystem ffs = new FileFileSystem(tempDir);
        BaseFile dataFile = ffs.getFile("data.json");
        
        // Simulating the async parameter passing from TransformerUtil
        String params = "_template_path=" + templatePdf.getAbsolutePath() + ",flatten=true";
        BaseFile result = method.convert(dataFile, "test_external", params, null, 5);

        assertThat(result).isNotNull();
        assertThat(result.getName()).endsWith(".pdf");
        
        File resultFile = ((FileFile) result).getJavaFile();
        assertThat(resultFile.exists()).isTrue();

        // Verify content by extracting text
        TransformMethod pdfToText = transformerService.getMethod("pdf_to_text");
        if (pdfToText != null && pdfToText.ensureServiceAvailable()) {
            BaseFile textResult = pdfToText.convert(result, "verify_external", "layout=true", null, 5);
            File textFile = ((FileFile) textResult).getJavaFile();
            String content = new String(Files.readAllBytes(textFile.toPath()));
            assertThat(content).as("PDF should contain the filled value from external data").contains("External Template Doe");
        }
    }

    /**
     * Helper method to create a PDF with a simple form field using PDFBox.
     * The form field is named "full_name".
     *
     * @return The created PDF file.
     * @throws IOException If an error occurs during PDF creation.
     */
    private File createTestFormPDF() throws IOException {
        File formPdf = new File(tempDir, "test-form.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            PDAcroForm acroForm = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(acroForm);

            // Add a text field
            PDTextField textField = new PDTextField(acroForm);
            textField.setPartialName("full_name");
            textField.setDefaultAppearance("/Helv 12 Tf 0 g");
            textField.setAlternateFieldName("Full Name");

            // Set the widget annotation for the text field
            PDAnnotationWidget widget = textField.getWidgets().get(0);
            PDRectangle rect = new PDRectangle(50, 700, 200, 25); // x, y, width, height
            widget.setRectangle(rect);
            widget.setPage(page);
            widget.setPrinted(true);

            // PDFBox 3.x Font handling
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            acroForm.setDefaultAppearance("/Helv 12 Tf 0 g");
            acroForm.setNeedAppearances(true);

            acroForm.getFields().add(textField);
            page.getAnnotations().add(widget);

            // Add some text to indicate the field
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(font, 12);
                contentStream.newLineAtOffset(50, 730);
                contentStream.showText("Full Name:");
                contentStream.endText();
            }

            document.save(formPdf);
        }
        return formPdf;
    }

    @Test
    public void testAllTransformersRegistered() {
        // Verify core transformers are registered (these should always be available)
        assertThat(transformerService.getMethod("pdf_to_image")).as("pdf_to_image should be registered").isNotNull();
        assertThat(transformerService.getMethod("libreoffice_convert")).as("libreoffice_convert should be registered").isNotNull();
        assertThat(transformerService.getMethod("imagemagick_convert")).as("imagemagick_convert should be registered").isNotNull();
        
        // New Tier 1 transformers - check if registered (may not be if tools aren't installed)
        TransformMethod pdfToText = transformerService.getMethod("pdf_to_text");
        TransformMethod psToPdf = transformerService.getMethod("ps_to_pdf");
        TransformMethod ocr = transformerService.getMethod("ocr");
        TransformMethod videoThumb = transformerService.getMethod("video_thumbnail");
        
        // New Tier 2 transformers - check if registered (may not be if tools aren't installed)
        TransformMethod pdfCompress = transformerService.getMethod("pdf_compress");
        TransformMethod htmlToText = transformerService.getMethod("html_to_text");
        TransformMethod markdownToHtml = transformerService.getMethod("markdown_to_html");
        TransformMethod pdfTemplate = transformerService.getMethod("pdf_template");
        
        // Log which transformers are available
        System.out.println("=== Transformer Availability ===");
        System.out.println("pdf_to_text: " + (pdfToText != null ? "✓" : "✗"));
        System.out.println("ps_to_pdf: " + (psToPdf != null ? "✓" : "✗"));
        System.out.println("ocr: " + (ocr != null ? "✓" : "✗"));
        System.out.println("video_thumbnail: " + (videoThumb != null ? "✓" : "✗"));
        System.out.println("pdf_compress: " + (pdfCompress != null ? "✓" : "✗"));
        System.out.println("html_to_text: " + (htmlToText != null ? "✓" : "✗"));
        System.out.println("markdown_to_html: " + (markdownToHtml != null ? "✓" : "✗"));
        System.out.println("pdf_template: " + (pdfTemplate != null ? "✓" : "✗"));
        System.out.println("================================");
        
        // At least verify that the transformer classes exist and can be instantiated
        // (even if the tools aren't installed)
        assertThat(pdfToText).as("pdf_to_text transformer class should exist").isNotNull();
        assertThat(psToPdf).as("ps_to_pdf transformer class should exist").isNotNull();
        assertThat(ocr).as("ocr transformer class should exist").isNotNull();
        assertThat(videoThumb).as("video_thumbnail transformer class should exist").isNotNull();
        assertThat(pdfCompress).as("pdf_compress transformer class should exist").isNotNull();
        assertThat(pdfTemplate).as("pdf_template transformer class should exist").isNotNull();
        // html_to_text and markdown_to_html may not be registered if tools aren't installed
    }

    @Test
    public void testTransformerParameterParsing() throws Exception {
        // Test that transformers correctly parse parameters
        TransformMethod htmlToText = transformerService.getMethod("html_to_text");
        assumeTrue(htmlToText != null && htmlToText.ensureServiceAvailable(), "HTMLToText not available");

        File testHtml = new File(tempDir, "param_test.html");
        try (FileWriter writer = new FileWriter(testHtml)) {
            writer.write("<html><body><h1>Parameter Test</h1></body></html>");
        }

        FileFileSystem ffs = new FileFileSystem(tempDir);
        BaseFile sourceFile = ffs.getFile("param_test.html");

        // Test with multiple parameters
        BaseFile result = htmlToText.convert(sourceFile, "test8", "width=100,links=reference,images=alt", null, 5);
        assertThat(result).isNotNull();

        // Test with JSON parameters (Verify BaseTransformMethod handles it)
        String jsonParams = "{\"width\": 120, \"links\": \"inline\"}";
        BaseFile jsonResult = htmlToText.convert(sourceFile, "test_json_params", jsonParams, null, 5);
        assertThat(jsonResult).isNotNull();
    }
}
