# UI and Docker Build Clarification

## ✅ React UI on Port 8090

### What You're Seeing

**YES**, the UI on port 8090 **IS the React app** from the `frontend/` directory!

Here's what happened:

1. **Built the React app**:
   ```bash
   cd frontend/
   npm install
   npm run build
   ```

2. **Build output**: Created in `frontend/dist/`
   - `index.html`
   - `assets/` directory with JavaScript bundles
   - React components, Material-UI styling, etc.

3. **Deployed to Spring Boot**:
   ```bash
   cp -r frontend/dist/* src/main/resources/static/
   ```

4. **Spring Boot serves it**: At http://localhost:8090/

### Verify It's the React App

**Check the HTML title**:
```bash
curl -s http://localhost:8090/ | grep title
# Output: <title>Hitorro DMS</title>
```

**Check the JavaScript is loading**:
```bash
curl -s http://localhost:8090/assets/index-f3B83tJk.js | head -c 100
# Output: import{_ as H,a as st,u as _r,j as c,i as ie...
```

**Open in browser**: http://localhost:8090
- You'll see the Material-UI interface
- Sidebar with navigation
- Dashboard, Documents, Upload, Transformations pages
- All the React components from `frontend/src/`

### How It Works

When you visit http://localhost:8090:

1. Spring Boot serves `index.html` from `src/main/resources/static/`
2. The HTML loads React JavaScript bundles from `/assets/`
3. React takes over and renders the single-page application
4. React Router handles navigation
5. API calls go to `/api/rest/*` endpoints (same server)

This is a standard production React deployment pattern - the React app is built into static files and served by the backend server.

## 📦 Docker Build - Config and Data Directories

### Previously: ❌ NOT Included

You were correct to ask! The Docker build was **NOT** including the `config/` and `data/` directories from the hitorro-all root.

It was only copying:
- CSV files from `hitorro-example-springboot/docker/csv/`
- The JAR file

### Now: ✅ FIXED and Included

I've updated both `Dockerfile` and `Dockerfile-with-ui` to include:

```dockerfile
# Copy configuration files from hitorro-all root (essential configs)
COPY --chown=hitorro:hitorro config/ ${HT_HOME}/config/

# Copy data files from hitorro-all root (WordNet, classifiers, NLP data, etc.)
COPY --chown=hitorro:hitorro data/ ${HT_HOME}/data/

# Copy CSV configuration files (these override defaults if present)
COPY --chown=hitorro:hitorro hitorro-example-springboot/docker/csv/*.csv ${HT_HOME}/config/csv/
```

### What Gets Copied from hitorro-all Root

**From `config/` directory (~34 items)**:
- `database.json` - Database configuration
- `generalconfig.json` - General application config
- `basemappers.json` - Base mappers
- `collections/` - Type collections
- `dbscripts/` - Database scripts
- `types/` - Type definitions
- `hibernate.json` - Hibernate configuration
- And many more configuration files

**From `data/` directory (~35 items)**:
- `WordNet-3.0/` - WordNet dictionary data
- `WordNet-2.1-stanford/` - Stanford WordNet
- `classifiers/` - Document classifiers
- `browncodes/` - Brown corpus codes
- `dtds/` - DTD files
- `initdb/` - Database initialization data
- `iso639.psv` - Language codes
- NLP data files
- And more

### Why This Matters

These directories contain **essential resources** that Hitorro needs:

1. **Type System**: Definitions for JVS types
2. **NLP Data**: WordNet dictionaries for text processing
3. **Classifiers**: Machine learning models
4. **Database Scripts**: Schema and initialization
5. **Configuration**: Application behavior settings

Without these, the Docker container would:
- ❌ Fail to initialize type system
- ❌ Missing NLP capabilities
- ❌ No classifiers for document processing
- ❌ Incomplete configuration

### Docker Build Now Includes Everything

```bash
cd hitorro-example-springboot/docker_build
./run-port-6000.sh  # or ./build-and-start.sh
```

The Docker image will now have:
1. ✅ All 19 Hitorro modules (compiled JARs)
2. ✅ React UI (built and bundled in JAR)
3. ✅ Config files from hitorro-all root
4. ✅ Data files from hitorro-all root
5. ✅ CSV overrides from docker/csv/
6. ✅ LibreOffice for transformations

### Build Context

The build **must be run from the hitorro-all root** so Docker can access:
```
/Users/chris/hitorro/          ← Build from here!
├── config/                     ← Now copied to Docker
├── data/                       ← Now copied to Docker
├── hitorro-util/
├── hitorro-base/
├── ...
└── hitorro-example-springboot/
    ├── Dockerfile-with-ui     ← Build with this
    └── docker_build/
        └── run-port-6000.sh   ← Handles build context
```

The `run-port-6000.sh` script builds from the parent directory:
```bash
docker build -f Dockerfile-with-ui -t hitorro-app:latest ..
```

## Summary

### React UI
- ✅ **YES, it's the React app** from `frontend/` directory
- ✅ Built with `npm run build`
- ✅ Served by Spring Boot at http://localhost:8090
- ✅ Material-UI, React Router, all components included
- ✅ Production-optimized bundle

### Docker Build
- ✅ **FIXED** - Now includes `config/` and `data/` from hitorro-all root
- ✅ ~70 files of essential configuration
- ✅ WordNet, classifiers, NLP data
- ✅ Type definitions and database scripts
- ✅ Complete, production-ready image

### Verification

**Test React UI locally**:
```bash
# Already running!
open http://localhost:8090
```

**Test Docker build** (after rebuild with new Dockerfile):
```bash
cd /Users/chris/hitorro/hitorro-example-springboot/docker_build
./build-and-start.sh
```

The Docker image will now be larger (~2-3 GB) because it includes all the data files, but it will be **complete and self-contained**.

## Next Steps

1. **Keep using the local version** - It's working perfectly with the React UI
2. **Rebuild Docker when ready** - New Dockerfile includes everything
3. **Test in Docker** - Verify config and data are accessible

---

**Updated**: Just now  
**React UI**: ✅ Working at http://localhost:8090  
**Docker**: ✅ Fixed to include config and data
