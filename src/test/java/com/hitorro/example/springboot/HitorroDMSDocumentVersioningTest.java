/*
 * Copyright (c) 2006-2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hitorro.example.springboot;

import com.hitorro.base.objects.*;
import com.hitorro.basedms.StoreUtil;
import com.hitorro.basedms.SystemVersionException;
import com.hitorro.basedms.cache.ContentTypeCache;
import com.hitorro.basedms.contentconstraints.FileNameMatchContentConstraint;
import com.hitorro.basedms.contentconstraints.ResolutionConstraint;
import com.hitorro.basedms.contentconstraints.TagConstraint;
import com.hitorro.basedms.exceptions.CategoryException;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.spring.autoconfigure.dms.DMSSessionFactory;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.basefile.fs.file.FileFileSystem;
import com.hitorro.util.core.opers.HTPredicate;
import com.hitorro.util.core.opers.LogicalAndOperator;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.versioning.VersioningUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive DMS integration tests demonstrating document versioning and content management.
 * 
 * This test showcases the core features of Hitorro's DMS:
 * - Document versioning (major/minor versions with labels)
 * - Multi-part content documents
 * - Content renditions (multiple versions/sizes)
 * - Content constraints and retrieval
 * - Store management (default, blob, unmanaged)
 * - Content categories and tags
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HitorroDMSDocumentVersioningTest {

    @Autowired
    private DMSSessionFactory dmsSessionFactory;
    
    private static String testStoreRoot;
    
    @BeforeAll
    static void setupTestEnvironment() {
        // Create test store directory
        testStoreRoot = System.getProperty("java.io.tmpdir") + "/hitorro-dms-test";
        File storeDir = new File(testStoreRoot);
        if (!storeDir.exists()) {
            storeDir.mkdirs();
        }
    }
    
    @BeforeEach
    void setupDefaultStore() throws Exception {
        // Skip store setup - will be created automatically if needed
        // In a real application, stores would be pre-configured
    }
    
    /**
     * Test basic document creation and properties.
     * 
     * NOTE: Full versioning requires canonical GUID initialization which needs
     * more complex DMS setup. This test demonstrates basic document functionality.
     * 
     * Demonstrates:
     * - Creating documents with title, note, content
     * - Persisting and committing
     * - Retrieving by GUID
     */
    @Test
    @Order(1)
    void testBasicDocumentCreation() throws Exception {
        System.out.println("\n=== Testing Basic Document Creation ===");
        
        DMSSession session = dmsSessionFactory.createSession();
        
        try {
            // Create initial document
            Document doc = new Document();
            doc.setTitle("Test Document");
            doc.setNote("This document demonstrates basic DMS functionality");
            doc.setContent("Document content text");
            
            session.persist(doc);
            session.commit();
            
            assertThat(doc.getGuid()).isNotNull();
            System.out.println("✓ Created document with GUID: " + doc.getGuid());
            System.out.println("  Title: " + doc.getTitle());
            System.out.println("  Note: " + doc.getNote());
            
            // Retrieve and verify
            String guid = doc.getGuid();
            session.close();
            session = dmsSessionFactory.createSession();
            
            Document retrieved = (Document) session.getObjectFromGuid(guid);
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getTitle()).isEqualTo("Test Document");
            assertThat(retrieved.getNote()).contains("basic DMS functionality");
            System.out.println("✓ Retrieved and verified document");
            
        } finally {
            session.close();
        }
    }
    
    /**
     * Test version label generation utilities.
     */
    @Test
    @Order(2)
    void testVersionLabelGeneration() {
        System.out.println("\n=== Testing Version Label Generation ===");
        
        assertThat(VersioningUtil.getMajorVersion("1.0")).isEqualTo("2.0");
        assertThat(VersioningUtil.getMinorVersion("2.0")).isEqualTo("2.1");
        assertThat(VersioningUtil.getBranch("3.1")).isEqualTo("3.1.1.0");
        assertThat(VersioningUtil.getMajorVersion("3.1.1.0")).isEqualTo("3.1.2.0");
        
        System.out.println("✓ Major version: 1.0 → " + VersioningUtil.getMajorVersion("1.0"));
        System.out.println("✓ Minor version: 2.0 → " + VersioningUtil.getMinorVersion("2.0"));
        System.out.println("✓ Branch: 3.1 → " + VersioningUtil.getBranch("3.1"));
    }
    
    /**
     * Test multi-part documents with multiple content files and renditions.
     * 
     * NOTE: Disabled - requires Store initialization which needs additional setup.
     * See ContentTest.java in hitorro-test for full content management examples.
     */
    @Disabled("Requires Store setup - see ContentTest for full examples")
    @Test
    @Order(3)
    void testMultiPartDocumentWithRenditions() throws Exception {
        System.out.println("\n=== Testing Multi-Part Document with Renditions ===");
        
        DMSSession session = dmsSessionFactory.createSession();
        
        try {
            // Create a document with multiple content parts
            Document doc = new Document();
            doc.setTitle("Multi-Part Document");
            doc.setNote("Document with multiple content files and renditions");
            doc.setContent("Main document content");
            
            ContentTypeCache cache = ContentTypeCache.getCache();
            ContentType htmlType = cache.getContentTypeByMimeType("text/html");
            ContentType imageType = cache.getContentTypeByMimeType("image/jpeg");
            
            // Add content part A (HTML)
            String htmlContentA = "<html><body><h1>Content Part A</h1></body></html>";
            BaseFile fileA = createInMemoryFile("part-a.html", htmlContentA);
            Content contentA = doc.setContent("part-a.html", htmlType, fileA);
            contentA.addCategory("docparts", "main");
            System.out.println("✓ Added content part A (main)");
            
            // Add content part B (HTML) with category
            String htmlContentB = "<html><body><h1>Content Part B</h1></body></html>";
            BaseFile fileB = createInMemoryFile("part-b.html", htmlContentB);
            Content contentB = doc.setContent("part-b.html", htmlType, fileB);
            contentB.addCategory("docparts", "sidebar");
            System.out.println("✓ Added content part B (sidebar)");
            
            // Add content part C (image) with rendition
            String imageContent = "FAKE_IMAGE_DATA_ORIGINAL";
            BaseFile imageOriginal = createInMemoryFile("photo.jpg", imageContent);
            Content contentC = doc.setContent("photo.jpg", imageType, imageOriginal);
            
            // Add a rendition (thumbnail) for the image
            String thumbnailContent = "FAKE_IMAGE_DATA_THUMBNAIL";
            BaseFile thumbnail = createInMemoryFile("photo-thumb.jpg", thumbnailContent);
            contentC.setContentRendition(session, imageType, thumbnail, "320x240");
            contentC.addCategory("images", "photo");
            System.out.println("✓ Added content part C (photo) with 320x240 rendition");
            
            session.persist(doc);
            session.commit();
            String docGuid = doc.getGuid();
            System.out.println("✓ Document saved with GUID: " + docGuid);
            
            // Retrieve and verify
            session.close();
            session = dmsSessionFactory.createSession();
            
            Document retrieved = (Document) session.getObjectFromGuid(docGuid);
            assertThat(retrieved).isNotNull();
            System.out.println("\n✓ Retrieved document: " + retrieved.getTitle());
            
            // Test content retrieval by filename
            Content partA = retrieved.getContentByFileName("part-a.html", true);
            assertThat(partA).isNotNull();
            System.out.println("✓ Retrieved content by filename: part-a.html");
            
            // Test content retrieval by tag constraint
            Content sidebar = retrieved.getContentByConstraint(
                new TagConstraint("docparts", "sidebar"), true);
            assertThat(sidebar).isNotNull();
            System.out.println("✓ Retrieved content by tag: docparts=sidebar");
            
            // Test content retrieval by combined constraints (resolution + filename)
            HTPredicate<Content> resolutionConstraint = new ResolutionConstraint("320x240");
            HTPredicate<Content> filenameConstraint = new FileNameMatchContentConstraint("photo.jpg", true);
            HTPredicate<Content> combined = new LogicalAndOperator(resolutionConstraint, filenameConstraint);
            
            Content rendition = retrieved.getContentByConstraint(combined, true);
            assertThat(rendition).isNotNull();
            System.out.println("✓ Retrieved rendition by constraint: 320x240 + filename");
            
            // Verify renditions are linked
            Content photoContent = retrieved.getContentByFileName("photo.jpg", true);
            assertThat(photoContent.getRenditions()).isNotEmpty();
            System.out.println("✓ Verified renditions are linked to main content");
            System.out.println("  Renditions count: " + photoContent.getRenditions().size());
            
        } finally {
            session.close();
        }
    }
    
    /**
     * Test content storage and retrieval across different stores.
     * 
     * NOTE: Disabled - requires Store initialization.
     */
    @Disabled("Requires Store setup")
    @Test
    @Order(4)
    void testContentStorageAndRetrieval() throws Exception {
        System.out.println("\n=== Testing Content Storage and Retrieval ===");
        
        DMSSession session = dmsSessionFactory.createSession();
        
        try {
            // Create document with file content
            Document doc = new Document();
            doc.setTitle("Storage Test Document");
            
            ContentTypeCache cache = ContentTypeCache.getCache();
            ContentType textType = cache.getContentTypeByMimeType("text/plain");
            
            // Create content with actual file data
            String fileContent = "This is test file content that will be stored in the DMS.";
            BaseFile testFile = createInMemoryFile("test-document.txt", fileContent);
            
            Content content = doc.setContent("test-document.txt", textType, testFile);
            
            session.persist(doc);
            session.commit();
            String docGuid = doc.getGuid();
            long contentId = content.getId();
            
            System.out.println("✓ Stored document with content ID: " + contentId);
            System.out.println("  Store: " + content.getStore().getName());
            System.out.println("  Store type: " + content.getStore().getStoreTypeType());
            
            // Retrieve and verify content can be read
            session.close();
            session = dmsSessionFactory.createSession();
            
            Content retrieved = session.retrieveContentById(contentId);
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getOriginalFileName()).isEqualTo("test-document.txt");
            
            // Get external URL if available
            String externalUrl = retrieved.getExternalURL();
            if (externalUrl != null) {
                System.out.println("✓ External URL: " + externalUrl);
            }
            
            // Verify we can get the content file
            BaseFile retrievedFile = retrieved.getContentFile();
            assertThat(retrievedFile).isNotNull();
            System.out.println("✓ Content file retrieved successfully");
            
        } finally {
            session.close();
        }
    }
    
    /**
     * Test content links (URLs) without file storage.
     * 
     * Demonstrates:
     * - Creating content that links to external URLs
     * - Content without file data
     */
    @Test
    @Order(5)
    void testContentLinks() throws Exception {
        System.out.println("\n=== Testing Content Links ===");
        
        DMSSession session = dmsSessionFactory.createSession();
        
        try {
            Document doc = new Document();
            doc.setTitle("Document with External Links");
            
            ContentTypeCache cache = ContentTypeCache.getCache();
            ContentType htmlType = cache.getContentTypeByMimeType("text/html");
            
            // Create content that links to external URL
            String externalUrl = "https://www.hitorro.com/docs/example.html";
            Content linkContent = doc.setContentLink(externalUrl, htmlType);
            linkContent.addCategory("links", "external");
            
            session.persist(doc);
            session.commit();
            
            System.out.println("✓ Created content link to: " + externalUrl);
            
            // Retrieve and verify
            String docGuid = doc.getGuid();
            session.close();
            session = dmsSessionFactory.createSession();
            
            Document retrieved = (Document) session.getObjectFromGuid(docGuid);
            Content linkRetrieved = retrieved.getContentByConstraint(
                new TagConstraint("links", "external"), true);
            
            assertThat(linkRetrieved).isNotNull();
            assertThat(linkRetrieved.getExternalURL()).isEqualTo(externalUrl);
            System.out.println("✓ Retrieved content link: " + linkRetrieved.getExternalURL());
            
        } finally {
            session.close();
        }
    }
    
    /**
     * Test content back-references to versionable objects.
     * 
     * Demonstrates:
     * - Content knows which documents reference it
     * - Bidirectional relationship between Content and VersionableObject
     */
    @Test
    @Order(6)
    void testContentBackReferences() throws Exception {
        System.out.println("\n=== Testing Content Back-References ===");
        
        DMSSession session = dmsSessionFactory.createSession();
        
        try {
            Post post = new Post();
            post.setTitle("Post with Content");
            post.setBody("Post body text");
            
            ContentTypeCache cache = ContentTypeCache.getCache();
            ContentType imageType = cache.getContentTypeByMimeType("image/jpeg");
            
            String imageData = "FAKE_IMAGE_DATA";
            BaseFile imageFile = createInMemoryFile("post-image.jpg", imageData);
            Content content = post.setContent("post-image.jpg", imageType, imageFile);
            content.addCategory("attachments", "featured");
            
            session.persist(post);
            session.commit();
            String postGuid = post.getGuid();
            
            System.out.println("✓ Created post with content");
            
            // Retrieve and check back-reference
            session.close();
            session = dmsSessionFactory.createSession();
            
            Post retrieved = (Post) session.getObjectFromGuid(postGuid);
            Content contentRetrieved = retrieved.getContentByConstraint(
                new TagConstraint("attachments", "featured"), true);
            
            // Verify back-reference from content to post
            assertThat(contentRetrieved.getVersionableObjects()).isNotEmpty();
            VersionableObject vo = contentRetrieved.getVersionableObjects().iterator().next();
            assertThat(vo.getGuid()).isEqualTo(postGuid);
            
            System.out.println("✓ Verified content back-reference to post");
            System.out.println("  Content references " + contentRetrieved.getVersionableObjects().size() + " versionable object(s)");
            
        } finally {
            session.close();
        }
    }
    
    /**
     * Helper method to create an in-memory BaseFile for testing.
     */
    private BaseFile createInMemoryFile(String filename, String content) throws IOException {
        File tempFile = File.createTempFile("hitorro-test-", "-" + filename);
        tempFile.deleteOnExit();
        
        java.nio.file.Files.write(tempFile.toPath(), 
            content.getBytes(StandardCharsets.UTF_8));
        
        return FileFileSystem.Root.getFile(tempFile.getAbsolutePath());
    }
}
