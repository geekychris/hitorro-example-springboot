# Lucene Search Integration Example

This example demonstrates the integration of the **hitorro-index** module with Spring Boot and React.

## Overview

The search integration provides:
- **Lucene-based indexing** of JVS documents
- **Full-text search** with fielded queries
- **Faceted search** for filtering and navigation
- **Multilingual support** via language-specific analyzers
- **NDJson streaming** for bulk operations
- **Interactive React UI** for testing

## Architecture

### Backend (Spring Boot)

**SearchController** (`src/main/java/com/hitorro/example/controller/SearchController.java`)

REST API endpoints:
- `POST /api/search/index` - Index a single document
- `POST /api/search/index/batch` - Index multiple documents
- `POST /api/search/index/stream` - Index from NDJson stream
- `GET /api/search/query` - Search with optional faceting
- `GET /api/search/query/stream` - Search with NDJson response
- `GET /api/search/facets` - Get available facet fields
- `GET /api/search/stats` - Get index statistics
- `DELETE /api/search/index` - Clear the index

The controller uses:
- `JVSLuceneIndexWriter` for indexing operations
- `JVSLuceneSearcher` for search operations
- `IndexConfig` for index configuration (filesystem-based)
- `Type` and `JsonTypeSystem` for field resolution

### Frontend (React)

**SearchPage** (`react-app/src/pages/SearchPage.tsx`)

Interactive UI with:
- Document indexing section with sample data loader
- Search interface with query builder
- Facet selection for filtered search
- Results display with expandable JSON viewer
- Index statistics dashboard
- NDJson streaming support

## Usage

### Starting the Application

1. Build the backend:
```bash
cd /Users/chris/hitorro/hitorro-example-springboot
mvn clean install
```

2. Start the Spring Boot application:
```bash
mvn spring-boot:run
```

3. Access the React UI:
```
http://localhost:8080
```

4. Navigate to the **Search** tab

### Example Workflow

1. **Load Sample Documents**
   - Click "Load Sample Documents" to index 3 example documents
   - Documents have titles and descriptions in English
   - Types include `core_sysobject` and `article`

2. **Search Documents**
   - Use queries like:
     - `*:*` - Match all documents
     - `title.mls:lucene` - Search in title field
     - `description.mls:search` - Search in description field
     - `type:core_sysobject` - Filter by type
     - `title.mls:lucene AND type:article` - Combined query

3. **Use Facets**
   - Select facet fields (type, id.domain, dates.created, dates.modified)
   - View facet counts in the results
   - Helps understand data distribution

4. **Index Custom Documents**
   - Modify the JSON in the text area
   - Must be valid JVS format with `type` field
   - Click "Index Document" to add to index

### API Examples

#### Index a Document
```bash
curl -X POST http://localhost:8080/api/search/index \\
  -H "Content-Type: application/json" \\
  -d '{
    "id": {"domain": "sysobject", "did": "doc001"},
    "type": "core_sysobject",
    "dates": {"created": "2024-01-15T10:00:00Z"},
    "title": {"mls": [{"lang": "en", "text": "Test Document"}]}
  }'
```

#### Search with Facets
```bash
curl "http://localhost:8080/api/search/query?query=title.mls:test&maxResults=10&facets=type,id.domain"
```

#### Get Statistics
```bash
curl http://localhost:8080/api/search/stats
```

## Index Configuration

The index is created in:
```
${java.io.tmpdir}/hitorro-lucene-index
```

Configuration:
- **Storage**: Filesystem-based (FSDirectory)
- **Default Type**: `core_sysobject`
- **Default Language**: English (`en`)
- **Analyzer**: Standard analyzer with language-specific tokenizers
- **Field Mapping**: Automatic via JVS Type System

## Field Resolution

The hitorro-index module uses the JSON Type System to automatically resolve field names to Lucene field specifications:

- `title.mls` → `title.mls.text_en_s` (English single-valued text field)
- `description.mls` → `description.mls.text_en_s` 
- `type` → `type.identifier_s` (Single-valued identifier)
- `dates.created` → `dates.created.date_s` (Single-valued date)

The `.text_LANG_s/m` suffix encodes:
- Field type (text, identifier, date, etc.)
- Language code (en, de, fr, etc.)
- Single/multiple valued (s/m)

## Supported Query Syntax

The search supports Lucene query syntax:
- **Term queries**: `lucene`
- **Phrase queries**: `"apache lucene"`
- **Fielded queries**: `title.mls:lucene`
- **Boolean operators**: `AND`, `OR`, `NOT`
- **Wildcards**: `luc*`, `luc?ne`
- **Ranges**: `dates.created:[2024-01-01 TO 2024-12-31]`
- **Fuzzy**: `lucen~`
- **Proximity**: `"apache lucene"~10`

## Multilingual Support

The index supports 30+ languages with language-specific analyzers:
- Arabic (ar), Bulgarian (bg), Catalan (ca)
- Danish (da), German (de), Greek (el)
- English (en), Spanish (es), Basque (eu)
- Persian (fa), Finnish (fi), French (fr)
- Irish (ga), Galician (gl), Hindi (hi)
- Hungarian (hu), Armenian (hy), Indonesian (id)
- Italian (it), Japanese (ja), Korean (ko)
- And more...

To index multilingual content:
```json
{
  "title": {
    "mls": [
      {"lang": "en", "text": "Hello World"},
      {"lang": "de", "text": "Hallo Welt"},
      {"lang": "fr", "text": "Bonjour le monde"}
    ]
  }
}
```

## Swagger UI

The REST API is documented and testable via Swagger UI:
```
http://localhost:8080/swagger-ui.html
```

Look for the **Search** section to explore all endpoints.

## Troubleshooting

### Index Not Found
If you see errors about index not found:
1. Index at least one document
2. The index is created on first document insertion
3. Check the logs for index path

### Type Not Found
If you see errors about type not found:
- Ensure your JVS documents have a valid `type` field
- Check that the type is defined in your type system configuration
- Default type `core_sysobject` should always be available

### Search Returns No Results
- Verify documents are actually indexed (check stats)
- Try a simple query like `*:*` to match all
- Check query syntax (Lucene query parser is strict)
- Ensure field names match indexed field names

## Next Steps

- **Persistence**: Configure a permanent index location in production
- **Type Configuration**: Customize field types via `lucene_fields.json`
- **Analyzers**: Add custom analyzers for domain-specific tokenization
- **Security**: Add authentication/authorization for index operations
- **Scaling**: Consider using separate indexes for different document types
- **Monitoring**: Add metrics for index size, search latency, etc.

## References

- [hitorro-index Module](../hitorro-index/README.md)
- [Apache Lucene Documentation](https://lucene.apache.org/)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [React Query](https://tanstack.com/query/latest)
