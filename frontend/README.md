# Hitorro React UI

Modern React-based user interface for Hitorro Document Management System.

## Features

- **Dashboard**: System overview with health status and statistics
- **Document Management**: Browse, upload, download, and delete documents
- **Content Transformation**: Transform documents between formats
- **Drag & Drop Upload**: Easy file upload with progress tracking
- **Responsive Design**: Works on desktop, tablet, and mobile
- **Material-UI**: Modern, beautiful UI components
- **Real-time Updates**: React Query for data fetching and caching

## Technology Stack

- **React 18**: Modern React with hooks
- **Vite**: Fast build tool and dev server
- **React Router**: Client-side routing
- **Material-UI (MUI)**: Component library
- **React Query**: Data fetching and caching
- **Axios**: HTTP client
- **React Dropzone**: File upload
- **React Toastify**: Notifications

## Development

### Prerequisites

- Node.js 18+ and npm
- Running Hitorro backend on `http://localhost:8080`

### Setup

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

The app will be available at `http://localhost:3000`

### Available Scripts

```bash
# Development server with hot reload
npm run dev

# Production build
npm run build

# Preview production build
npm run preview

# Lint code
npm run lint
```

## Project Structure

```
frontend/
├── public/              # Static assets
├── src/
│   ├── components/      # Reusable components
│   │   ├── Header.jsx
│   │   └── Sidebar.jsx
│   ├── pages/          # Page components
│   │   ├── Dashboard.jsx
│   │   ├── Documents.jsx
│   │   ├── Upload.jsx
│   │   ├── Transformations.jsx
│   │   └── Settings.jsx
│   ├── services/       # API services
│   │   └── api.js
│   ├── App.jsx         # Main app component
│   ├── main.jsx        # Entry point
│   └── index.css       # Global styles
├── index.html
├── package.json
└── vite.config.js
```

## API Integration

The frontend communicates with the Hitorro backend through REST APIs:

- **DMS API**: Document management operations
- **Transformer API**: Content transformation
- **Commands API**: Execute system commands
- **System API**: Health and metrics

API configuration is in `src/services/api.js`.

### API Proxy

In development, Vite proxies API requests to the backend:

```javascript
// vite.config.js
proxy: {
  '/api': 'http://localhost:8080',
  '/actuator': 'http://localhost:8080'
}
```

## Building for Production

### Standalone Build

```bash
npm run build
```

Output will be in the `dist/` directory.

### Docker Build

The React app is automatically built and included in the Docker image:

```bash
# Build Docker image with UI
cd ..
docker build -f Dockerfile-with-ui -t hitorro-ui:latest .

# Or use docker-compose
docker-compose -f docker-compose-with-ui.yml up --build
```

The React build is copied to Spring Boot's `static/` directory, so Spring Boot serves the UI.

## Environment Variables

Create a `.env` file for environment-specific configuration:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

Access in code:

```javascript
const apiUrl = import.meta.env.VITE_API_BASE_URL
```

## Component Overview

### Header
Top navigation bar with app title and user actions.

### Sidebar
Left navigation menu with links to different sections.

### Dashboard
Home page showing system statistics and health status.

### Documents
Browse and manage documents with table view.

### Upload
Drag-and-drop file upload with progress tracking.

### Transformations
View available content transformations.

### Settings
Application configuration and preferences.

## Customization

### Theme

Edit the Material-UI theme in `src/main.jsx`:

```javascript
const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
  },
})
```

### Routes

Add new routes in `src/App.jsx`:

```javascript
<Routes>
  <Route path="/my-page" element={<MyPage />} />
</Routes>
```

### API Endpoints

Add new API functions in `src/services/api.js`:

```javascript
export const myApi = {
  getData: () => api.get('/my-endpoint'),
}
```

## Troubleshooting

### CORS Issues

If you encounter CORS errors, ensure the backend allows requests from `http://localhost:3000`:

```yaml
# application.yml
spring:
  web:
    cors:
      allowed-origins: http://localhost:3000
```

### API Connection Failed

1. Verify backend is running: `curl http://localhost:8080/actuator/health`
2. Check proxy configuration in `vite.config.js`
3. Verify API base URL

### Build Errors

```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install

# Clear Vite cache
rm -rf node_modules/.vite
npm run dev
```

## Deployment

### Spring Boot Static Resources

The production build is served by Spring Boot from `/static/`:

1. Build React app: `npm run build`
2. Copy `dist/` contents to `src/main/resources/static/`
3. Build Spring Boot app: `mvn package`

### Separate nginx Deployment

For separate deployment, use nginx to serve the React app:

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://backend:8080;
    }
}
```

## Performance Optimization

### Code Splitting

Vite automatically splits code. Manual chunks in `vite.config.js`:

```javascript
build: {
  rollupOptions: {
    output: {
      manualChunks: {
        'vendor': ['react', 'react-dom'],
      }
    }
  }
}
```

### Lazy Loading

Lazy load routes:

```javascript
const Documents = lazy(() => import('./pages/Documents'))
```

### Caching

React Query caches API responses for 5 minutes by default.

## License

Copyright © 2006-2025 Chris Collins
