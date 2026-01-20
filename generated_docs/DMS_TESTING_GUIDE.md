# DMS API Testing Guide

## Overview

This guide covers testing the Document Management System (DMS) API using both automated JUnit tests and manual IntelliJ HTTP tests.

## Files

### 1. JUnit Tests
**File:** `src/test/java/com/hitorro/example/controllers/DocumentManagementControllerTest.java`

**What's Tested:**
- Document CRUD operations
- Query and search functionality
- Category management
- Version management
- Container operations
- Content listing
- DTO validations
- Complete workflow integration test

### 2. IntelliJ HTTP Tests
**File:** `dms-api-tests.http`

**What's Included:**
- 33 HTTP requests covering all API endpoints
- Organized by feature area
- Automatic response validation with JavaScript tests
- Variable management for document IDs
- Error handling test scenarios
- Performance tests
- Cleanup operations

## Running JUnit Tests

### Using Maven

```bash
cd hitorro-example-springboot

# Run all tests
mvn test

# Run only DMS controller tests
mvn test -Dtest=DocumentManagementControllerTest

# Run specific test method
mvn test -Dtest=DocumentManagementControllerTest#testCreateDocument_Validation
```

### Using IntelliJ IDEA

1. Open `DocumentManagementControllerTest.java`
2. Right-click on the class name
3. Select "Run 'DocumentManagementControllerTest'"

Or run individual tests:
1. Click the green play button next to any `@Test` method
2. Select "Run" or "Debug"

### Test Output

Tests will either:
- **Skip** if DMS is not configured (graceful degradation)
- **Pass** if operations succeed
- **Fail** if there are unexpected errors

Example output:
```
Warning: DMSSessionFactory not available, tests will be skipped
Skipping test - DMS not available
Skipping integration test - DMS not available
```

## Running IntelliJ HTTP Tests

### Prerequisites

1. **Start the application:**
   ```bash
   cd hitorro-example-springboot
   ./mvnw spring-boot:run
   ```

2. **Verify DMS is enabled** in `application.yml`:
   ```yaml
   hitorro:
     dms:
       enabled: true
   ```

### Using the HTTP Client

#### Method 1: IntelliJ IDEA HTTP Client

1. Open `dms-api-tests.http` in IntelliJ
2. Click the green play button (▶) next to any request
3. View response in the "Run" panel below
4. Check test results in the response tab

**Features:**
- ✅ Syntax highlighting
- ✅ Auto-completion
- ✅ Variable substitution
- ✅ JavaScript test assertions
- ✅ Response history
- ✅ Environment switching

#### Method 2: Command Line with httpYac

Install httpYac:
```bash
npm install -g httpyac
```

Run tests:
```bash
cd hitorro-example-springboot

# Run all requests
httpyac dms-api-tests.http

# Run specific request
httpyac dms-api-tests.http --filter "Create a new document"
```

### Test Execution Order

#### Recommended Flow:

1. **Create Document** (Request #1)
   - Creates a test document
   - Saves document ID to variable `{{documentId}}`

2. **Get Document** (Request #2)
   - Retrieves the created document
   - Validates document properties

3. **Update Document** (Request #3)
   - Updates document title and note

4. **Category Operations** (Requests #5-6)
   - Add and remove categories

5. **Version Operations** (Requests #7-8)
   - Create new version
   - View version history

6. **Query Operations** (Requests #10-14)
   - Test various search criteria

7. **Cleanup** (Delete requests at end)
   - Remove test documents

### Variables

The HTTP file uses these variables:

- `{{baseUrl}}` - API base URL (http://localhost:8080/api/dms)
- `{{contentType}}` - application/json
- `{{documentId}}` - Created by first request, used throughout
- `{{versionId}}` - Created when making versions
- `{{testDocId}}` - Created for comprehensive tests
- `{{containerId}}` - Must be set manually (default: 1)

### Response Assertions

Each request includes JavaScript tests that validate:

```javascript
> {%
    client.test("Request executed successfully", function() {
        client.assert(response.status === 201, "Response status is not 201");
    });
    client.test("Document has correct title", function() {
        var data = response.body;
        client.assert(data.title === "Expected Title", "Title mismatch");
    });
%}
```

## Test Scenarios

### Basic CRUD

```http
### Create
POST http://localhost:8080/api/dms/documents
Content-Type: application/json

{"title": "Test Doc", "creator": "test"}

### Read
GET http://localhost:8080/api/dms/documents/123

### Update
PUT http://localhost:8080/api/dms/documents/123
Content-Type: application/json

{"title": "Updated Title"}

### Delete
DELETE http://localhost:8080/api/dms/documents/123
```

### Complex Query

```http
POST http://localhost:8080/api/dms/documents/query
Content-Type: application/json

{
  "title": "report",
  "creator": "john.doe",
  "createdAfter": "2026-01-01T00:00:00Z",
  "orderBy": "modifiedDate",
  "descending": true,
  "maxResults": 50
}
```

### Version Management

```http
### Create version
POST http://localhost:8080/api/dms/documents/123/version
Content-Type: application/json

{"note": "Version 1.1"}

### Get history
GET http://localhost:8080/api/dms/documents/123/versions
```

## Troubleshooting

### Tests Skip with "DMS not available"

**Cause:** DMS session factory not initialized

**Solutions:**
1. Ensure `hitorro.dms.enabled=true` in application.yml
2. Check that database is accessible (H2 console: http://localhost:8080/h2-console)
3. Verify DMS initialization in application logs

### HTTP Requests Return 503

**Cause:** DMS not configured or not started

**Solutions:**
1. Start the application: `./mvnw spring-boot:run`
2. Check application logs for DMS initialization
3. Verify `hitorro.dms.enabled=true`

### Variable {{documentId}} Not Set

**Cause:** First request (Create Document) failed

**Solutions:**
1. Run the "Create a new document" request first
2. Check response has `id` field
3. Manually set: add `@documentId = 123` to top of file

### 404 Not Found Errors

**Cause:** Document/Container IDs don't exist

**Solutions:**
1. Run create operations first
2. Update IDs in requests to match created resources
3. Check database: http://localhost:8080/h2-console

### Database Connection Errors

**Cause:** H2 database file locked or corrupted

**Solutions:**
1. Stop all running instances
2. Delete `./data/hitorrodb.*` files
3. Restart application (database will be recreated)

## Best Practices

### For JUnit Tests

1. **Test Isolation**: Each test should be independent
2. **Cleanup**: Use `@AfterEach` to clean up resources
3. **Conditional Tests**: Skip tests gracefully when DMS unavailable
4. **Assertions**: Use meaningful assertion messages
5. **Test Data**: Use unique identifiers to avoid conflicts

### For HTTP Tests

1. **Order Matters**: Run create operations before read/update/delete
2. **Save Variables**: Use response handlers to save IDs for later use
3. **Validate Responses**: Always include test assertions
4. **Clean Up**: Delete test data after running scenarios
5. **Document IDs**: Use descriptive names in comments

## Test Coverage

### Covered Features ✅

- Document CRUD (create, read, update, delete)
- Category management (add, remove, search)
- Version creation and history
- Container attachment/detachment
- Query with multiple filters
- Search by category
- DTO validation
- Error handling (404, 503)
- Complete workflows

### Not Yet Covered ⚠️

- Content upload/download (not implemented in controller)
- Rendition creation (structure present, implementation pending)
- Bulk operations
- Permission/security checks
- Pagination
- Concurrent modifications

## Performance Testing

### Load Test Example

Run the batch creation tests (#30-33) multiple times:

```bash
for i in {1..10}; do
  httpyac dms-api-tests.http --filter "Create Document"
done
```

Then query all:
```http
POST http://localhost:8080/api/dms/documents/query
Content-Type: application/json

{"creator": "batch-test", "maxResults": 1000}
```

### Monitoring

1. **Response Times**: Check HTTP client timing info
2. **Database Size**: Monitor `./data/hitorrodb.mv.db` file size
3. **Memory**: Use JConsole or VisualVM
4. **Logs**: Watch application logs for slow queries

## Integration with CI/CD

### Maven Surefire

Tests run automatically during Maven build:

```bash
mvn clean test
```

### JUnit 5 Features Used

- `@TestMethodOrder` - Ensures tests run in order
- `@BeforeAll` / `@AfterEach` - Setup and cleanup
- `@DisplayName` - Readable test names
- `@Order` - Explicit test ordering
- Conditional execution - Skips when DMS unavailable

## Example Test Sessions

### Session 1: Basic CRUD

```http
1. Create document → Save {{documentId}}
2. Get document → Verify properties
3. Update document → Verify changes
4. Delete document → Confirm deletion
```

### Session 2: Categorization

```http
1. Create document → Save {{documentId}}
2. Add category "type=report"
3. Add category "status=draft"
4. Search by category "type=report" → Should find document
5. Remove category "status=draft"
6. Delete document
```

### Session 3: Versioning

```http
1. Create document → Save {{documentId}}
2. Create version 1.1 → Save {{versionId}}
3. Create version 1.2
4. Get version history → Should show 3 versions
5. Delete all versions
```

## Additional Resources

- [DMS API Guide](DMS_API_GUIDE.md) - Complete API reference
- [Swagger UI](http://localhost:8080/swagger-ui.html) - Interactive API docs
- [H2 Console](http://localhost:8080/h2-console) - Database viewer

## Quick Reference

### Run All Tests
```bash
# JUnit
mvn test -Dtest=DocumentManagementControllerTest

# HTTP (in IntelliJ)
# Click "Run All Requests" button in HTTP file
```

### Common Assertions

```javascript
// Status code
client.assert(response.status === 200, "Expected 200");

// Response body
var data = response.body;
client.assert(data.id !== null, "Missing ID");
client.assert(Array.isArray(data), "Not an array");

// Save variable
client.global.set("documentId", data.id);
```

### Quick Test

```bash
# Start app
./mvnw spring-boot:run

# In another terminal
curl -X POST http://localhost:8080/api/dms/documents \
  -H "Content-Type: application/json" \
  -d '{"title": "Quick Test", "creator": "curl"}'
```

---

**Happy Testing!** 🧪

For issues or questions, check the application logs or consult the [DMS API Guide](DMS_API_GUIDE.md).
