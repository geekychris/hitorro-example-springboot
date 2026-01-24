import axios from 'axios'

// Create axios instance with default config
const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor
api.interceptors.request.use(
  (config) => {
    // Add auth token if available
    const token = localStorage.getItem('authToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Handle unauthorized
      localStorage.removeItem('authToken')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// DMS API
export const dmsApi = {
  // Documents
  getDocuments: (params) => api.get('/rest/dms/documents', { params }),
  getDocument: (id) => api.get(`/rest/dms/documents/${id}`),
  createDocument: (data) => api.post('/rest/dms/documents', data),
  updateDocument: (id, data) => api.put(`/rest/dms/documents/${id}`, data),
  deleteDocument: (id) => api.delete(`/rest/dms/documents/${id}`),
  
  // Folders
  getFolders: (params) => api.get('/rest/dms/folders', { params }),
  getFolder: (id) => api.get(`/rest/dms/folders/${id}`),
  createFolder: (data) => api.post('/rest/dms/folders', data),
  
  // Content
  uploadContent: (file, onProgress) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/rest/dms/content/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const percentCompleted = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          )
          onProgress(percentCompleted)
        }
      },
    })
  },
  downloadContent: (contentId) => 
    api.get(`/rest/dms/content/${contentId}/download`, {
      responseType: 'blob',
    }),
    
  // Versions
  getVersions: (documentId) => 
    api.get(`/rest/dms/documents/${documentId}/versions`),
  
  // Stores
  getStores: () => api.get('/rest/dms/stores'),
  getDefaultStore: () => api.get('/rest/dms/stores/default'),
}

// Transformer API
export const transformerApi = {
  getAvailableTransformations: () => 
    api.get('/rest/transformer/transformations'),
  transform: (contentId, targetFormat, options) =>
    api.post('/rest/transformer/transform', {
      contentId,
      targetFormat,
      options,
    }),
  getTransformationStatus: (jobId) =>
    api.get(`/rest/transformer/status/${jobId}`),
}

// Commands API
export const commandsApi = {
  executeCommand: (command, args) =>
    api.post('/commands/execute', { command, args }),
  getAvailableCommands: () => api.get('/commands/list'),
}

// System API
export const systemApi = {
  getHealth: () => api.get('/actuator/health'),
  getInfo: () => api.get('/actuator/info'),
  getMetrics: () => api.get('/actuator/metrics'),
}

export default api
