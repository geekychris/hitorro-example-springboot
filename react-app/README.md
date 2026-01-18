# Hitorro Test Application

A comprehensive React testing interface for the Hitorro Spring Boot Example application. This application provides interactive UIs for testing various Hitorro features including DMS, filesystem crawling, type system enrichment, and CommandDef execution.

## Features

### 🗂️ Document Management System (DMS)
- **Document CRUD**: Create, read, update, and delete documents
- **Content Management**: Upload and download content as renditions
- **Versioning**: Create new versions and view version history
- **Categories**: Add and remove category tags
- **Containers**: Attach documents to containers (folders, forums, etc.)
- **Advanced Search**: Query documents with flexible criteria

### 📁 Filesystem Crawler
- Import files and directories from the server filesystem into DMS
- Recursive crawling with configurable depth limits
- Real-time progress tracking
- Error reporting and file path listing

### 🔧 JSON Type System (JVS)
- **Enrichment**: Apply JVS2JVSEnrichMapper to expand JSON objects
- **Field Exploration**: View all fields with types and paths
- **Type Browser**: Explore available type definitions
- **Interactive JSON Viewer**: Collapsible JSON tree view with copy support

### 💻 CommandDef Executor
- **Command Discovery**: Automatically lists all @CommandDef methods
- **Dynamic Forms**: Generate parameter forms based on command signatures
- **Type Conversion**: Automatic parameter type handling
- **Result Visualization**: View execution results in formatted JSON

## Prerequisites

- Node.js 18+ and npm
- Hitorro Spring Boot Example running on `http://localhost:8080`

## Installation

1. Navigate to the react-app directory:
```bash
cd hitorro-example-springboot/react-app
```

2. Install dependencies:
```bash
npm install
```

## Running the Application

### Development Mode

Start the development server with hot reload:

```bash
npm run dev
```

The application will be available at `http://localhost:3000`

### Production Build

Build for production:

```bash
npm run build
```

Preview the production build:

```bash
npm run preview
```

## Project Structure

```
react-app/
├── src/
│   ├── pages/           # Page components
│   │   ├── DMSPage.tsx
│   │   ├── CrawlerPage.tsx
│   │   ├── TypeSystemPage.tsx
│   │   └── CommandsPage.tsx
│   ├── services/        # API services
│   │   └── api.ts
│   ├── types/           # TypeScript types
│   │   └── api.ts
│   ├── App.tsx          # Main application
│   ├── App.css          # Global styles
│   └── main.tsx         # Entry point
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

## API Endpoints

The application communicates with the following backend endpoints:

### DMS API (`/api/dms`)
- `POST /documents` - Create document
- `GET /documents/{id}` - Get document
- `PUT /documents/{id}` - Update document
- `DELETE /documents/{id}` - Delete document
- `POST /documents/query` - Query documents
- `GET /documents/{id}/content/list` - List content
- `POST /documents/{id}/content` - Upload content
- `GET /documents/{id}/content/download` - Download content
- `POST /documents/{id}/version` - Create version
- `GET /documents/{id}/versions` - Get version history

### Crawler API (`/api/dms/crawler`)
- `POST /crawl` - Start filesystem crawl

### Type System API (`/api/jvs`)
- `POST /enrich` - Enrich JSON object
- `GET /types` - List all types
- `GET /types/{name}` - Get type definition
- `POST /field` - Get field value

### CommandDef API (`/api/commands`)
- `GET /list` - List all commands
- `GET /{name}` - Get command details
- `POST /execute` - Execute command

## Technology Stack

- **React 18** - UI framework
- **TypeScript** - Type safety
- **Vite** - Build tool and dev server
- **TanStack Query** - Data fetching and caching
- **Axios** - HTTP client
- **Lucide React** - Icon library
- **React JSON View** - JSON visualization

## Development

### Adding a New Page

1. Create a new component in `src/pages/`
2. Add the page to the tabs array in `App.tsx`
3. Add routing logic in the `renderTabContent` function

### Adding New API Endpoints

1. Define TypeScript types in `src/types/api.ts`
2. Add API functions in `src/services/api.ts`
3. Use with `useQuery` or `useMutation` in components

### Styling

The application uses CSS variables for theming. See `App.css` for available variables:

- `--primary` - Primary color
- `--background` - Background color
- `--surface` - Card/surface color
- `--border` - Border color
- `--text-primary` - Primary text
- `--text-secondary` - Secondary text

## Common Issues

### CORS Errors

If you see CORS errors, ensure the Spring Boot application has proper CORS configuration. The Vite proxy is configured to forward `/api` requests to `http://localhost:8080`.

### API Not Available

Ensure the Hitorro Spring Boot Example is running and accessible at `http://localhost:8080`. Check the console for connection errors.

### Type System Features Not Working

The Type System features require:
- `hitorro-text-core` dependency
- `JVS2JVSEnrichMapper` class available
- Type definitions loaded in the application

## Contributing

To contribute to this test application:

1. Follow the existing code style
2. Add TypeScript types for all data structures
3. Use TanStack Query for data fetching
4. Implement proper error handling
5. Add loading states for async operations

## License

Copyright (c) 2006-2025 Chris Collins. See parent project for license details.
