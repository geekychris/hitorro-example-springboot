# Extending DMS Entities: Developer Guide

## Overview

This guide explains how to create custom document types in the Hitorro Document Management System (DMS) by extending the base `Document` class. The DMS provides a powerful foundation for managing versioned, categorized documents with content management capabilities.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Start](#quick-start)
3. [Step-by-Step Guide](#step-by-step-guide)
4. [Working Example: ProductReview](#working-example-productreview)
5. [Testing Your Entity](#testing-your-entity)
6. [Best Practices](#best-practices)
7. [Advanced Topics](#advanced-topics)

## Prerequisites

### Dependencies

Your module must depend on:
- `hitorro-basedms` - Core DMS functionality
- `hitorro-spring-boot` - Spring Boot integration (if using Spring)
- Jakarta Persistence API (JPA) - For entity annotations

### Knowledge Requirements

- Basic understanding of JPA/Hibernate
- Familiarity with Java inheritance
- Understanding of database schema design

## Quick Start

Here's a minimal example of extending `Document`:

```java
@Entity
@Table(name = "my_document")
@PrimaryKeyJoinColumn(name = "system_id")
@TypeClassMetaInfo(
    shortTypeName = "MyDocument",
    isPersisted = true,
    schemaVersion = 1
)
public class MyDocument extends Document {
    
    @Column(name = "custom_field")
    private String customField;
    
    // Getters, setters, copy(), serialize(), deserialize()
}
```

## Step-by-Step Guide

### Step 1: Create Your Entity Class

Create a new Java class that extends `com.hitorro.base.objects.Document`:

```java
package com.example.entities;

import com.hitorro.base.objects.Document;
import com.hitorro.base.objects.VersionableObject;

public class ProductReview extends Document {
    // Your implementation
}
```

### Step 2: Add JPA Annotations

Add the required JPA annotations for persistence:

```java
@Entity
@Table(name = "product_review")
@PrimaryKeyJoinColumn(name = "system_id")
public class ProductReview extends Document {
    // Fields
}
```

**Key Points:**
- `@Entity` - Marks this as a JPA entity
- `@Table(name = "...")` - Specifies the database table name
- `@PrimaryKeyJoinColumn(name = "system_id")` - **Critical**: Links to parent Document table using `system_id`

### Step 3: Add Hitorro Type System Annotations

Add Hitorro-specific metadata annotations:

```java
@TypeClassMetaInfo(
    shortTypeName = "ProductReview",
    isView = false,
    isPersisted = true,
    schemaVersion = ProductReview.SerializationVersion,
    softLinkField = "productName"  // Optional: field for soft references
)
@UiTypeProperties(
    name = "Product Review"
)
public class ProductReview extends Document {
    public static final int SerializationVersion = 1;
    // ...
}
```

**Key Points:**
- `shortTypeName` - Unique identifier for the type system
- `schemaVersion` - Version number for serialization compatibility
- `softLinkField` - Optional field name for soft reference lookups
- `UiTypeProperties` - Display name for UI components

### Step 4: Define Your Fields

Add custom fields with JPA column annotations:

```java
@Column(name = "product_name", nullable = false, length = 255)
private String productName;

@Column(name = "rating", nullable = false)
private Integer rating;

@Column(name = "pros", columnDefinition = "TEXT")
private String pros;

@Column(name = "verified", nullable = false)
private Boolean verified = false;
```

**Best Practices:**
- Use snake_case for database column names
- Specify `nullable` constraints
- Use `columnDefinition = "TEXT"` for large text fields
- Provide sensible defaults where appropriate

### Step 5: Add Getters and Setters

Add getters and setters with UI annotations:

```java
@UiProperties(
    displayName = "Product Name",
    displayType = UiProperties.TextFieldDisplay,
    order = 30
)
@FullTextAttributeMetaInfo(
    displayName = "productName",
    isFullTextIndexable = true,
    luceneIndexingFilters = "STANDARD,CASE,PORTERSTEM",
    luceneFieldName = "productName",
    stringLiteral = false,
    allField = true
)
public String getProductName() {
    return productName;
}

public void setProductName(String productName) {
    this.productName = productName;
}
```

**Key Points:**
- `@UiProperties` - Controls UI display
- `order` - Display order in forms (lower numbers first)
- `@FullTextAttributeMetaInfo` - Enables full-text search on this field
- Add validation logic in setters if needed

### Step 6: Implement copy() Method

Override `copy()` for versioning support:

```java
@Override
public void copy(VersionableObject orig) {
    super.copy(orig);
    if (orig instanceof ProductReview) {
        ProductReview other = (ProductReview) orig;
        this.productName = other.productName;
        this.rating = other.rating;
        this.reviewerName = other.reviewerName;
        this.pros = other.pros;
        this.cons = other.cons;
        this.verified = other.verified;
    }
}
```

**Important:** Always call `super.copy(orig)` first, then copy your custom fields.

### Step 7: Implement Serialization

Implement Hitorro's serialization methods:

```java
@Override
public void serialize(HTObjectOutputStream os) throws IOException, StoreException {
    os.writeInt(getSerializationVersion());
    super.serialize(os);
    
    // Version 1 fields
    os.writeString(productName);
    os.writeInt(rating != null ? rating : 0);
    os.writeString(reviewerName);
    os.writeString(pros);
    os.writeString(cons);
    os.writeBoolean(verified != null ? verified : false);
}

@Override
public void deserialize(HTObjectInputStream os) 
        throws IOException, ClassNotFoundException, StoreException {
    int version = os.readInt();
    super.deserialize(os);
    
    switch (version) {
        case 1:
            productName = os.readString();
            rating = os.readInt();
            reviewerName = os.readString();
            pros = os.readString();
            cons = os.readString();
            verified = os.readBoolean();
            break;
        default:
            throw new IOException("Unknown version: " + version);
    }
}
```

**Key Points:**
- Always write/read version number first
- Call `super.serialize()/deserialize()` after version
- Use switch statement for version compatibility
- Handle null values appropriately

### Step 8: Implement Schema Upgrade Support

Add upgrade logic for future schema changes:

```java
@Override
public boolean upgradeAllInstances(long currentSchemaVersion) {
    switch ((int) currentSchemaVersion) {
        case 1:
            // Future upgrade logic: 1->2
            return true;
        default:
            return false;
    }
}
```

### Step 9: Database Schema

The DMS will automatically create your table, but you need to ensure proper inheritance:

```sql
-- The Document table already exists with system_id as primary key
-- Your table extends it:
CREATE TABLE product_review (
    system_id BIGINT PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    rating INTEGER NOT NULL,
    reviewer_name VARCHAR(255),
    pros TEXT,
    cons TEXT,
    verified BOOLEAN NOT NULL,
    FOREIGN KEY (system_id) REFERENCES document(system_id)
);
```

**Note:** Hibernate will typically handle this automatically with `@PrimaryKeyJoinColumn`.

## Working Example: ProductReview

See the complete implementation in:
- Entity: [ProductReview.java](file:///Users/chris/hitorro/hitorro-example-springboot/src/main/java/com/hitorro/example/entities/ProductReview.java)
- Tests: [ProductReviewDMSTest.java](file:///Users/chris/hitorro/hitorro-example-springboot/src/test/java/com/hitorro/example/springboot/ProductReviewDMSTest.java)

### Key Features Demonstrated

1. **Custom Fields**: Product-specific fields (rating, pros, cons, verified)
2. **Validation**: Rating must be 1-5
3. **Full-Text Search**: Product name, pros, and cons are searchable
4. **Versioning**: Supports creating major/minor versions
5. **Inheritance**: Inherits all Document features (GUID, dates, notes, categories)

### Usage Example

```java
DMSSession session = dmsSessionFactory.createSession();
try {
    // Create a product review
    ProductReview review = new ProductReview();
    review.setTitle("Excellent Laptop");
    review.setProductName("ThinkPad X1 Carbon");
    review.setRating(5);
    review.setReviewerName("John Developer");
    review.setPros("Lightweight, excellent keyboard");
    review.setCons("Expensive");
    review.setVerified(true);
    
    // Persist
    session.persist(review);
    session.commit();
    
    // Retrieve
    ProductReview retrieved = (ProductReview) session.getSingleObjectById(
        ProductReview.class, 
        review.getId()
    );
    
    // Update
    retrieved.setRating(4);
    session.update(retrieved);
    session.commit();
    
} finally {
    session.close();
}
```

## Testing Your Entity

### Integration Test Structure

Create a Spring Boot test class:

```java
@SpringBootTest
@ActiveProfiles("test")
class MyDocumentDMSTest {
    
    @Autowired
    private DMSSessionFactory dmsSessionFactory;
    
    @Test
    void canCreateAndPersist() throws Exception {
        DMSSession session = dmsSessionFactory.createSession();
        try {
            MyDocument doc = new MyDocument();
            // Set fields
            session.persist(doc);
            session.commit();
            
            assertThat(doc.getId()).isNotNull();
        } finally {
            session.close();
        }
    }
}
```

### Essential Tests

1. **Create and Persist** - Verify entity can be saved
2. **Retrieve by ID** - Verify entity can be loaded
3. **Update** - Verify changes persist
4. **Delete** - Verify deletion works
5. **Validation** - Test field constraints
6. **Versioning** - Test `createMajorVersion()` and `createMinorVersion()`
7. **Query** - Test HQL queries
8. **Rollback** - Verify transaction rollback
9. **Inheritance** - Verify Document features work

### Running Tests

```bash
cd hitorro-example-springboot
mvn test -Dtest=ProductReviewDMSTest
```

## Best Practices

### 1. Naming Conventions

- **Class Name**: PascalCase, descriptive (e.g., `ProductReview`, `CustomerOrder`)
- **Table Name**: snake_case (e.g., `product_review`, `customer_order`)
- **Column Names**: snake_case (e.g., `product_name`, `created_date`)

### 2. Field Design

- Use appropriate data types (Integer for numbers, Boolean for flags)
- Set `nullable` constraints appropriately
- Use `TEXT` for large text fields
- Provide sensible defaults in constructors

### 3. Serialization

- Always increment `SerializationVersion` when changing fields
- Never remove fields from serialization (for backward compatibility)
- Add new fields at the end in new version cases
- Handle null values explicitly

### 4. Versioning Support

- Always implement `copy()` to copy all custom fields
- Test version creation thoroughly
- Remember that versions share the same `canonicalGuid`

### 5. Performance

- Add database indexes on frequently queried fields
- Use `@ManyToOne(fetch = FetchType.LAZY)` for relationships
- Consider using `@Column(length = ...)` to optimize storage

### 6. Security

- Validate input in setters
- Use appropriate access modifiers
- Consider adding realm-based access control

## Advanced Topics

### Adding Relationships

You can add relationships to other entities:

```java
@ManyToOne(fetch = FetchType.EAGER)
@JoinColumn(name = "author_id")
private User author;
```

### Using Categories

Documents support categorization:

```java
review.addCategory("status", "approved");
review.addCategory("featured", "true");
```

### Content Management

Documents can have associated content (files):

```java
Content content = new Content();
content.setOriginalFileName("review-image.jpg");
content.setContent(fileName, inputStream, contentType);
review.getContents().add(content);
```

### Full-Text Search

Fields marked with `@FullTextAttributeMetaInfo` are indexed:

```java
@FullTextAttributeMetaInfo(
    displayName = "productName",
    isFullTextIndexable = true,
    luceneIndexingFilters = "STANDARD,CASE,PORTERSTEM",
    luceneFieldName = "productName",
    stringLiteral = false,
    allField = true
)
public String getProductName() {
    return productName;
}
```

### Soft References

If you specify `softLinkField`, you can retrieve by that field:

```java
HTSerializable found = session.getBySoftReference(
    ProductReview.class, 
    "ThinkPad X1 Carbon"  // productName value
);
```

### Custom Queries

Use HQL for complex queries:

```java
String hql = "from ProductReview where rating >= :minRating and verified = true";
Query query = session.createQuery(hql);
query.setParameter("minRating", 4);
List<ProductReview> results = query.list();
```

### Version History

Retrieve all versions of a document:

```java
List<VersionableObject> versions = document.getAllVersions(session);
```

## Common Pitfalls

### ❌ Forgetting @PrimaryKeyJoinColumn

```java
// WRONG - will create separate ID column
@Entity
@Table(name = "my_document")
public class MyDocument extends Document { }

// CORRECT - uses parent's system_id
@Entity
@Table(name = "my_document")
@PrimaryKeyJoinColumn(name = "system_id")
public class MyDocument extends Document { }
```

### ❌ Not Calling super.copy()

```java
// WRONG - parent fields not copied
@Override
public void copy(VersionableObject orig) {
    MyDocument other = (MyDocument) orig;
    this.customField = other.customField;
}

// CORRECT
@Override
public void copy(VersionableObject orig) {
    super.copy(orig);  // Copy parent fields first
    if (orig instanceof MyDocument) {
        MyDocument other = (MyDocument) orig;
        this.customField = other.customField;
    }
}
```

### ❌ Incorrect Serialization Order

```java
// WRONG - super called before version
@Override
public void serialize(HTObjectOutputStream os) throws IOException {
    super.serialize(os);
    os.writeInt(getSerializationVersion());
    // ...
}

// CORRECT - version first, then super
@Override
public void serialize(HTObjectOutputStream os) throws IOException {
    os.writeInt(getSerializationVersion());
    super.serialize(os);
    // ...
}
```

### ❌ Breaking Serialization Compatibility

```java
// WRONG - removing fields breaks old data
switch (version) {
    case 2:
        newField = os.readString();
        // Missing case 1 fields!
}

// CORRECT - fall-through to read all fields
switch (version) {
    case 2:
        newField = os.readString();
    case 1:
        oldField = os.readString();
}
```

## Summary

Extending DMS entities involves:

1. ✅ Extend `Document` class
2. ✅ Add JPA annotations (`@Entity`, `@Table`, `@PrimaryKeyJoinColumn`)
3. ✅ Add Hitorro annotations (`@TypeClassMetaInfo`, `@UiTypeProperties`)
4. ✅ Define custom fields with `@Column`
5. ✅ Add getters/setters with `@UiProperties`
6. ✅ Implement `copy()` method
7. ✅ Implement `serialize()/deserialize()`
8. ✅ Implement `upgradeAllInstances()`
9. ✅ Write comprehensive tests
10. ✅ Test versioning, queries, and inheritance

## Additional Resources

- [Document.java](file:///Users/chris/hitorro/hitorro-basedms/src/main/java/com/hitorro/base/objects/Document.java) - Base Document class
- [Post.java](file:///Users/chris/hitorro/hitorro-basedms/src/main/java/com/hitorro/base/objects/Post.java) - Example extension
- [HitorroDMSIntegrationTest.java](file:///Users/chris/hitorro/hitorro-example-springboot/src/test/java/com/hitorro/example/springboot/HitorroDMSIntegrationTest.java) - DMS test examples
- [ProductReview.java](file:///Users/chris/hitorro/hitorro-example-springboot/src/main/java/com/hitorro/example/entities/ProductReview.java) - Complete working example

---

**Need Help?** Review the ProductReview implementation for a complete, working example of all concepts covered in this guide.
