# Type System Page - Updated for JSON Type System

## Changes Made

### 1. Updated Example Document
**Before**: Simple object with `name` and `email`
```json
{
  "name": "John Doe",
  "email": "john@example.com"
}
```

**After**: Realistic sysobject example from `data/typeexamples/sysobject_example.json`
```json
{
  "id": {
    "domain": "sysobject",
    "did": "1234"
  },
  "type": "core_sysobject",
  "dates": {
    "created": "2018-04-23T18:25:43Z",
    "modified": "2018-04-23T18:25:43Z"
  },
  "title": {
    "mls": [
      {
        "lang": "en",
        "text": "The quick brown fox..."
      },
      {
        "lang": "de",
        "text": "Der schnelle braune Fuchs..."
      }
    ]
  }
}
```

### 2. Clarified Type System Context

Updated descriptions to emphasize this is the **JSON Type System (JVS)**, not DMS types:

- Main description mentions "JSON Type System (JVS)" and clarifies it's from `hitorro-util`
- Type Explorer section explicitly states these are **not DMS entity types**
- Added examples of what types are: `core_sysobject`, `document`, custom content types
- Clarified types come from `JsonTypeSystem.getMe()` and are loaded from `HT_BIN/types/`

### 3. Added Helper Buttons

Added two convenience buttons:
- **Load Example**: Loads the sysobject example
- **Clear**: Clears the input to start fresh

### 4. Updated Placeholders and Labels

- Changed placeholder from generic `{"key": "value"}` to `{"type": "core_sysobject", ...}`
- Enhanced section headers for clarity

## What This Page Does

### JSON Type System (JVS)
The Type System page is specifically for the **JSON Type System** which:
- Lives in `hitorro-util` module
- Uses `JsonTypeSystem.getMe()` to access types
- Loads type definitions from `HT_BIN/types/` directory
- Provides enrichment via `JVS2JVSEnrichMapper`
- Works with JVS objects (JSON + type metadata)

### NOT DMS Types
This page does **not** deal with:
- Hibernate/JPA entity types (Document, Content, Container, etc.)
- DMS database schema
- `@Entity` annotated classes

Those are different - DMS types are for the database/persistence layer, while JVS types are for JSON documents with rich type metadata and computed fields.

## How to Use

1. **Load the example** by clicking "Load Example" button
2. **Or paste your own** JSON object with a `type` field
3. **Click "Enrich"** to apply JVS2JVSEnrichMapper
4. **View results** showing:
   - Original vs enriched comparison
   - Fields added/computed
   - Type information
   - Field paths and types

## Example Types to Try

If you have type definitions in `HT_BIN/types/`, try:
- `core_sysobject` - Base system object
- `document` - Document type
- `content` - Content type
- Custom types defined in your types directory

## Backend Implementation

The JVSController (`/api/jvs/*`) uses:
- `JVS2JVSEnrichMapper` - Applies enrichment
- `JsonTypeSystem.getMe()` - Accesses type registry
- `ObjectMapper` - Parses JSON strings to JsonNode
- `JVS(JsonNode)` - Creates JVS objects

## Notes

The type listing at the bottom is currently simplified because `JsonTypeSystem` may not expose all types programmatically. The types are defined in JSON/XML files in the `HT_BIN/types/` directory and loaded at startup.
