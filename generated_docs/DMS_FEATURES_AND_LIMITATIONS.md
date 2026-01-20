# Hitorro DMS Features and Current Testing Status

## Overview

This document explains the comprehensive features of Hitorro's DMS (Document Management System) and the current state of testing in the Spring Boot integration.

## Core DMS Features (From hitorro-test)

### 1. Document Versioning (`VersioningTest.java`)

**Key Features**:
- **Major versions**: 1.0 → 2.0 → 3.0
- **Minor versions**: 3.0 → 3.1 → 3.2
- **Branch versions**: 3.1 → 3.1.1.0
- **Version chains**: Navigate from version to version via `getNextVersion()`
- **Canonical GUIDs**: All versions share a canonical GUID
- **Version labels**: Automatic label generation and management

**Usage**:
```java
Document doc = new Document();
session.persist(doc);  // Creates version 1.0

Document v2 = (Document) doc.createMajorVersion();  // Creates 2.0
Document v2_1 = (Document) v2.createMinorVersion(); // Creates 2.1
```

### 2. Multi-Part Content (`ContentTest.java`)

**Key Features**:
- **Multiple content files per document**: a.html, b.html, c.html, d.html
- **Content renditions**: Create multiple sizes/versions of the same content
- **Content retrieval by filename**: `doc.getContentByFileName("a.html")`
- **Content types**: HTML, images, Word docs, etc.
- **Original filenames preserved**

**Usage**:
```java
Document doc = new Document();
doc.setContent("main.html", htmlType, fileA);
doc.setContent("sidebar.html", htmlType, fileB);
doc.setContent("image.jpg", imageType, fileC)
   .setContentRendition(session, imageType, thumbnail, "320x240");
```

### 3. Content Constraints and Categories

**Key Features**:
- **Tag constraints**: Find content by category tags
- **Resolution constraints**: Find renditions by size
- **Filename constraints**: Match by filename pattern
- **Logical operators**: Combine constraints with AND/OR
- **Categories**: Organize content into hierarchical categories

**Usage**:
```java
// Add categories
content.addCategory("docparts", "thumbnail");
content.addCategory("images", "photo");

// Retrieve by constraint
Content thumbnail = doc.getContentByConstraint(
    new TagConstraint("docparts", "thumbnail"), true);

// Combined constraints
HTPredicate<Content> constraint = new LogicalAndOperator(
    new ResolutionConstraint("320x240"),
    new FileNameMatchContentConstraint("photo.jpg", true)
);
Content result = doc.getContentByConstraint(constraint, true);
```

### 4. Store Management

**Store Types**:
- **File stores**: Store files on filesystem
- **Blob stores**: Store in database
- **Unmanaged stores**: Link to external filesystems
- **Default store**: Automatic store selection

**Features**:
- Store paths and roots
- Public/private visibility
- Online/offline status
- Linkable content (symlinks to Apache-accessible directories)

**Usage**:
```java
Store store = StoreUtil.getStore("default");
Content content = new Content();
content.setStore(store);
content.setContent("file.dat", inputFile, contentType);
```

### 5. Content Retrieval and Storage

**Key Features**:
- **Store and retrieve files**: Binary-safe content handling
- **External URLs**: Generate URLs for public access
- **Content links**: Link to URLs without storing files
- **Linkable files**: Create symlinks for Apache serving
- **Content back-references**: Content knows which documents reference it

**Usage**:
```java
// Store content
Content c = doc.setContent("data.bin", contentType, inputFile);
session.persist(doc);

// Retrieve content
BaseFile outputFile = c.getContentFile();
c.getContent(outputFile);  // Copy content to file

// Get URL
String url = c.getExternalURL();

// Create external link (no file storage)
Content link = doc.setContentLink("https://example.com/doc.pdf", pdfType);
```

### 6. Containers and Relationships

**Key Features**:
- **Containers**: Group related documents
- **Type-safe containers**: `Container(Post.class, "id")`
- **Iterator support**: Iterate over container contents
- **Bidirectional relationships**: Documents ↔ Containers

## Current Spring Boot Integration Status

### ✅ What Works

1. **Basic DMS Operations** (`HitorroDMSIntegrationTest.java`)
   - ✅ Session creation and management
   - ✅ Entity persistence (NamedLongEntry)
   - ✅ CRUD operations
   - ✅ Transactions (commit/rollback)
   - ✅ Soft references (getBySoftReference)
   - ✅ Unified ID system
   - ✅ HQL queries
   - ✅ Multiple sessions

2. **Type System Integration**
   - ✅ TypeManager initialization
   - ✅ Entity registration
   - ✅ Soft reference lookups
   - ✅ GUID generation

3. **Hibernate Integration**
   - ✅ Spring EntityManagerFactory → Hitorro HibernateUtil bridge
   - ✅ Transaction management
   - ✅ Session lifecycle

### ⚠️ Limitations

1. **Store Configuration Required**
   - Content storage features require Store entities to be configured
   - Stores need proper root paths and file system setup
   - This is typically done during application initialization, not in tests

2. **Versioning Requires Canonical GUID Setup**
   - Document versioning needs proper canonical GUID initialization
   - This requires the full service framework to be initialized
   - `createMajorVersion()` and `createMinorVersion()` need additional setup

3. **Service Loading Conflicts**
   - Loading HibernateService via services causes schema conflicts with DMS auto-config
   - Both try to initialize Hibernate, causing duplicate schema creation
   - Need better coordination between the two initialization paths

## Recommended Approach for Real Applications

###  Application Initialization**

Create stores during application startup:

```java
@Component
public class DMSInitializer implements ApplicationRunner {
    @Autowired
    private DMSSessionFactory sessionFactory;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        DMSSession session = sessionFactory.createSession();
        try {
            // Check if default store exists
            Store defaultStore = (Store) session.getSingleObject(
                Store.class, " where defaultStore=true");
            
            if (defaultStore == null) {
                // Create default store
                defaultStore = new Store();
                defaultStore.setName("default");
                defaultStore.setDefaultStore(true);
                defaultStore.setStoreType(StoreType.File.name());
                defaultStore.setRootPath("/var/lib/hitorro/content");
                defaultStore.setDocRoot("/content");
                defaultStore.setIsPubliclyVisible(true);
                
                session.persist(defaultStore);
                session.commit();
            }
        } finally {
            session.close();
        }
    }
}
```

### 2. **Use DMS Features in Services**

```java
@Service
public class DocumentService {
    @Autowired
    private DMSSessionFactory sessionFactory;
    
    public Document createDocumentWithContent(String title, MultipartFile file) {
        DMSSession session = sessionFactory.createSession();
        try {
            Document doc = new Document();
            doc.setTitle(title);
            
            // Get content type
            ContentType ct = ContentTypeCache.getCache()
                .getTypeFromFileWithDefault(file.getOriginalFilename());
            
            // Create temp file from upload
            File tempFile = File.createTempFile("upload-", file.getOriginalFilename());
            file.transferTo(tempFile);
            BaseFile baseFile = FileFileSystem.Root.getFile(tempFile.getAbsolutePath());
            
            // Add content
            doc.setContent(file.getOriginalFilename(), ct, baseFile);
            
            session.persist(doc);
            session.commit();
            
            return doc;
        } finally {
            session.close();
        }
    }
}
```

## Testing Strategy

### Unit Tests
- Test business logic without DMS
- Mock DMSSession and entities
- Fast, isolated tests

### Integration Tests (Current)
- Test DMS auto-configuration
- Test basic CRUD operations
- Test TypeManager integration
- Use simple entities (NamedLongEntry)

### Full DMS Tests (hitorro-test module)
- Complete Store configuration
- Document versioning
- Multi-part content
- Content constraints
- Real file system operations

## Next Steps

To fully showcase DMS features in Spring Boot:

1. **Create DMSInitializer component** - Set up stores on startup
2. **Add REST endpoints for document management** - Upload, version, retrieve
3. **Create example controllers** showing:
   - Document upload with multiple files
   - Version creation and retrieval
   - Content download by constraint
   - Thumbnail/rendition serving
4. **Add integration tests** with proper Store setup
5. **Document Store configuration** in application.yml

## References

- **VersioningTest.java** - `/hitorro-test/src/main/java/com/hitorro/test/dms/VersioningTest.java`
- **ContentTest.java** - `/hitorro-test/src/main/java/com/hitorro/test/dms/ContentTest.java`
- **Hitorro DMS Documentation** - See ARCHITECTURE.md in hitorro-all

## Conclusion

The Hitorro DMS is a powerful document versioning and content management system with:
- Sophisticated version control (major/minor/branch)
- Multi-part content with renditions
- Flexible storage backends (file, blob, unmanaged)
- Advanced content retrieval via constraints
- Full Spring Boot integration

Current Spring Boot integration provides the foundation (transactions, sessions, type system), but full DMS features require proper Store configuration which is typically done during application initialization rather than in tests.
