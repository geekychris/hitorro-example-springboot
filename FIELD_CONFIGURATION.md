# Lucene Field Configuration Issue

## Problem

Search queries return 0 results for all fields except wildcard queries (`*:*`). Only the `id` field appears in search results.

### Root Cause

The `LuceneFieldTypes` configuration system requires a `lucene_fields.json` file at:
```
$HT_BIN/config/jsonconfigs/lucene/lucene_fields.json
```

Without this configuration:
1. `LuceneFieldTypes.getInstance()` returns an empty map
2. `LuceneIndexerAction.lft` is null (line 50)
3. The action returns early without indexing any fields (line 62)
4. Only the ID field is indexed (handled separately)
5. Search queries find documents but can't match on field content

## Solution Created

Created `/Users/chris/hitorro/config/jsonconfigs/lucene/lucene_fields.json` with field type definitions for:
- `text` - Tokenized, i18n text fields (mls)
- `textmarkup` - Tokenized markup text
- `identifier` - Non-tokenized strings (brand, sku, etc.)
- `long`, `int`, `double` - Numeric types  
- `date` - Date fields
- `boolean` - Boolean fields

## Field Name Convention

Fields are indexed with suffixes based on type and cardinality:
- **i18n fields**: `path.indexType_lang_m/s`
  - Example: `title.mls.text_en_s` for single-valued English text
  - Example: `description.text_en_m` for multi-valued English text

- **Non-i18n fields**: `path.indexType_m/s`
  - Example: `brand.identifier_s` for single-valued brand
  - Example: `category.identifier_m` for multi-valued category

Where:
- `_s` = single-valued
- `_m` = multi-valued
- `indexType` = field type name from lucene_fields.json
- `lang` = ISO language code (en, es, fr, etc.)

## Expected Search Queries

Once configuration is properly loaded:

```bash
# Search by brand
curl "http://localhost:8080/api/search/query?query=brand.identifier_s:ChrisTech"

# Search by title (English)
curl "http://localhost:8080/api/search/query?query=title.text_en_s:laptop"

# Search by SKU
curl "http://localhost:8080/api/search/query?query=sku.identifier_s:CHRIS-001"

# Wildcard search (works now)
curl "http://localhost:8080/api/search/query?query=*:*"
```

## Current Status

⚠️ **Configuration loading is failing silently**

The `LuceneFieldTypes.loadConfiguration()` method catches all exceptions and continues with an empty map. This needs further investigation:

1. Check if `Name2JsonMapper` is finding the file
2. Verify JSON structure matches expected format
3. Add logging to configuration loading process
4. Consider alternative configuration approach (classpath resource, application.yml, etc.)

## Next Steps

1. **Add logging** to `LuceneFieldTypes.loadConfiguration()` to see why it fails
2. **Verify JSON format** matches what `MapProperty` expects
3. **Test with hardcoded field types** in `LuceneFieldTypes` constructor
4. **Consider moving config** to `application.yml` or classpath resources for Spring Boot integration
5. **Update AGENTS.md** with field naming conventions and search examples

## Temporary Workaround

Until configuration loading is fixed, fields won't be searchable. Only wildcard queries (`*:*`) will work to verify documents are indexed.

The system is currently:
- ✅ Indexing documents (ID field only)
- ✅ Committing to Lucene index
- ✅ Refreshing searcher properly
- ❌ Not indexing other fields (config not loaded)
- ❌ Field searches return 0 results
