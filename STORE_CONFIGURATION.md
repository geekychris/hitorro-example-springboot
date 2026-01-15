# Store Configuration Guide

## NullPointerException in Store.init()

### Error Message

```
java.lang.NullPointerException: Cannot invoke "com.hitorro.util.basefile.fs.BaseFile.mkdir()" 
because "this.rootDir" is null
    at com.hitorro.base.objects.Store.init(Store.java:152)
    at com.hitorro.basedms.csvconsumers.StoreCSVConsumer.add(StoreCSVConsumer.java:69)
```

### What This Means

This error occurs when the DMS tries to initialize **Store** objects from CSV data, but the stores don't have valid file system paths configured.

**Store objects** in Hitorro DMS represent:
- Content storage locations
- File system directories for document content
- Different storage backends (file, blob, etc.)

## Quick Fix (Recommended for Example App)

Disable Store CSV loading in `application.yml`:

```yaml
dms:
  db-init:
    enabled: false  # ✅ Disabled - no CSV loading
```

**This is the default configuration now.**

### Why Disable?

For a basic example application:
- ✅ You don't need pre-configured stores
- ✅ Stores can be created programmatically as needed
- ✅ No file system setup required
- ✅ Simpler initial configuration

The error is non-fatal (`fail-on-error: false`) but disabling avoids the noise in logs.

## When Do You Need Stores?

You need Store configuration when:

1. **Storing document content** - Documents have binary content (PDF, images, etc.)
2. **Using content stores** - Different storage locations for different content types
3. **Production DMS** - Organized content storage with backup/archival

For testing or learning, you can:
- Create documents without content
- Store content inline in database
- Create stores programmatically in code

## How to Configure Stores (If Needed)

### Step 1: Create Store Directories

```bash
# Create content store directories
mkdir -p /Users/chris/hthome/stores/default
mkdir -p /Users/chris/hthome/stores/archive
mkdir -p /Users/chris/hthome/stores/temp
```

### Step 2: Create stores.csv

**File**: `src/main/resources/data/stores.csv`

```csv
name,root_dir,store_type,is_default
default,/Users/chris/hthome/stores/default,file,true
archive,/Users/chris/hthome/stores/archive,file,false
temp,/Users/chris/hthome/stores/temp,file,false
```

**Columns**:
- `name` - Store identifier
- `root_dir` - Absolute path to storage directory
- `store_type` - Storage backend type (file, blob, link, unmanaged)
- `is_default` - Whether this is the default store for new content

### Step 3: Enable CSV Loading

**File**: `application.yml`

```yaml
dms:
  db-init:
    enabled: true
    fail-on-error: true  # Change to true once stores are configured
    data-sets:
      - name: "stores"
        csv-file: "classpath:data/stores.csv"
        consumer: "com.hitorro.basedms.csvconsumers.StoreCSVConsumer"
```

### Step 4: Verify Configuration

```bash
# Start application
mvn spring-boot:run

# Check logs - should see:
# Loading CSV data from: classpath:data/stores.csv
# Store 'default' initialized successfully
```

## Programmatic Store Creation

Alternative: Create stores in code instead of CSV:

```java
@Component
public class StoreInitializer {
    
    @Autowired
    private DMSSession session;
    
    @EventListener(ApplicationReadyEvent.class)
    public void createDefaultStore() {
        try {
            // Check if default store exists
            Store defaultStore = session.getStore("default");
            if (defaultStore == null) {
                // Create new store
                defaultStore = (Store) session.newObject("dm_store");
                defaultStore.setObjectName("default");
                defaultStore.setRootDir("/Users/chris/hthome/stores/default");
                defaultStore.setStoreType("file");
                defaultStore.setIsDefault(true);
                
                // Create directory if needed
                new File(defaultStore.getRootDir()).mkdirs();
                
                // Save to database
                defaultStore.save();
                
                logger.info("Default store created: {}", defaultStore.getRootDir());
            }
        } catch (Exception e) {
            logger.error("Failed to create default store", e);
        }
    }
}
```

## Store Types

### 1. File Store

**Most common** - stores content as files on disk

```csv
default,/Users/chris/hthome/stores/default,file,true
```

**Properties**:
- Direct file system access
- Easy to backup (just copy directory)
- Good for moderate content volumes
- OS-level file permissions

### 2. Blob Store

Stores content in database as BLOBs

```csv
dbstore,,blob,false
```

**Properties**:
- Content in database (no separate files)
- Transactional with document metadata
- Easier deployment (no file system setup)
- Can impact database size

### 3. Link Store

References content at external URLs

```csv
external,,link,false
```

**Properties**:
- Content stored elsewhere (S3, CDN, etc.)
- Just stores URLs/references
- Good for externally managed content

### 4. Unmanaged Store

Content not managed by DMS

```csv
unmanaged,,unmanaged,false
```

## Example: Using Stores

### Create Document with Content

```java
// Get DMS session
DMSSession session = dmsSessionFactory.getSession();

// Create document
Document doc = (Document) session.newObject("dm_document");
doc.setObjectName("example.pdf");

// Add content from file
File pdfFile = new File("/path/to/document.pdf");
Content content = doc.setFile(pdfFile, "pdf");

// Content automatically stored in default store
// Store directory: /Users/chris/hthome/stores/default/...

// Save
doc.save();

logger.info("Document saved with content in store: {}", 
    content.getStoreName());
```

### Query Content Location

```sql
-- In H2 Console
SELECT 
    s.object_name as doc_name,
    c.store_name,
    st.root_dir,
    c.content_size
FROM sysobject s
JOIN content c ON s.r_object_id = c.parent_id
LEFT JOIN dm_store st ON c.store_name = st.object_name;
```

## Troubleshooting

### Error: "Store not found"

**Symptom**:
```
Store 'default' not found
```

**Solutions**:
1. Create default store programmatically (see example above)
2. Enable CSV loading with valid stores.csv
3. Specify store name when creating content:
   ```java
   Content content = doc.setFile(file, "pdf", "mystore");
   ```

### Error: "Permission denied" on store directory

**Symptom**:
```
java.io.IOException: Permission denied: /Users/chris/hthome/stores/default
```

**Solution**:
```bash
# Fix permissions
chmod -R 755 /Users/chris/hthome/stores
chown -R $(whoami) /Users/chris/hthome/stores
```

### Store directory doesn't exist

**Solution**:
```bash
# Create directories
mkdir -p /Users/chris/hthome/stores/default
mkdir -p /Users/chris/hthome/stores/archive
```

Or in code:
```java
File storeDir = new File("/Users/chris/hthome/stores/default");
if (!storeDir.exists()) {
    storeDir.mkdirs();
}
```

## Best Practices

### Development

✅ **DO**:
- Start with `db-init.enabled: false` (no stores)
- Create documents without content initially
- Add stores when you need content storage
- Use programmatic store creation

```yaml
dms:
  db-init:
    enabled: false  # Start simple
```

### Production

✅ **DO**:
- Pre-configure stores via CSV or migration scripts
- Use separate stores for different purposes:
  - `default` - Regular documents
  - `archive` - Old/archived documents
  - `temp` - Temporary/draft content
- Set up backup for store directories
- Monitor disk space
- Use appropriate store types (file vs blob)

```yaml
dms:
  db-init:
    enabled: true
    fail-on-error: true  # Fail fast if stores not configured
```

## Configuration Matrix

| Scenario | db-init.enabled | stores.csv | Store Directories |
|----------|-----------------|------------|-------------------|
| **Learning/Testing** | `false` | Not needed | Not needed |
| **Basic DMS** | `false` | Not needed | Create as needed |
| **With Content** | `true` | Required | Must exist |
| **Production** | `true` | Required | Must exist + backup |

## Summary

### Default Configuration (Recommended)

```yaml
dms:
  enabled: true
  db-init:
    enabled: false  # No CSV loading
```

**Result**:
- ✅ No NullPointerException errors
- ✅ Clean startup logs
- ✅ Can still use DMS for document metadata
- ✅ Create stores programmatically when needed

### With Content Stores

```yaml
dms:
  enabled: true
  db-init:
    enabled: true
    fail-on-error: false  # or true if stores configured
    data-sets:
      - name: "stores"
        csv-file: "classpath:data/stores.csv"
        consumer: "com.hitorro.basedms.csvconsumers.StoreCSVConsumer"
```

**Requirements**:
1. `stores.csv` with valid paths
2. Store directories must exist
3. Write permissions on directories

## Quick Checklist

To eliminate the error:

- [ ] Disable `db-init.enabled` in `application.yml`
- [ ] Restart application
- [ ] No more NullPointerException in logs
- [ ] Create stores programmatically if needed later

To properly configure stores:

- [ ] Create store directories on file system
- [ ] Create `stores.csv` with correct paths
- [ ] Enable `db-init.enabled: true`
- [ ] Set `fail-on-error: true` (once working)
- [ ] Restart and verify in logs

## References

- See `readme_dms.md` for complete DMS documentation
- See Store class documentation for API details
- See content management examples in DMS tests

---

**Default configuration now has stores disabled** to avoid this error in the example application. Enable when you need content storage! ✅
