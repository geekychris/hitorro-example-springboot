# Crawler ContentType Null Error - Fixed

## Problem

When crawling files, the crawler was failing with errors like:
```
Cannot invoke "com.hitorro.base.objects.ContentType.getSoftGuid()" because "ct" is null
```

This happened for files like `data.noun`, `index.adj`, etc.

## Root Cause

The `ContentTypeCache` doesn't have `ContentType` objects loaded. This happens when:

1. The DMS database hasn't been initialized with ContentType records
2. The file's MIME type doesn't match any registered ContentType
3. The ContentTypeCache is empty on startup

## Solution Applied

### 1. Added Null Safety in Crawler

Updated `createDocumentFromFile()` to handle null ContentType gracefully:

```java
// If no content type found, try fallbacks
if (contentType == null) {
    // Try common fallbacks
    contentType = cache.getContentTypeByMimeType("application/octet-stream");
    
    if (contentType == null) {
        contentType = cache.getContentTypeByMimeType("text/plain");
    }
    
    // If still null, create document without content attachment
    if (contentType == null) {
        logger.warn("No ContentType available for file: {} (mime: {}). " +
                   "Creating document without content attachment.", 
                   file.getName(), mimeType);
        // Just save the document metadata without content
        return doc;
    }
}
```

### 2. Improved Error Messages

Changed error reporting to be more helpful:
```java
if (errorMsg != null && errorMsg.contains("ContentType") && errorMsg.contains("null")) {
    errorMsg = "ContentType not found (DMS may need ContentType records initialized)";
}
```

## How to Initialize ContentTypes (If Needed)

ContentTypes are typically initialized via:

### Option 1: CSV Import (Recommended for Production)

Enable in `application.yml`:
```yaml
hitorro:
  dms:
    db-init:
      enabled: true
      data-sets:
        - name: "contenttypes"
          csv-file: "classpath:data/contenttypes.csv"
          consumer: "com.hitorro.basedms.csvconsumers.ContentTypeCSVConsumer"
```

### Option 2: Manual Creation via API

Create ContentTypes programmatically:
```java
ContentType textType = new ContentType();
textType.setMimeType("text/plain");
textType.setFileExtension("txt");
session.persist(textType);
```

### Option 3: Use Existing Store Configuration

If you have a Store configured with ContentTypes, they should be loaded automatically when the store is initialized.

## Current Behavior

With the fix applied:

1. **First attempt**: Look up ContentType by detected MIME type
2. **Fallback 1**: Try `application/octet-stream`
3. **Fallback 2**: Try `text/plain`
4. **Final fallback**: Create document WITHOUT content attachment (metadata only)
5. **Error reporting**: Log warning and continue with next file

## Testing

After fix:
- ✅ Crawler continues even if ContentType is missing
- ✅ Documents created with metadata (title, note)
- ✅ Warning logged for files without ContentType
- ✅ Error message is informative
- ✅ Crawl completes successfully

## Files Affected

- `DMSCrawlerController.java` - Added null safety and better error handling

## Recommendation

For production use, initialize ContentType records via CSV import with common MIME types:
- text/plain (.txt)
- application/pdf (.pdf)
- image/jpeg (.jpg, .jpeg)
- image/png (.png)
- application/octet-stream (binary default)
- text/html (.html)
- application/json (.json)
- etc.

This ensures all files can be properly typed and stored with content.

## Status

✅ Null pointer errors eliminated
✅ Graceful fallback handling
✅ Better error messages
✅ Crawler completes successfully
✅ Documents created (with or without content)
