# Hitorro React UI Integration Guide

This guide explains how to build and deploy the Hitorro Example Spring Boot application with the React UI.

## Overview

The React UI provides a modern web interface for Hitorro DMS with:
- Document management
- File upload with drag & drop
- Content transformation
- System monitoring
- Responsive design

## Architecture

```
┌─────────────────┐
│   React UI      │ ← User interacts with browser
│   (Port 3000)   │
└────────┬────────┘
         │ HTTP API calls
         │
┌────────▼────────┐
│  Spring Boot    │ ← Backend API + Static files
│   (Port 8080)   │
└────────┬────────┘
         │
┌────────▼────────┐
│   Database      │
│   (H2/Postgres) │
└─────────────────┘
```

## Deployment Options

### Option 1: Integrated Deployment (Recommended)

React build is included in the Spring Boot JAR and served as static content.

**Pros:**
- Single container/JAR
- Simple deployment
- No CORS issues
- Automatic routing

**Build:**
```bash
# Using Docker
docker build -f Dockerfile-with-ui -t hitorro-ui:latest .
docker run -p 8080:8080 hitorro-ui:latest

# Or with docker-compose
docker-compose -f docker-compose-with-ui.yml up --build
```

**Access:**
- UI: http://localhost:8080
- API: http://localhost:8080/api
- Swagger: http://localhost:8080/swagger-ui.html

### Option 2: Separate Development

Run React dev server separately for development hot-reload.

**Start Backend:**
```bash
cd hitorro-example-springboot
mvn spring-boot:run
```

**Start Frontend:**
```bash
cd hitorro-example-springboot/frontend
npm install
npm run dev
```

**Access:**
- UI: http://localhost:3000 (Vite dev server)
- API: http://localhost:8080 (proxied through Vite)

### Option 3: Separate Production (nginx)

Deploy React and Spring Boot separately with nginx reverse proxy.

**docker-compose-nginx.yml:**
```yaml
version: '3.8'

services:
  backend:
    image: hitorro-example-springboot:latest
    expose:
      - "8080"
  
  frontend:
    image: nginx:alpine
    volumes:
      - ./frontend/dist:/usr/share/nginx/html
      - ./nginx.conf:/etc/nginx/nginx.conf
    ports:
      - "80:80"
    depends_on:
      - backend
```

## Quick Start Guide

### 1. Development Mode (Hot Reload)

```bash
# Terminal 1: Start backend
cd hitorro-example-springboot
mvn spring-boot:run

# Terminal 2: Start frontend
cd frontend
npm install
npm run dev

# Access UI at http://localhost:3000
```

### 2. Production Build (Integrated)

```bash
# Build everything in Docker
cd hitorro-example-springboot
docker-compose -f docker-compose-with-ui.yml up --build

# Access UI at http://localhost:8080
```

### 3. Manual Production Build

```bash
# Build React app
cd frontend
npm install
npm run build

# Copy to Spring Boot static resources
cp -r dist/* ../src/main/resources/static/

# Build Spring Boot
cd ..
mvn clean package

# Run
java -jar target/hitorro-example-springboot-*.jar

# Access UI at http://localhost:8080
```

## Configuration

### Backend (Spring Boot)

Configure CORS if running separately:

```yaml
# application.yml
spring:
  web:
    cors:
      allowed-origins:
        - http://localhost:3000
        - http://localhost:8080
      allowed-methods: "*"
      allowed-headers: "*"
```

Configure static resource handling:

```yaml
spring:
  web:
    resources:
      static-locations: classpath:/static/
      cache:
        cachecontrol:
          max-age: 31536000  # 1 year for production
```

### Frontend (React)

Configure API endpoint:

```javascript
// vite.config.js
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})
```

Environment variables:

```bash
# .env.development
VITE_API_BASE_URL=http://localhost:8080/api

# .env.production
VITE_API_BASE_URL=/api
```

## Build Scripts

### Build Script with UI

```bash
#!/bin/bash
# docker-build-ui.sh

cd hitorro-example-springboot

echo "Building React UI..."
cd frontend
npm install
npm run build

echo "Building Docker image with UI..."
cd ../..
docker build -f hitorro-example-springboot/Dockerfile-with-ui \
  -t hitorro-ui:latest .

echo "✓ Build complete!"
echo "Run: docker run -p 8080:8080 hitorro-ui:latest"
```

Make it executable:
```bash
chmod +x docker-build-ui.sh
./docker-build-ui.sh
```

## Features

### Dashboard Page
- System health status
- Document statistics
- Store information
- Quick links to admin tools

### Documents Page
- List all documents
- Download documents
- Delete documents
- View document details
- Refresh data

### Upload Page
- Drag & drop file upload
- Multiple file upload
- Upload progress tracking
- File list management

### Transformations Page
- View available transformations
- Transform documents
- Check transformation status

### Settings Page
- General settings
- Notification preferences
- Storage configuration
- About information

## API Endpoints

The UI interacts with these backend APIs:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/rest/dms/documents` | GET | List documents |
| `/api/rest/dms/documents/{id}` | GET | Get document |
| `/api/rest/dms/content/upload` | POST | Upload file |
| `/api/rest/dms/stores` | GET | List stores |
| `/api/rest/transformer/transformations` | GET | List transformations |
| `/actuator/health` | GET | System health |

## Customization

### Change Theme Colors

Edit `src/main.jsx`:

```javascript
const theme = createTheme({
  palette: {
    primary: { main: '#YOUR_COLOR' },
    secondary: { main: '#YOUR_COLOR' },
  },
})
```

### Add New Page

1. Create page component: `src/pages/MyPage.jsx`
2. Add route in `src/App.jsx`:
   ```javascript
   <Route path="/mypage" element={<MyPage />} />
   ```
3. Add menu item in `src/components/Sidebar.jsx`

### Add New API Call

Edit `src/services/api.js`:

```javascript
export const myApi = {
  getMyData: () => api.get('/my-endpoint'),
  postMyData: (data) => api.post('/my-endpoint', data),
}
```

Use in component:

```javascript
const { data } = useQuery('myData', () => myApi.getMyData())
```

## Troubleshooting

### UI Shows Blank Page

1. Check browser console for errors
2. Verify backend is running: `curl http://localhost:8080/actuator/health`
3. Check network tab for failed API calls
4. Ensure static resources are served

### API Calls Fail with 404

1. Verify API base path configuration
2. Check proxy settings in `vite.config.js`
3. Verify backend REST endpoints are enabled

### Build Fails

```bash
# Clear caches
rm -rf node_modules frontend/node_modules
rm -rf frontend/dist

# Reinstall
cd frontend
npm install
npm run build
```

### CORS Errors

Add CORS configuration to Spring Boot:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("*");
    }
}
```

## Performance Tips

1. **Enable gzip compression** in Spring Boot:
   ```yaml
   server:
     compression:
       enabled: true
   ```

2. **Use React.memo** for expensive components

3. **Enable React Query caching**:
   ```javascript
   staleTime: 5 * 60 * 1000  // 5 minutes
   ```

4. **Code splitting** (automatic with Vite)

5. **Lazy load routes**:
   ```javascript
   const Documents = lazy(() => import('./pages/Documents'))
   ```

## Production Checklist

- [ ] Set `NODE_ENV=production`
- [ ] Optimize images and assets
- [ ] Enable compression
- [ ] Set appropriate cache headers
- [ ] Remove console.log statements
- [ ] Enable HTTPS
- [ ] Configure security headers
- [ ] Set up monitoring
- [ ] Test on multiple browsers
- [ ] Test responsive design

## Support

For issues:
1. Check frontend console for errors
2. Check backend logs
3. Verify API endpoints with Swagger UI
4. Check network tab in browser DevTools

## Next Steps

- Add authentication/authorization
- Implement search functionality
- Add document preview
- Implement real-time notifications
- Add user management
- Implement advanced filtering
- Add file version comparison
- Implement collaborative features

## License

Copyright © 2006-2025 Chris Collins
