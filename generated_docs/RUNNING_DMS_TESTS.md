# Running DMS Tests - Quick Guide

## TL;DR - Run the Integration Tests

```bash
# Run integration tests (requires running application)
mvn test -Dtest=DocumentManagementControllerIntegrationTest
```

## Test Files Overview

### ✅ DocumentManagementControllerIntegrationTest.java (USE THIS ONE!)

**Type:** Spring Boot Integration Tests

**What it does:**
- Runs with full Spring Boot context
- Uses actual DMS session factory
- Tests against H2 database
- Complete end-to-end testing

**Why use it:**
- Tests actual functionality
- Catches real bugs
- Validates database operations
- Production-like environment

**How to run:**

#### In IntelliJ IDEA:
1. Open `DocumentManagementControllerIntegrationTest.java`
2. Click green play button next to class name
3. Select "Run DocumentManagementControllerIntegrationTest"
4. Watch tests execute with ✓ checkmarks

#### With Maven:
```bash
mvn test -Dtest=DocumentManagementControllerIntegrationTest
```

#### Run specific test:
```bash
mvn test -Dtest=DocumentManagementControllerIntegrationTest#testCreateDocument
```

### DocumentManagementControllerTest.java

**Type:** Unit Tests (No Spring Context)

**What it does:**
- Tests DTOs and data structures
- Validates request/response objects
- Checks error handling

**Why it exists:**
- Fast execution (no app startup)
- Good for CI/CD
- Validates API contracts

**Limitation:** 
- Cannot test actual DMS operations
- Tests will skip with "DMS not available" message
- Useful for DTO validation only

## Prerequisites

### For Integration Tests

1. **Enable DMS** in `application.yml`:
```yaml
hitorro:
  dms:
    enabled: true
```

2. **Database configured:**
```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/hitorrodb
    driver-class-name: org.h2.Driver
```

3. **Required paths exist:**
```yaml
hitorro:
  ht-bin: /Users/chris/hitorro
  ht-home: /Users/chris/hthome
```

## Running Tests

### Method 1: IntelliJ IDEA (Easiest)

1. **Open the test file:**
   - `DocumentManagementControllerIntegrationTest.java`

2. **Run all tests:**
   - Right-click on class name
   - Select "Run DocumentManagementControllerIntegrationTest"

3. **Run single test:**
   - Click green play button next to test method
   - Example: `testCreateDocument()`

4. **Debug tests:**
   - Right-click and select "Debug" instead
   - Set breakpoints as needed

### Method 2: Maven Command Line

```bash
cd hitorro-example-springboot

# Run all integration tests
mvn test -Dtest=DocumentManagementControllerIntegrationTest

# Run with output
mvn test -Dtest=DocumentManagementControllerIntegrationTest -X

# Run specific test
mvn test -Dtest=DocumentManagementControllerIntegrationTest#testCreateDocument
```

### Method 3: Run All Tests

```bash
# Run all tests (unit + integration)
mvn test

# Skip tests during build
mvn install -DskipTests
```

## Expected Output

### Successful Run:

```
=== Starting Complete Workflow Test ===

1. Created document: 123
2. Added category
3. Updated title
4. Created version
5. Queried documents: found 2
6. Retrieved version history: 2 versions
7. Deleted document

=== Workflow Test Complete ===

✓ Created document ID: 123
✓ Retrieved document: Integration Test Document
✓ Updated document title
✓ Added category: priority=high
✓ Query found 1 document(s)
✓ Category search found 1 document(s)
✓ Created version: 1.1
✓ Version history has 2 version(s)
✓ Content list retrieved (empty as expected)
✓ Removed category: priority=high
✓ Correctly returns 404 for non-existent document
✓ Handles invalid query parameters
✓ Deleted test document ID: 123

Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
```

### If DMS Not Available:

```
WARNING: DocumentManagementController not available - tests will be skipped
Skipping - controller not available
Skipping - prerequisites not met

Tests run: 13, Failures: 0, Errors: 0, Skipped: 13
```

## Troubleshooting

### Tests are Skipping

**Problem:** All tests show "Skipping - controller not available"

**Solutions:**

1. **Check DMS is enabled:**
```yaml
hitorro:
  dms:
    enabled: true  # Must be true!
```

2. **Verify database exists:**
```bash
ls -la ./data/hitorrodb.mv.db
```

3. **Check application logs:**
```bash
mvn spring-boot:run
# Look for "DMS initialized" or similar messages
```

4. **Verify paths exist:**
```bash
ls -la ~/hitorro
ls -la ~/hthome
```

### Tests Fail with Database Errors

**Problem:** "Could not open connection to database"

**Solutions:**

1. **Delete and recreate database:**
```bash
rm -rf ./data/hitorrodb.*
# Restart app - database will be recreated
```

2. **Check H2 console:**
- Open: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/hitorrodb`
- Username: `sa`
- Password: (from application.yml)

### Tests Fail with "Table not found"

**Problem:** Database schema not created

**Solution:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # or create
```

## Test Execution Order

Tests run in this order (see `@Order` annotations):

1. **Setup** - Check controller available
2. **Create** - Create test document
3. **Read** - Get document by ID
4. **Update** - Modify document
5. **Category** - Add/search/remove categories
6. **Query** - Test search functionality
7. **Version** - Create versions and view history
8. **Content** - List content (empty)
9. **Error Handling** - Test 404s and invalid input
10. **Cleanup** - Delete test document
11. **Workflow** - Complete end-to-end test

## What Gets Tested

### ✅ Covered

- ✓ Document create, read, update, delete
- ✓ Category add, remove, search
- ✓ Query by title, creator, date range
- ✓ Version creation and history
- ✓ Content listing
- ✓ Error handling (404, invalid params)
- ✓ Complete workflows
- ✓ Database persistence
- ✓ Transaction management

### ⚠️ Not Yet Tested

- Content upload/download (not implemented)
- Rendition creation (structure only)
- Container operations (needs container setup)
- Concurrent modifications
- Performance/load testing
- Security/permissions

## Quick Commands

```bash
# Run integration tests
mvn test -Dtest=DocumentManagementControllerIntegrationTest

# Run with Spring profile
mvn test -Dtest=DocumentManagementControllerIntegrationTest -Dspring.profiles.active=test

# Run and show all output
mvn test -Dtest=DocumentManagementControllerIntegrationTest -X | tee test-output.log

# Run in IntelliJ - just click the green play button!
```

## IntelliJ Run Configuration

Create a run configuration for easy testing:

1. **Run → Edit Configurations**
2. **Add New → JUnit**
3. **Settings:**
   - Name: "DMS Integration Tests"
   - Test kind: Class
   - Class: `com.hitorro.example.controllers.DocumentManagementControllerIntegrationTest`
   - Module: hitorro-example-springboot
   - VM options: `-Dspring.profiles.active=test`

4. **Click OK**

Now you can run tests from the toolbar!

## Best Practices

1. **Always run integration tests** when changing DMS controller
2. **Check logs** if tests skip or fail
3. **Clean database** periodically (delete ./data/hitorrodb.*)
4. **Use descriptive test names** when adding new tests
5. **Add cleanup** in @AfterEach or high-order @Test methods

## Related Files

- `DocumentManagementController.java` - The controller being tested
- `dms-api-tests.http` - Manual HTTP tests for IntelliJ
- `DMS_API_GUIDE.md` - Complete API documentation
- `DMS_TESTING_GUIDE.md` - Detailed testing guide

## Need Help?

1. Check application logs: `tail -f logs/spring.log`
2. Verify H2 console: http://localhost:8080/h2-console
3. Check Swagger UI: http://localhost:8080/swagger-ui.html
4. Review test output for specific error messages

---

**Remember:** Use `DocumentManagementControllerIntegrationTest` for real testing! 🧪
