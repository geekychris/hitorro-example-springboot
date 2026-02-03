# Type Groups and Indexing

## The Real Issue

While the `lucene_fields.json` configuration is loading correctly (confirmed by debugging), fields are still not being indexed. The root cause is the **type system groups**.

### How Indexing Works

1. `LuceneExecutionBuilderMapper` creates an `ExecutionBuilder` for each type
2. It uses `GroupNameFilter.indexFilter` as the predicate to filter fields
3. **Only fields with appropriate groups are included in the execution plan**
4. Without groups, fields are skipped even though field types are configured

### Current Type Definitions

The `demo_product` type fields show:
```json
{
  "name": "brand",
  "type": "string",
  "groups": null  // ← This is the problem!
}
```

### What's Needed

Fields need to have groups defined that include indexing information. The group system tells the ExecutionBuilder which operations to perform on each field.

## Verification Steps

When debugging `LuceneIndexerAction` construction:

1. Check if `LuceneExecutionBuilderMapper.getFactory()` is creating actions for brand/sku/price
2. Verify that `GroupNameFilter.indexFilter` is matching these fields
3. Check the `Group` object associated with each field - does it have the right method?

## Possible Solutions

### Option 1: Update Type Definitions
Add groups to the type JSON files to include indexing groups for each field.

### Option 2: Default Group Behavior
Modify `LuceneExecutionBuilderMapper` to include fields even without explicit groups, using a default group based on field type.

### Option 3: Use Simple Document Fallback
The `JVSLuceneIndexWriter.createSimpleDocument()` method indexes all fields as text without using the type system. This could be enhanced to use proper field types.

## Testing Without Type System

To verify the Lucene integration works independently of groups, try indexing a document without a type:

```bash
curl -X POST http://localhost:8080/api/search/index \
  -H "Content-Type: application/json" \
  -d '{"brand":"ChrisTech","sku":"TEST-001","description":"Test product"}'
```

This should trigger the `createSimpleDocument()` path which doesn't require type system groups.

## Next Investigation

1. **Check the type JSON files** in `$HT_BIN/config/jsonconfigs/types/` to see if demo types have group definitions
2. **Debug the ExecutionBuilder** creation to see which fields are being included
3. **Examine how other Hitorro modules** (like hitorro-solr) handle groups for indexing
4. **Review GroupNameFilter.indexFilter** implementation to understand the predicate logic

The configuration loading is working - the issue is the type system integration layer.
