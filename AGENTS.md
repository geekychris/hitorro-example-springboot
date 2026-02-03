# AGENTS.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Build & Run Commands

### Building
```bash
# Build entire application
mvn clean package

# Compile only (skip tests)
mvn clean compile -DskipTests

# Build React UI only
cd react-app && npm install && npm run build
```

### Running
```bash
# Run Spring Boot application
mvn spring-boot:run

# Run in development mode with React hot reload
cd react-app && npm run dev
# Then access frontend at http://localhost:3000, backend at http://localhost:8080

# Run via Docker (recommended for production)
cd docker_build && ./build-and-start.sh
```

### Testing
```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=ClassName

# Run specific test method
mvn test -Dtest=ClassName#methodName
```

## High-Level Architecture

### Hybrid Framework Integration
This application demonstrates **Hitorro framework integration with Spring Boot**, creating a hybrid system where:
- **Hitorro services** run alongside Spring services via `HitorroAutoConfiguration`
- **Two property systems coexist**: Spring's `application.yml` and Hitorro's legacy property system
- **Entity scanning requires both frameworks**: `@EntityScan` includes both Hitorro (`com.hitorro.basedms`) and Spring entities

### Critical Initialization Order
The application startup sequence is **order-dependent**:

1. **BEFORE Spring context**: `HitorroExampleApplication.main()` calls `configureHitorroSystemProperties()` to set `HT_BIN` and `HT_HOME` system properties
2. **During Spring startup**: `HitorroAutoConfiguration` initializes Hitorro services based on `application.yml` configuration
3. **Service framework init**: `ServiceContext` runs with `dbInit=true` when `hitorro.hitorro-properties.dbinit: true` is set, triggering CSV data import
4. **Hibernate schema**: Tables created/updated based on `spring.jpa.hibernate.ddl-auto: update`

**Why this matters**: If `HT_BIN` or `HT_HOME` are not set before Spring starts, Hitorro's property loading will fail, causing cascading failures in type system, services, and DMS initialization.

### Property System Bridging
Configuration flows from Spring to Hitorro through multiple layers:

```
application.yml
  └→ hitorro.ht-bin → System.setProperty("HT_BIN")
  └→ hitorro.hitorro-properties.* → JVSProperties (Hitorro's legacy system)
  └→ hitorro.services.load → ServiceContext initialization
```

Key properties that bridge both systems:
- `hitorro.ht-bin` / `HT_BIN`: Hitorro installation directory (contains `config/`, `types/`)
- `hitorro.ht-home` / `HT_HOME`: Runtime data directory
- `hitorro.hitorro-properties.dbinit`: Triggers Hitorro's service initialization with database setup

### JSON Type System (JVS) Architecture
The type system is central to document processing:

**Type Resolution**:
- Types defined in `${HT_BIN}/config/jsonconfigs/types/` as JSON files
- `JsonTypeSystem.getMe().getType("typename")` provides global singleton access
- Each `JVS` object wraps a Jackson `JsonNode` + optional `Type` reference
- Type determined by `type` field in JSON document

**Type-Driven Operations**:
- **Indexing**: `LuceneIndexerAction` projects JVS fields → Lucene fields using type metadata
- **Enrichment**: `JVS2JVSEnrichMapper` adds computed fields, NLP annotations based on type
- **Field access**: `JVS.get("path.to.field")` uses type to navigate dotted paths

**Creating JVS instances**:
- From JSON string: `JVS.read(jsonString)` (NOT `new JVS(jsonString)`)
- From JsonNode: `new JVS(jsonNode)`
- From Type: `new JVS(type)` creates empty document with type set

### Controller Patterns

#### REST Controllers
Located in `src/main/java/com/hitorro/example/controller/` (note: some are in `/controllers/` subdirectory - naming inconsistency exists):

- **Use Swagger/OpenAPI annotations** (`@Tag`, `@Operation`, `@Parameter`) for API documentation
- **JVS operations require type context**: Most operations need `JsonTypeSystem.getMe().getType()` access
- **NDJson streaming pattern**: Several controllers support streaming with `application/x-ndjson` content type
  - Accept: `InputStream` with `MediaType.APPLICATION_OCTET_STREAM_VALUE`
  - Return: Stream to `HttpServletResponse.getOutputStream()`
  - Use `JSONIterator` for input, `JsonSink` for output
- **Error handling**: Return `ResponseEntity<Map<String, Object>>` with `status` and `message` keys

Example streaming pattern (from `JVSController`):
```java
@PostMapping(value = "/endpoint/stream", 
             consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
             produces = "application/x-ndjson")
public void processStream(InputStream inputStream, HttpServletResponse response) {
    try (JSONIterator jsonIterator = new JSONIterator(new InputStreamReader(inputStream));
         JsonSink jsonSink = new JsonSink(response.getOutputStream())) {
        jsonIterator.map(processor).sink(jsonSink);
    }
}
```

#### Search Integration Pattern
`SearchController` demonstrates hitorro-index integration:
- **Index initialization**: Must happen in `@PostConstruct` with `IndexConfig.builder().filesystem(path)`
- **Type requirement**: `JVSLuceneSearcher` constructor requires `Type` + `defaultLang`
- **Searcher refresh**: Call `searcher.refresh()` before each search to see new documents
- **Faceting**: Pass `List<String>` of field names, not `Set` (API changed)

### React UI Architecture

Located in `react-app/src/`:

**Tab-based navigation**: `App.tsx` contains:
- `TabId` type union defining all tabs
- `tabs` array with metadata (id, label, description)
- `renderTabContent()` switch statement for routing
- To add new feature: Add to all three places + create page component

**API integration pattern**:
- Types in `src/types/api.ts`
- API functions in `src/services/api.ts` using Axios
- Components use TanStack Query: `useQuery` for GET, `useMutation` for POST/PUT/DELETE
- Base URL handled by Vite proxy in dev, relative paths in production

**Page component structure** (see `SearchPage.tsx` as reference):
- State management with React hooks
- `useQuery` for initial data load with `queryKey` and `queryFn`
- `useMutation` with `onSuccess` callbacks for refetching
- JSON visualization with `ReactJson` component (`@microlink/react-json-view`)
- Lucide React for icons

**Building UI for Spring Boot**:
- `npm run build` creates `react-app/dist/`
- Spring Boot serves from `src/main/resources/static/` (copy build output there)
- Production build required for Docker images

### Document Management System (DMS)

**Entity hierarchy**:
- `SysObject` (base): Core document with `id.did`, `id.domain`, timestamps
- `Document` extends `SysObject`: Adds content, versions, containers
- `Folder` extends `SysObject`: Container for organizing documents
- Custom entities (e.g., `ProductReview`) extend `SysObject` or `Document`

**Content & Renditions**:
- Documents have multiple `Rendition` objects (original, PDF, thumbnail, etc.)
- Content stored via `Store` abstraction (filesystem, S3, etc.)
- Transformations: `TransformerService` converts formats (PDF→text, image→thumbnail)

**Critical: Entity Scanning**
- `@EntityScan` in main application class **must include** `com.hitorro.base.objects` and `com.hitorro.basedms`
- Missing these causes "entity not mapped" errors for `Folder`, `NamedLongEntry`, etc.
- Custom entities go in `com.hitorro.example.entities` and must be added to `@EntityScan`

### Logging Exclusions
**Critical**: All Hitorro dependencies use Log4j 1.x which conflicts with Spring Boot's Logback. Every Hitorro dependency in `pom.xml` **must exclude**:
```xml
<exclusion>
    <groupId>log4j</groupId>
    <artifactId>log4j</artifactId>
</exclusion>
<exclusion>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-reload4j</artifactId>
</exclusion>
<exclusion>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-log4j12</artifactId>
</exclusion>
```

Forgetting these causes cryptic classpath errors and duplicate logging.

### Database Configuration

**H2 file-based persistence**:
- Database at `./data/hitorrodb` (persistent across restarts)
- H2 console at http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/hitorrodb`
- Username: `sa`, Password: `hitorro`

**Schema management**:
- `spring.jpa.hibernate.ddl-auto: update` preserves data across restarts
- **DO NOT use `create`** - it drops all data on each startup
- Hitorro entities use `globally_quoted_identifiers: true` due to reserved words in table names

**Known issue**: Index `name_idx` may error with "already exists" - this is harmless, the index exists from previous run

## Code Patterns & Conventions

### When Adding New Controllers

1. **Placement**: Create in `src/main/java/com/hitorro/example/controller/` (not `/controllers/` - the inconsistency exists but prefer `/controller/`)
2. **Annotations**:
   ```java
   @RestController
   @RequestMapping("/api/feature")
   @Tag(name = "Feature Name", description = "Feature description")
   ```
3. **Swagger docs**: Use `@Operation`, `@Parameter`, `@ApiResponse` on all endpoints
4. **Error responses**: Return `Map<String, Object>` with `status` and `message` keys
5. **JVS operations**: Get type system instance: `JsonTypeSystem.getMe().getType("typename")`

### When Working with Lucene Index

The `hitorro-index` module provides Lucene integration:

1. **Configuration**: Use `IndexConfig.builder().filesystem(path).build()` or `.inMemory()`
2. **Type-aware searching**: `JVSLuceneSearcher` requires `Type` instance and default language
3. **Field resolution**: Queries like `title.mls:search` automatically resolve to `title.mls.text_en_s` based on type
4. **Refresh required**: Call `searcher.refresh()` before searches to see newly indexed documents
5. **API signatures**:
   - `indexWriter.indexDocument(JVS)` for single doc
   - `indexWriter.indexDocuments(List<JVS>)` for batch
   - `searcher.search(query, offset, limit, facetDims)` where facetDims is `List<String>` (not Set)

### When Adding React Pages

1. **Create page**: `react-app/src/pages/MyFeaturePage.tsx`
2. **Update App.tsx**:
   - Add to `TabId` type union
   - Add to `tabs` array with metadata
   - Add case to `renderTabContent()` switch
3. **API integration**:
   - Add types to `src/types/api.ts`
   - Add API functions to `src/services/api.ts`
   - Use `useQuery`/`useMutation` in component
4. **Follow existing patterns**: See `SearchPage.tsx` or `TypeSystemPage.tsx` as reference

### When Modifying application.yml

Key sections:
- **`hitorro.ht-bin`**: Must point to Hitorro installation (contains `config/`, `types/`)
- **`hitorro.services.load`**: List of Hitorro services to initialize (order matters)
- **`hitorro.hitorro-properties`**: Bridges to legacy JVSProperties system
  - **Critical**: Property `dbinit: true` (no hyphen) triggers database initialization
- **`spring.jpa.hibernate.ddl-auto`**: Use `update` to preserve data, NOT `create`

## Common Pitfalls

1. **HT_BIN not set early enough**: Must be set in `main()` before `SpringApplication.run()`
2. **Using wrong JVS constructor**: Use `JVS.read(string)` not `new JVS(string)`
3. **Missing entity scan packages**: Causes "Unknown entity" errors for Hitorro entities
4. **Forgetting log exclusions**: Causes duplicate logging and classpath issues
5. **Not refreshing searcher**: Newly indexed documents won't appear in search results
6. **Using `create` for ddl-auto**: Drops all data on every restart
7. **Wrong controller directory**: Inconsistency exists between `/controller/` and `/controllers/`
8. **Type system not initialized**: Trying to use `JsonTypeSystem` before Hitorro services load
9. **React build not copied**: Spring Boot can't serve UI if `dist/` not in `src/main/resources/static/`
10. **Wrong API method signature**: `searcher.search()` takes offset/limit/facetDims, not just limit

## Directory Structure Context

```
hitorro-example-springboot/
├── src/main/java/com/hitorro/example/
│   ├── HitorroExampleApplication.java    # Entry point - sets HT_BIN/HT_HOME
│   ├── controller/                        # REST controllers (preferred location)
│   │   ├── SearchController.java          # Lucene search integration
│   │   ├── JVSController.java             # Type system operations
│   │   └── ...
│   ├── controllers/                       # Some controllers here (inconsistency)
│   ├── commands/                          # @CommandDef implementations
│   ├── entities/                          # Custom DMS entities
│   ├── services/                          # Application services
│   └── logging/                           # Structured logging
├── src/main/resources/
│   ├── application.yml                    # Spring + Hitorro configuration
│   ├── log-configs/                       # Structured logging schemas
│   └── static/                            # Served by Spring Boot (React build goes here)
├── react-app/                             # React UI source
│   ├── src/
│   │   ├── pages/                         # Page components
│   │   ├── services/api.ts                # Axios API functions
│   │   ├── types/api.ts                   # TypeScript types
│   │   └── App.tsx                        # Main app with tab routing
│   ├── package.json
│   └── vite.config.ts
├── docker_build/                          # Docker build scripts
│   ├── build-and-start.sh                 # One-command startup
│   └── hitorro.sh                         # Master control script
├── data/                                  # H2 database (gitignored)
├── pom.xml                                # Maven configuration
└── SEARCH_EXAMPLE.md                      # Lucene integration guide
```

## Technology Stack

- **Java 21** (source/target)
- **Spring Boot 3.2.2** with Spring Security
- **Hitorro Framework 3.0.0** (proprietary DMS framework)
- **Apache Lucene 9.11.1** via `hitorro-index`
- **H2 Database** (file-based, persistent)
- **React 18 + TypeScript** with Vite
- **TanStack Query** for data fetching
- **Material-UI** concepts (custom CSS implementation)

## Key External Documentation

- **START_HERE.md**: Quickstart guide for Docker deployment
- **SEARCH_EXAMPLE.md**: Lucene integration examples and API reference
- **STRUCTURED_LOGGING_DEMO.md**: Kafka-based structured logging guide
- **react-app/README.md**: Frontend development guide
- **docker_build/README.md**: Docker build scripts documentation
- Swagger UI: http://localhost:8080/swagger-ui.html (when running)
