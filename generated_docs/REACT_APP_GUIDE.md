# React Test Application - Setup Guide

This guide explains how to set up and use the React test application for the Hitorro Spring Boot Example.

## Overview

The React test application provides a modern, interactive web interface for testing Hitorro features:

- **Document Management System**: Full CRUD operations, versioning, content upload/download
- **Filesystem Crawler**: Import files and directories into DMS
- **Type System**: JVS enrichment and type exploration
- **CommandDef Executor**: Execute @CommandDef methods with dynamic forms

## Quick Start

### 1. Start the Spring Boot Application

```bash
cd hitorro-example-springboot
./run.sh
```

The backend should be running on `http://localhost:8080`

### 2. Install and Run the React App

```bash
cd react-app
npm install
npm run dev
```

The React app will be available at `http://localhost:3000`

## Features by Tab

### Document Management Tab

**Create Documents**
1. Click "New Document" button
2. Fill in title, note, and creator fields
3. Click "Create"

**Upload Content**
1. Select a document from the list
2. Click "Upload" in the details panel
3. Choose a file from your local system
4. The content is stored in the DMS store

**Versioning**
1. Select a document
2. Click "New Version"
3. Optionally add a version note
4. View version history at the bottom of the details panel

**Search Documents**
1. Click "Search" button
2. Enter search criteria (title, creator, date range)
3. Set max results
4. Click "Search"

### Crawler Tab

**Import Files from Filesystem**
1. Enter an absolute path on the server (e.g., `/Users/chris/hitorro/data`)
2. Configure options:
   - **Recursive**: Check to crawl subdirectories
   - **Max Depth**: Set crawl depth (-1 for unlimited)
   - **Store Name**: Leave empty for default store
3. Click "Start Crawl"
4. Monitor progress and view results

**Note**: The path must exist on the server filesystem, not your local machine.

### Type System Tab

**Enrich JSON Objects**
1. Enter JSON in the left textarea
2. Click "Enrich"
3. View the enriched result on the right
4. Examine enrichment details including:
   - Type name detected
   - Fields added/modified
   - Field types and paths

**Explore Type Definitions**
1. Scroll down to "Type Definitions" section
2. Browse available types in the left panel
3. Click a type to view its definition
4. See base types, extended types, and field information

### Commands Tab

**Execute CommandDef Methods**
1. Browse available commands in the left panel
2. Click a command to select it
3. Fill in required parameters in the form
4. Click "Execute"
5. View execution results including:
   - Success/failure status
   - Return value (formatted JSON for objects)
   - Execution time
   - Error messages if any

## Backend Controllers

The React app communicates with these Spring Boot controllers:

### JVSController (`/api/jvs`)
**NEW** - Provides type system endpoints:
- `POST /enrich` - Enriches JSON using JVS2JVSEnrichMapper
- `GET /types` - Lists all available types
- `GET /types/{name}` - Gets type definition details
- `POST /field` - Gets specific field value from JSON

### CommandDefController (`/api/commands`)
**NEW** - Provides CommandDef execution:
- `GET /list` - Lists all registered commands
- `GET /{name}` - Gets command details
- `POST /execute` - Executes a command with parameters

### DocumentManagementController (`/api/dms`)
**EXISTING** - Full DMS operations:
- Document CRUD
- Content upload/download
- Versioning
- Categories and containers

### DMSCrawlerController (`/api/dms/crawler`)
**EXISTING** - Filesystem crawling:
- `POST /crawl` - Crawls and imports directories

## Configuration

### Backend (application.yml)

Ensure these settings are configured:

```yaml
server:
  port: 8080

hitorro:
  enabled: true
  ht-bin: /Users/chris/hitorro
  ht-home: /Users/chris/hthome
  
  dms:
    enabled: true
    
  jvs:
    enabled: true
```

### Frontend (vite.config.ts)

API proxy is configured to forward to backend:

```typescript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    }
  }
}
```

## Troubleshooting

### React App Won't Start

**Error**: `Cannot find module`
- **Solution**: Run `npm install` in the `react-app` directory

**Error**: Port 3000 already in use
- **Solution**: Either stop the other service or change port in `vite.config.ts`

### Backend Connection Issues

**Error**: Network errors in browser console
- **Solution**: Ensure Spring Boot app is running on port 8080
- **Check**: Visit `http://localhost:8080/swagger-ui.html` to verify backend is up

### Type System Features Not Working

**Issue**: Empty type list or enrichment fails
- **Cause**: Type definitions may not be loaded
- **Solution**: Check `HT_BIN` is set correctly and contains `types/` directory
- **Check logs**: Look for type system initialization messages in Spring Boot logs

### CommandDef List Empty

**Issue**: No commands appear in the Commands tab
- **Cause**: CommandRegistry may not be initialized
- **Solution**: Ensure service framework is enabled in `application.yml`
- **Check**: Look for "CommandDefScanner" logs at startup

### File Upload/Download Issues

**Issue**: Upload succeeds but download fails
- **Cause**: Store configuration may be incorrect
- **Solution**: Verify store paths exist and are writable
- **Check**: Review store configuration in application.yml or database

## Development

### Adding New Features

1. **Add Backend Endpoint**
   - Create controller method in appropriate controller
   - Add OpenAPI annotations for documentation
   - Test with Swagger UI

2. **Add Frontend Support**
   - Add types to `src/types/api.ts`
   - Add API function to `src/services/api.ts`
   - Use in components with `useQuery` or `useMutation`

3. **Add New Tab**
   - Create page component in `src/pages/`
   - Add to tabs array in `App.tsx`
   - Update routing in `renderTabContent`

### Code Style

**React Components**
- Use functional components with hooks
- Destructure props
- Add TypeScript types
- Handle loading and error states

**API Calls**
- Use TanStack Query for data fetching
- Invalidate queries after mutations
- Show loading spinners
- Display user-friendly error messages

## Testing Workflow

### Complete Test Scenario

1. **Create a Document**
   - Go to DMS tab
   - Create document "Test Doc 1"
   - Verify it appears in list

2. **Upload Content**
   - Select the document
   - Upload a small test file
   - Verify content count increases

3. **Create Version**
   - Click "New Version"
   - Add note "Version 2"
   - Check version history

4. **Import Files via Crawler**
   - Go to Crawler tab
   - Crawl a directory with a few files
   - Return to DMS tab
   - Search for imported documents

5. **Test Type System**
   - Go to Type System tab
   - Enter a JSON object
   - Enrich it
   - Examine added fields

6. **Execute Commands**
   - Go to Commands tab
   - Find a simple command (e.g., status check)
   - Execute with parameters
   - View results

## Next Steps

- Explore the Swagger UI at `http://localhost:8080/swagger-ui.html`
- Review API documentation for each controller
- Test different document types and content formats
- Experiment with complex type definitions
- Try executing various CommandDef methods

## Support

For issues or questions:
1. Check the Spring Boot logs for backend errors
2. Check browser console for frontend errors
3. Review this guide and the main README
4. Verify all prerequisites are met
