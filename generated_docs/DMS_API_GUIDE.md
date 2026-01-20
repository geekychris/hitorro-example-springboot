# Document Management System (DMS) API Guide

## Overview

The **DocumentManagementController** provides a comprehensive REST API for managing documents in the Hitorro DMS. This guide covers all available operations with examples.

## Base URL

```
http://localhost:8080/api/dms
```

## API Documentation

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Features

### ✅ Document CRUD Operations
- Create, read, update, and delete documents
- Manage document metadata (title, author, notes, etc.)

### ✅ Version Management  
- Create new document versions
- View version history
- Track version lineage

### ✅ Container Management
- Attach documents to containers (folders, forums, etc.)
- Detach documents from containers
- List all containers for a document

### ✅ Category/Tagging
- Add categories to documents
- Remove categories
- Search by category

### ✅ Query & Search
- Flexible document queries
- Search by title, author, date range
- Category-based search
- Container-based queries

### ✅ Content Listing
- List all content attached to documents
- View content metadata

## API Endpoints

### Document CRUD

#### Create Document
```bash
POST /api/dms/documents
Content-Type: application/json

{
  "title": "My Document",
  "note": "This is a test document",
  "creator": "john.doe",
  "realm": "default",
  "authorId": 1,
  "categories": [
    {"domain": "type", "value": "report"},
    {"domain": "department", "value": "engineering"}
  ]
}
```

**Response:** `201 Created`
```json
{
  "id": 123,
  "guid": "550e8400-e29b-41d4-a716-446655440000",
  "title": "My Document",
  "note": "This is a test document",
  "creator": "john.doe",
  "realm": "default",
  "versionLabel": "1.0",
  "creationDate": "2026-01-14T20:00:00Z",
  "modifiedDate": "2026-01-14T20:00:00Z",
  "authoredDate": "2026-01-14T20:00:00Z",
  "authorId": 1,
  "authorName": "John Doe",
  "categories": [
    {"domain": "type", "value": "report"},
    {"domain": "department", "value": "engineering"}
  ],
  "contentCount": 0,
  "canonicalId": null,
  "parentVersionId": null
}
```

#### Get Document
```bash
GET /api/dms/documents/123
```

**Response:** `200 OK` (same structure as create response)

#### Update Document
```bash
PUT /api/dms/documents/123
Content-Type: application/json

{
  "title": "Updated Title",
  "note": "Updated note",
  "authorId": 2
}
```

**Response:** `200 OK`

#### Delete Document
```bash
DELETE /api/dms/documents/123
```

**Response:** `200 OK`
```json
{
  "status": "success",
  "message": "Document deleted successfully",
  "documentId": 123
}
```

### Content Management

#### List Document Content
```bash
GET /api/dms/documents/123/content/list
```

**Response:** `200 OK`
```json
[
  {
    "id": 456,
    "originalFileName": "report.pdf",
    "contentSize": 1048576,
    "storeName": "default",
    "creationDate": "2026-01-14T20:00:00Z",
    "width": 0,
    "height": 0,
    "durationSeconds": 0,
    "resolutionAux": null,
    "parentRenditionId": null,
    "renditionCount": 2
  }
]
```

### Versioning

#### Create New Version
```bash
POST /api/dms/documents/123/version
Content-Type: application/json

{
  "note": "Version 1.1 - Added new section"
}
```

**Response:** `201 Created`
```json
{
  "id": 124,
  "guid": "550e8400-e29b-41d4-a716-446655440001",
  "title": "My Document",
  "versionLabel": "1.1",
  "canonicalId": 123,
  "parentVersionId": 123,
  ...
}
```

#### Get Version History
```bash
GET /api/dms/documents/123/versions
```

**Response:** `200 OK`
```json
[
  {
    "id": 123,
    "versionLabel": "1.0",
    "creationDate": "2026-01-14T20:00:00Z",
    "modifiedDate": "2026-01-14T20:00:00Z",
    "note": "Initial version"
  },
  {
    "id": 124,
    "versionLabel": "1.1",
    "creationDate": "2026-01-14T20:05:00Z",
    "modifiedDate": "2026-01-14T20:05:00Z",
    "note": "Version 1.1 - Added new section"
  }
]
```

### Container Management

#### Attach Document to Container
```bash
POST /api/dms/documents/123/containers/789
```

**Response:** `200 OK`
```json
{
  "status": "success",
  "message": "Document attached to container",
  "documentId": 123,
  "containerId": 789
}
```

#### Detach Document from Container
```bash
DELETE /api/dms/documents/123/containers/789
```

**Response:** `200 OK`
```json
{
  "status": "success",
  "message": "Document detached from container",
  "documentId": 123,
  "containerId": 789
}
```

#### List Document Containers
```bash
GET /api/dms/documents/123/containers
```

**Response:** `200 OK`
```json
[
  {
    "id": 789,
    "guid": "550e8400-e29b-41d4-a716-446655440002",
    "description": "Engineering Documents Folder",
    "type": "Folder"
  }
]
```

### Category Management

#### Add Category
```bash
POST /api/dms/documents/123/categories
Content-Type: application/json

{
  "domain": "status",
  "value": "published"
}
```

**Response:** `200 OK`
```json
{
  "status": "success",
  "message": "Category added",
  "domain": "status",
  "value": "published"
}
```

#### Remove Category
```bash
DELETE /api/dms/documents/123/categories?domain=status&value=published
```

**Response:** `200 OK`
```json
{
  "status": "success",
  "message": "Category removed",
  "domain": "status",
  "value": "published"
}
```

### Query and Search

#### Query Documents
```bash
POST /api/dms/documents/query
Content-Type: application/json

{
  "title": "report",
  "creator": "john.doe",
  "realm": "default",
  "createdAfter": "2026-01-01T00:00:00Z",
  "createdBefore": "2026-12-31T23:59:59Z",
  "orderBy": "modifiedDate",
  "descending": true,
  "maxResults": 50
}
```

**Response:** `200 OK`
```json
[
  {
    "id": 123,
    "title": "Annual Report 2026",
    ...
  },
  {
    "id": 124,
    "title": "Quarterly Report Q1",
    ...
  }
]
```

#### Search by Category
```bash
GET /api/dms/documents/search/category?domain=type&value=report
```

**Response:** `200 OK` (array of documents)

#### Get Documents in Container
```bash
GET /api/dms/containers/789/documents?maxResults=100
```

**Response:** `200 OK` (array of documents)

## Common Query Parameters

### QueryRequest Fields
- `title` - Search by title (uses LIKE %...%)
- `authorId` - Filter by author ID
- `creator` - Filter by creator username
- `realm` - Filter by realm
- `createdAfter` - Documents created after this date
- `createdBefore` - Documents created before this date
- `orderBy` - Field to sort by (e.g., "modifiedDate", "title")
- `descending` - Sort descending (default: false)
- `maxResults` - Maximum number of results (default: 100)

## Error Responses

### 404 Not Found
```json
{
  "timestamp": "2026-01-14T20:00:00Z",
  "status": 404,
  "error": "Not Found",
  "path": "/api/dms/documents/999"
}
```

### 503 Service Unavailable
Returned when DMS session factory is not available.

### 500 Internal Server Error
Returned when an unexpected error occurs. Check server logs for details.

## Example Workflows

### Creating a Complete Document

```bash
# 1. Create document
curl -X POST http://localhost:8080/api/dms/documents \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Project Proposal",
    "note": "Q1 2026 Project Proposal",
    "creator": "jane.smith",
    "categories": [
      {"domain": "type", "value": "proposal"},
      {"domain": "priority", "value": "high"}
    ]
  }'

# Response: {"id": 123, ...}

# 2. Add categories
curl -X POST http://localhost:8080/api/dms/documents/123/categories \
  -H "Content-Type: application/json" \
  -d '{"domain": "status", "value": "draft"}'

# 3. Attach to container
curl -X POST http://localhost:8080/api/dms/documents/123/containers/789

# 4. Create new version
curl -X POST http://localhost:8080/api/dms/documents/123/version \
  -H "Content-Type: application/json" \
  -d '{"note": "Revised version after review"}'
```

### Searching Documents

```bash
# Search by title
curl -X POST http://localhost:8080/api/dms/documents/query \
  -H "Content-Type: application/json" \
  -d '{
    "title": "proposal",
    "orderBy": "modifiedDate",
    "descending": true,
    "maxResults": 20
  }'

# Search by category
curl "http://localhost:8080/api/dms/documents/search/category?domain=type&value=proposal"

# Get documents in a folder
curl "http://localhost:8080/api/dms/containers/789/documents?maxResults=50"
```

### Managing Document Versions

```bash
# Get current document
curl http://localhost:8080/api/dms/documents/123

# Create new version
curl -X POST http://localhost:8080/api/dms/documents/123/version \
  -H "Content-Type: application/json" \
  -d '{"note": "Version 1.1"}'

# View version history
curl http://localhost:8080/api/dms/documents/123/versions
```

## Data Models

### DocumentResponse
- `id` - System ID
- `guid` - Global unique identifier
- `title` - Document title
- `note` - Document notes
- `creator` - Creator username
- `realm` - Security realm
- `versionLabel` - Version (e.g., "1.0", "1.1")
- `creationDate` - When created
- `modifiedDate` - Last modified
- `authoredDate` - Authoring date
- `authorId` - Author user ID
- `authorName` - Author display name
- `categories` - Array of category tags
- `contentCount` - Number of content items
- `canonicalId` - Root version ID
- `parentVersionId` - Previous version ID

### ContentResponse
- `id` - Content ID
- `originalFileName` - Original file name
- `contentSize` - Size in bytes
- `storeName` - Storage location name
- `creationDate` - When uploaded
- `width` - Image/video width (if applicable)
- `height` - Image/video height (if applicable)
- `durationSeconds` - Video/audio duration
- `resolutionAux` - Rendition type info
- `parentRenditionId` - Parent content ID (for renditions)
- `renditionCount` - Number of renditions

### CategoryInfo
- `domain` - Category domain/namespace
- `value` - Category value

### ContainerInfo
- `id` - Container ID
- `guid` - Global unique identifier
- `description` - Container description
- `type` - Container type (Folder, Forum, etc.)

## Configuration

The DMS controller requires a `DMSSessionFactory` bean to be available. This is automatically configured when:

```yaml
hitorro:
  dms:
    enabled: true
```

## Best Practices

### 1. Use Transactions
All DMS operations are automatically wrapped in transactions. Failed operations are rolled back.

### 2. Set Realms for Multi-Tenancy
Use the `realm` field to partition documents for different tenants or security domains.

### 3. Organize with Containers
Use containers (folders, forums) to organize related documents hierarchically.

### 4. Tag with Categories
Use categories for flexible cross-cutting organization (status, type, priority, etc.).

### 5. Version Important Documents
Create new versions for significant changes to maintain history.

### 6. Limit Query Results
Always set `maxResults` to avoid retrieving too many documents at once.

## Integration Examples

### Java/Spring

```java
@Autowired
private RestTemplate restTemplate;

public DocumentResponse createDocument(String title) {
    CreateDocumentRequest request = new CreateDocumentRequest();
    request.setTitle(title);
    request.setCreator("system");
    
    return restTemplate.postForObject(
        "http://localhost:8080/api/dms/documents",
        request,
        DocumentResponse.class
    );
}
```

### JavaScript/Fetch API

```javascript
async function createDocument(title) {
  const response = await fetch('http://localhost:8080/api/dms/documents', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      title: title,
      creator: 'webapp',
      realm: 'default'
    })
  });
  
  return await response.json();
}
```

### Python

```python
import requests

def create_document(title):
    response = requests.post(
        'http://localhost:8080/api/dms/documents',
        json={
            'title': title,
            'creator': 'script',
            'realm': 'default'
        }
    )
    return response.json()
```

## Testing

### Using curl

```bash
# Create test document
DOC_ID=$(curl -s -X POST http://localhost:8080/api/dms/documents \
  -H "Content-Type: application/json" \
  -d '{"title": "Test Doc", "creator": "test"}' \
  | jq -r '.id')

echo "Created document ID: $DOC_ID"

# Get document
curl http://localhost:8080/api/dms/documents/$DOC_ID | jq

# Update document
curl -X PUT http://localhost:8080/api/dms/documents/$DOC_ID \
  -H "Content-Type: application/json" \
  -d '{"title": "Updated Test Doc"}' | jq

# Delete document
curl -X DELETE http://localhost:8080/api/dms/documents/$DOC_ID | jq
```

### Using Swagger UI

1. Navigate to `http://localhost:8080/swagger-ui.html`
2. Expand "Document Management" section
3. Click "Try it out" on any endpoint
4. Fill in parameters and click "Execute"
5. View response below

## Troubleshooting

### DMS not available (503 error)
**Cause:** DMS session factory not configured or DMS disabled.

**Solution:** Enable DMS in `application.yml`:
```yaml
hitorro:
  dms:
    enabled: true
```

### Document not found (404 error)
**Cause:** Document ID doesn't exist or was deleted.

**Solution:** Verify the document ID is correct and the document exists.

### Category errors
**Cause:** Invalid category domain/value or category already exists.

**Solution:** Check category values are not empty and domain/value combination is valid.

## Limitations

### Current Implementation
- **Content Upload/Download**: Not included (will be added in future)
- **Renditions**: API structure present but implementation pending
- **Full-text Search**: Use Hitorro's full-text search features separately
- **Permissions**: Security/permissions checking not implemented in examples

### Performance Considerations
- Default query limit: 100 documents
- Large result sets may require pagination (not yet implemented)
- Version history traversal is recursive (may be slow for deep trees)

## Next Steps

1. **Add Content Upload API** - Multipart file upload for document content
2. **Add Rendition Creation** - Generate thumbnails, PDFs, previews
3. **Implement Pagination** - Page through large result sets
4. **Add Security** - Role-based access control
5. **Bulk Operations** - Batch create/update/delete documents

## See Also

- [Hitorro DMS Architecture](DMS_INTEGRATION_STATUS.md)
- [API Testing Guide](API_TESTING_GUIDE.md)
- [Swagger Documentation](http://localhost:8080/swagger-ui.html)
- [H2 Database Console](http://localhost:8080/h2-console)

---

**Need Help?** Check the application logs or use Swagger UI for interactive API exploration.
