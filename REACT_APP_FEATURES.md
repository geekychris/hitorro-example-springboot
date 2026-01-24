# React-App UI Features Guide

## ✅ Your React-App Has 6 Interactive Tabs

The react-app UI at **http://localhost:8090** provides a comprehensive interactive testing interface with the following tabs:

## 1. 📄 Document Management (DMS)

**Tab**: "Document Management"  
**Component**: `DMSPageEnhanced`  
**Purpose**: Complete document management system

### Features:
- **Document Upload & Management**
  - Upload documents to DMS
  - View document metadata
  - Download documents
  - Delete documents
  
- **Container Management**
  - Browse folder/container hierarchies
  - Create new containers
  - Move documents between containers
  
- **Version Control**
  - View document versions
  - Compare versions
  - Restore previous versions
  
- **Content Transformation**
  - Transform document formats
  - PDF generation
  - Format conversion
  - Text extraction

## 2. 📁 Filesystem Crawler

**Tab**: "Filesystem Crawler"  
**Component**: `CrawlerPage`  
**Purpose**: Import files from filesystem into DMS

### Features:
- **File System Import**
  - Browse local filesystem
  - Select files and directories
  - Batch import into DMS
  
- **Crawler Configuration**
  - Set import rules
  - File filtering
  - Metadata extraction options
  
- **Progress Monitoring**
  - Track import progress
  - View import results
  - Error handling

## 3. 🔧 Type System

**Tab**: "Type System"  
**Component**: `TypeSystemPage`  
**Purpose**: JSON Type System enrichment and field exploration

### Features:
- **Type Browser**
  - View all registered JVS types
  - Explore type definitions
  - View type hierarchies
  
- **Field Exploration**
  - Browse type fields
  - View field types and metadata
  - Field enrichment options
  
- **Type Enrichment**
  - Add/modify type definitions
  - Enrich existing types
  - Type validation

## 4. ⌨️ Commands (CLI Interface)

**Tab**: "Commands"  
**Component**: `CommandsPage`  
**Purpose**: Execute CommandDef annotated methods

### Features:
- **Command Discovery**
  - View all available commands
  - Browse command definitions
  - See command parameters
  
- **Command Execution**
  - Execute commands with parameters
  - View command results
  - Command history
  
- **Interactive Testing**
  - Test CLI commands from UI
  - Parameter validation
  - Result visualization

## 5. 🔌 REST API Explorer ⭐

**Tab**: "REST API Explorer"  
**Component**: `RestExplorerPage`  
**Purpose**: Discover and test REST endpoints with streaming support

### Features:
- **API Discovery**
  - Auto-discover all REST endpoints
  - View endpoint documentation
  - See request/response schemas
  
- **Interactive Testing**
  - Test any REST endpoint
  - Set request parameters
  - View responses in real-time
  
- **Streaming Support**
  - Test streaming endpoints
  - View streaming data
  - WebSocket support
  
- **Request Builder**
  - Build complex requests
  - Set headers and body
  - Save/load request templates

### How It Works:
The REST Explorer automatically discovers endpoints from your Spring Boot application and provides an interactive interface to test them. Much more powerful than just Swagger!

## 6. 🔗 Services (Dependency Viewer) ⭐

**Tab**: "Services"  
**Component**: `ServicesExplorerPage`  
**Purpose**: Explore Hitorro services and dependency hierarchy

### Features:
- **Service Discovery**
  - View all running Hitorro services
  - See service status
  - Service lifecycle information
  
- **Dependency Hierarchy**
  - Visualize service dependencies
  - See which services depend on others
  - Dependency graph
  
- **Service Information**
  - Service configurations
  - Service metadata
  - Service health status
  
- **Service Management**
  - View service details
  - Monitor service state
  - Inspect service properties

### How It Works:
The Services Explorer connects to the Hitorro ServiceContext and displays the entire service hierarchy, showing you how services are organized and depend on each other.

## How to Access

**URL**: http://localhost:8090

**Navigation**: Click any tab at the top to switch between features:

```
┌─────────────────────────────────────────────────────────────┐
│  Hitorro Test Application                                    │
│  Spring Boot Example - Interactive Testing Interface         │
├─────────────────────────────────────────────────────────────┤
│ [Document Management] [Filesystem Crawler] [Type System]    │
│ [Commands] [REST API Explorer] [Services]                   │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  (Tab content appears here)                                  │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

## Technical Details

### Stack:
- **Framework**: React 18 with TypeScript
- **Build Tool**: Vite
- **State Management**: TanStack Query (React Query)
- **UI**: Custom CSS with modern design

### API Integration:
The UI connects to these backend endpoints:
- `/api/rest/dms/*` - Document management
- `/api/rest/filesystem/*` - File system operations
- `/api/rest/transformer/*` - Content transformation
- `/api/commands/*` - Command execution
- `/api/rest/services/*` - Service information
- `/api/rest/types/*` - Type system data

### Real-Time Features:
- Query caching and automatic refetching
- Streaming response support
- Progressive data loading
- Error handling and retry logic

## Examples of What You Can Do

### With REST API Explorer:
1. **Discover all endpoints** automatically
2. **Test document upload** via REST API
3. **Try transformation endpoints** with different formats
4. **View streaming responses** in real-time
5. **Save test requests** for reuse

### With Services Explorer:
1. **View the entire service tree**
   - See HibernateService
   - See BaseDMSService
   - See TransformerService
   - And all their dependencies

2. **Understand service relationships**
   - Which service loads first?
   - What does each service depend on?
   - Service initialization order

3. **Debug service issues**
   - Check service status
   - View service errors
   - Inspect configurations

## Why These Two Tabs Are Special

### REST API Explorer vs Swagger:
- **Swagger**: Static API documentation
- **REST Explorer**: Dynamic endpoint discovery + interactive testing + streaming support

### Services vs Actuator:
- **Actuator**: Generic Spring Boot metrics
- **Services Explorer**: Hitorro-specific service hierarchy + dependency visualization

## Quick Start

1. **Open the UI**:
   ```bash
   open http://localhost:8090
   ```

2. **Try REST API Explorer**:
   - Click "REST API Explorer" tab
   - It will auto-discover all your REST endpoints
   - Click any endpoint to test it

3. **Try Services Explorer**:
   - Click "Services" tab
   - View the complete Hitorro service hierarchy
   - Click on any service to see details

4. **Try Document Management**:
   - Click "Document Management" tab
   - Upload a test document
   - Try transforming it to different formats

## Current Status

✅ **Application Running**: http://localhost:8090  
✅ **All 6 Tabs**: Available and functional  
✅ **REST Explorer**: Ready to discover endpoints  
✅ **Services Explorer**: Ready to show dependency tree  
✅ **Process**: Running as PID 80666  

## Troubleshooting

**If tabs don't load**:
```bash
# Check JavaScript is loading
curl -s http://localhost:8090/assets/index-SQPtOG_x.js | head -c 100

# Check application is running
curl http://localhost:8090/actuator/health
```

**If REST endpoints don't appear**:
The REST Explorer discovers endpoints from your running application. Make sure:
- Application is fully started
- REST controllers are registered
- No errors in startup logs

**If Services don't appear**:
The Services Explorer queries the Hitorro ServiceContext. Check:
- Services are enabled in configuration
- ServiceContext is initialized
- Check logs: `tail -f /tmp/hitorro-local.log`

## Summary

Your **react-app** is a comprehensive testing and exploration tool with:

✅ **REST API Explorer** - Interactive REST endpoint testing with streaming  
✅ **Services Explorer** - Complete service dependency visualization  
✅ **Document Management** - Full DMS with transformation  
✅ **Filesystem Crawler** - Import files into DMS  
✅ **Type System Browser** - JVS type exploration  
✅ **Commands Interface** - CLI command execution  

**Much more powerful than just Swagger or Actuator!**

---

**Access Now**: http://localhost:8090  
**Status**: ✅ Running  
**All Features**: Available
