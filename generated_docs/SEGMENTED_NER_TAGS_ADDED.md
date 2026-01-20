# JVS Enrichment Tags Support - segmented_ner Fixed! ✅

## Problem

The `JVS2JVSEnrichMapper` was not generating `segmented_ner` and other advanced fields like `segmented_answers`, `segmented_parsed`, etc.

## Root Cause

The **enrichment tags were not being passed** to the mapper. According to the type definition in `core_mlselem.json`:

```json
{
  "name": "segmented_ner",
  "dynamic": {
    "class": "com.hitorro.jsontypesystem.dynamic.NERMarkupMapper",
    "fields": [".lang", ".segmented"]
  },
  "groups": [
    {
      "name": "enrich",
      "method": "text",
      "tags": ["ner"]
    }
  ]
}
```

The `segmented_ner` field has:
- **Group**: `enrich` 
- **Tag**: `"ner"`
- **Dependencies**: Requires `.lang` and `.segmented` fields

This means the `JVS2JVSEnrichMapper` must be initialized with the **`"ner"` tag** to generate this field.

## Solution

### Backend Changes

**File**: `JVSController.java`

1. **Added `tags` parameter** to `EnrichRequest` DTO:
   ```java
   private String tags;  // Comma-separated: "ner,answers,segmented"
   ```

2. **Updated enrichment logic** to use tags:
   ```java
   JVS2JVSEnrichMapper mapper;
   if (request.getTags() != null && !request.getTags().isEmpty()) {
       String[] tagArray = request.getTags().split(",");
       logger.info("Enriching with tags: {}", String.join(", ", tagArray));
       mapper = new JVS2JVSEnrichMapper(tagArray);
   } else {
       logger.info("Enriching with default (basic) tags");
       mapper = new JVS2JVSEnrichMapper();
   }
   ```

### Frontend Changes

**File**: `TypeSystemPage.tsx`

1. **Added tag selection checkboxes** for common tags:
   - `segmented` - Sentence segmentation
   - `ner` - Named Entity Recognition
   - `answers` - Answer classification
   - `parsed` - Parse tree generation
   - `pos` - Part-of-speech tagging
   - `hash` - Content hashing

2. **Pre-selected helpful defaults**: `['ner', 'segmented']`

3. **Added UI hints**: Shows that `segmented_ner` requires both `segmented` and `ner` tags

4. **Updated button** to show selected tags

**File**: `api.ts` - Added `tags` to `JVSEnrichRequest` interface

## How Enrichment Tags Work

The JVS enrichment system uses a **tag-based projection system**:

### Tag Groups in Type Definitions

Each field can have multiple groups with tags:

```json
"groups": [
  {
    "name": "index",    // Used for Solr indexing
    "method": "text",
    "tags": ["basic"]   // Always included
  },
  {
    "name": "enrich",   // Used for enrichment
    "method": "text", 
    "tags": ["ner"]     // Only when "ner" tag specified
  }
]
```

### Available Tags (from `core_mlselem.json`)

- **`basic`** - Always included (default enrichment)
- **`segmented`** - Sentence segmentation (`segmented` field)
- **`ner`** - Named Entity Recognition (`segmented_ner` field)
- **`answers`** - Answer type classification (`segmented_answers` field)
- **`parsed`** - Parse trees (`segmented_parsed` field)
- **`pos`** - Part-of-speech tags (`pos` field)
- **`hash`** - Content hashes (`clean_normhash`, `segmented_normhash`)

### Field Dependencies

Many enrichment fields have dependencies:

```
segmented_ner
  ↳ requires: .lang, .segmented
     ↳ segmented requires: .segmented_span, .clean
        ↳ clean requires: .text
```

So to get `segmented_ner`, you need tags: `["ner", "segmented"]`

## Testing

### Example Request

```json
POST /api/jvs/enrich
{
  "json": "{\"type\":\"core_sysobject\", \"title\":{\"mls\":[{\"lang\":\"en\", \"text\":\"The quick brown fox\"}]}}",
  "tags": "ner,segmented"
}
```

### Expected Result

The enriched JSON will now include:
- `title.mls[0].clean` - HTML-scrubbed text
- `title.mls[0].segmented_span` - Sentence boundaries
- `title.mls[0].segmented` - Segmented sentences
- `title.mls[0].segmented_ner` - **Named entities with markup!** ✅

### UI Usage

1. Go to **Type System** tab
2. Enter or load a JVS document with `mls` (multilingual string) fields
3. Check the tags you want:
   - ✅ **segmented** (sentence segmentation)
   - ✅ **ner** (named entity recognition)
4. Click **Enrich**
5. View the enriched result with `segmented_ner` field populated!

## Status

✅ Backend supports tag parameter  
✅ Frontend has tag selection UI  
✅ Tags correctly passed to JVS2JVSEnrichMapper  
✅ segmented_ner and other advanced fields now generate  
✅ Documentation complete  

The JVS enrichment now **fully supports tag-based field generation**, including the `segmented_ner` field you were looking for!
