# Type System Indexing Debug Plan

## Goal
Prove each step of the type-based indexing pipeline works correctly, from JVS document → ExecutionBuilder → LuceneDocument → Searchable Index.

---

## Test 1: Type System Groups Configuration

**Question**: Do the demo types have groups configured for indexing?

**How to verify**:
```java
// In a debugger or test:
Type productType = JsonTypeSystem.getMe().getType("demo_product");
Field brandField = productType.getField("brand");
List<Group> groups = brandField.getGroups();

// Print or inspect:
// - Are groups null or empty?
// - If groups exist, what are their names?
// - Do any groups have method="identifier" or similar?
```

**Expected behavior**:
- Fields should have at least one group with an appropriate method (e.g., "identifier" for string fields)
- The group name should pass `GroupNameFilter.indexFilter`

**If this fails**: Groups are missing from type definitions. We need to add them to the type JSON files or understand how they should be configured.

---

## Test 2: LuceneFieldTypes Configuration Loading

**Question**: Are the field type mappings correctly loaded and accessible?

**How to verify**:
```java
// In SearchController or a test:
LuceneFieldTypes lfts = LuceneFieldTypes.getInstance();
LuceneFieldType identifierType = lfts.get("identifier");
LuceneFieldType textType = lfts.get("text");
LuceneFieldType longType = lfts.get("long");

// Print or inspect:
System.out.println("identifier type: " + identifierType);
System.out.println("text type: " + textType);
System.out.println("long type: " + longType);

// Check properties:
if (identifierType != null) {
    System.out.println("indexed: " + identifierType.isIndexed());
    System.out.println("stored: " + identifierType.isStored());
    System.out.println("tokenized: " + identifierType.isTokenized());
    System.out.println("indexType: " + identifierType.getIndexType());
}
```

**Expected behavior**:
- All field types should be non-null
- Each should have appropriate properties set
- `identifierType.getIndexType()` should return "identifier"

**If this fails**: Configuration file format might be wrong or path is incorrect.

---

## Test 3: ExecutionBuilder Creation

**Question**: Does the ExecutionBuilder create LuceneIndexerAction instances for fields?

**How to verify**:
```java
// In a test or SearchController initialization:
Type productType = JsonTypeSystem.getMe().getType("demo_product");
HashCache<Type, ExecutionBuilder> cache = Type.getExecBuilderCache("lucene", LuceneExecutionBuilderMapper.me);
ExecutionBuilder builder = cache.get(productType);

// Inspect the builder:
ExecutionNode root = builder.getExecutor();

// You'll need to traverse the execution tree to see what actions exist
// This is complex - alternatively, put a breakpoint in LuceneIndexerAction constructor
// and see if it's called for brand, sku, price fields when indexing a demo_product document
```

**Expected behavior**:
- ExecutionBuilder should be created successfully
- LuceneIndexerAction constructor should be called for each field that has appropriate groups
- Each action should have a non-null `lft` (LuceneFieldType)

**If this fails**: Either groups are missing, or GroupNameFilter.indexFilter is filtering them out.

---

## Test 4: Field Name Generation

**Question**: What field names are being generated for indexed fields?

**How to verify**:
```java
// Put breakpoint in LuceneIndexerAction.project() method at line 75:
// String fieldName = lpc.sb.toString();

// When indexing a demo_product with brand="ChrisTech", inspect:
// - What is the value of `fieldName`?
// - What is `lft.getIndexType()`?
// - What is the `lang` parameter?
// - What is `isMulti`?

// Expected: fieldName should be something like "brand.identifier_s"
```

**Expected behavior**:
- For brand field: `brand.identifier_s` (single-valued string identifier)
- For sku field: `sku.identifier_s`
- For price field: `price.long_s` (single-valued long)

**If this fails**: Field type mapping might be wrong or naming convention differs.

---

## Test 5: Lucene Document Fields

**Question**: What fields are actually added to the Lucene Document?

**How to verify**:
```java
// Put breakpoint in JVSLuceneIndexWriter.indexDocument() at line 76:
// Document doc = projectToLuceneDocument(jvs);

// After this line, inspect `doc`:
for (IndexableField field : doc.getFields()) {
    System.out.println("Field: " + field.name() + " = " + field.stringValue() + 
                      " (type: " + field.fieldType() + ")");
}

// Also check:
Document facetedDoc = facetsConfig.build(doc); // If still using FacetsConfig
// See if more fields are added
```

**Expected behavior**:
- Should see fields like: `brand.identifier_s`, `sku.identifier_s`, `price.long_s`, `id.id.identifier_s`
- Each field should have appropriate Lucene field type (StringField, NumericField, etc.)

**If this fails**: The projection step isn't working - fields aren't being added to the document.

---

## Test 6: Index Segments Verification

**Question**: Are the fields actually written to the Lucene index on disk?

**How to verify**:
```bash
# After indexing, check the index directory
ls -la /var/folders/.../hitorro-lucene-index/

# Use Luke (Lucene Index Toolbox) or write a test:
```

```java
// In a test:
IndexReader reader = DirectoryReader.open(config.getDirectory());
Document doc = reader.document(0); // First document

for (IndexableField field : doc.getFields()) {
    System.out.println("Stored field: " + field.name() + " = " + field.stringValue());
}

// Check indexed terms:
Terms terms = reader.terms("brand.identifier_s");
if (terms != null) {
    TermsEnum termsEnum = terms.iterator();
    while (termsEnum.next() != null) {
        System.out.println("Term in brand.identifier_s: " + termsEnum.term().utf8ToString());
    }
}
```

**Expected behavior**:
- Stored fields should include those marked as stored in lucene_fields.json
- Terms should exist for indexed fields
- For `brand.identifier_s`, should see term "ChrisTech"

**If this fails**: Fields are being created but not properly indexed or stored.

---

## Test 7: Query Parsing

**Question**: Is the query correctly parsed to search the right field?

**How to verify**:
```java
// Put breakpoint in SearchController.search() or JVSQueryParser

// When query is "brand.identifier_s:ChrisTech", inspect:
QueryParser parser = new QueryParser("brand.identifier_s", analyzer);
Query query = parser.parse("brand.identifier_s:ChrisTech");

System.out.println("Parsed query: " + query.toString());
System.out.println("Query class: " + query.getClass().getName());

// Expected: TermQuery[brand.identifier_s:ChrisTech] or similar
```

**Expected behavior**:
- Query should parse to a TermQuery for exact match on non-tokenized field
- Field name in query should match indexed field name exactly

**If this fails**: Query parser might be using wrong field name or analyzer.

---

## Test 8: Search Execution

**Question**: Does the search actually look for documents?

**How to verify**:
```java
// In JVSLuceneSearcher.search() method:

IndexSearcher searcher = getIndexSearcher();
TopDocs topDocs = searcher.search(query, maxResults);

System.out.println("Total hits: " + topDocs.totalHits);
System.out.println("Score docs length: " + topDocs.scoreDocs.length);

for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
    Document doc = searcher.doc(scoreDoc.doc);
    System.out.println("Found doc " + scoreDoc.doc + " with score " + scoreDoc.score);
    System.out.println("Fields: " + doc.getFields());
}
```

**Expected behavior**:
- topDocs.totalHits should be > 0 if document matches
- Should be able to retrieve the document

**If this fails**: Either query doesn't match, or searcher isn't seeing the indexed data.

---

## Suggested Debug Session Flow

1. **Start with Test 1**: Check if groups exist on demo_product fields
   - If groups are null → **Root cause found**: Types need group configuration
   
2. **If groups exist, move to Test 3**: Check if ExecutionBuilder creates actions
   - Put breakpoint in `LuceneIndexerAction` constructor
   - Index a demo_product document
   - See if constructor is called for brand/sku/price
   
3. **If actions are created, go to Test 4**: Check field naming
   - Breakpoint in `LuceneIndexerAction.project()` at line 75
   - See what field names are generated
   
4. **Then Test 5**: Check Lucene Document
   - Breakpoint after `projectToLuceneDocument(jvs)`
   - Inspect what fields are in the Document
   
5. **Then Test 6**: Check index on disk
   - Use Luke or write a test to read the index
   - Verify terms exist for the fields

6. **Finally Tests 7 & 8**: Debug query and search
   - Verify query parsing uses correct field names
   - Verify search execution finds the documents

---

## Quick Diagnostic Test

Create this as a unit test or REST endpoint:

```java
@GetMapping("/api/search/diagnostic")
public ResponseEntity<Map<String, Object>> diagnostic() {
    Map<String, Object> result = new HashMap<>();
    
    // 1. Check type system
    Type productType = JsonTypeSystem.getMe().getType("demo_product");
    result.put("type_exists", productType != null);
    if (productType != null) {
        Field brandField = productType.getField("brand");
        result.put("brand_field_exists", brandField != null);
        if (brandField != null) {
            List<Group> groups = brandField.getGroups();
            result.put("brand_groups", groups != null ? groups.size() : 0);
            if (groups != null && !groups.isEmpty()) {
                List<String> groupMethods = groups.stream()
                    .map(g -> g.getMethod())
                    .collect(Collectors.toList());
                result.put("brand_group_methods", groupMethods);
            }
        }
    }
    
    // 2. Check field types
    LuceneFieldTypes lfts = LuceneFieldTypes.getInstance();
    result.put("identifier_type_loaded", lfts.get("identifier") != null);
    result.put("text_type_loaded", lfts.get("text") != null);
    result.put("long_type_loaded", lfts.get("long") != null);
    
    // 3. Check execution builder
    if (productType != null) {
        try {
            HashCache<Type, ExecutionBuilder> cache = 
                Type.getExecBuilderCache("lucene", LuceneExecutionBuilderMapper.me);
            ExecutionBuilder builder = cache.get(productType);
            result.put("execution_builder_created", builder != null);
        } catch (Exception e) {
            result.put("execution_builder_error", e.getMessage());
        }
    }
    
    return ResponseEntity.ok(result);
}
```

This will give us a quick overview of the state without stepping through code.

---

## Expected Outcomes

After running these tests, we should know:
1. ✅ or ❌ Do types have groups configured?
2. ✅ or ❌ Are field types loaded?
3. ✅ or ❌ Is ExecutionBuilder creating field actions?
4. ✅ or ❌ Are correct field names being generated?
5. ✅ or ❌ Are fields being added to Lucene documents?
6. ✅ or ❌ Are fields written to the index?
7. ✅ or ❌ Are queries parsed correctly?
8. ✅ or ❌ Are searches finding documents?

Each ❌ points us to the exact location of the problem.
