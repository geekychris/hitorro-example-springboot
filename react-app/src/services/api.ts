import axios from 'axios';
import type {
  Document,
  CreateDocumentRequest,
  UpdateDocumentRequest,
  QueryRequest,
  ContentInfo,
  VersionInfo,
  ContainerInfo,
  CrawlRequest,
  CrawlResult,
  JVSEnrichRequest,
  JVSEnrichResponse,
  CommandDefInfo,
  CommandExecuteRequest,
  CommandExecuteResponse,
  TypeDefinition,
} from '../types/api';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// DMS API
export const dmsApi = {
  // Documents
  createDocument: (data: CreateDocumentRequest) =>
    api.post<Document>('/dms/documents', data),
  
  getDocument: (id: number) =>
    api.get<Document>(`/dms/documents/${id}`),
  
  updateDocument: (id: number, data: UpdateDocumentRequest) =>
    api.put<Document>(`/dms/documents/${id}`, data),
  
  checkoutDocument: (id: number, versionType: 'major' | 'minor' = 'major') =>
    api.put<Document>(`/dms/documents/${id}/checkout?versionType=${versionType}`),

  deleteDocument: (id: number) =>
    api.delete(`/dms/documents/${id}`),
  
  queryDocuments: (query: QueryRequest) =>
    api.post<Document[]>('/dms/documents/query', query),
  
  // Content
  listContent: (documentId: number) =>
    api.get<ContentInfo[]>(`/dms/documents/${documentId}/content/list`),
  
  uploadContent: (documentId: number, file: File, rendition?: string) => {
    const formData = new FormData();
    formData.append('file', file);
    if (rendition) formData.append('rendition', rendition);
    return api.post(`/dms/documents/${documentId}/content`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  
  downloadContent: (documentId: number, contentId?: number) => {
    const url = contentId
      ? `/dms/documents/${documentId}/content/${contentId}/download`
      : `/dms/documents/${documentId}/content/download`;
    return api.get(url, { responseType: 'blob' });
  },
  
  // Versioning
  createVersion: (documentId: number, note?: string) =>
    api.post<Document>(`/dms/documents/${documentId}/version`, { note }),
  
  getVersionHistory: (documentId: number) =>
    api.get<VersionInfo[]>(`/dms/documents/${documentId}/versions`),
  
  // Containers
  attachToContainer: (documentId: number, containerId: number) =>
    api.post(`/dms/documents/${documentId}/containers/${containerId}`),
  
  detachFromContainer: (documentId: number, containerId: number) =>
    api.delete(`/dms/documents/${documentId}/containers/${containerId}`),
  
  listContainers: (documentId: number) =>
    api.get<ContainerInfo[]>(`/dms/documents/${documentId}/containers`),
  
  getDocumentsInContainer: (containerId: number, maxResults?: number) =>
    api.get<Document[]>(`/dms/containers/${containerId}/documents`, {
      params: { maxResults },
    }),
  
  // Categories
  addCategory: (documentId: number, domain: string, value: string) =>
    api.post(`/dms/documents/${documentId}/categories`, { domain, value }),
  
  removeCategory: (documentId: number, domain: string, value: string) =>
    api.delete(`/dms/documents/${documentId}/categories`, {
      params: { domain, value },
    }),
  
  searchByCategory: (domain: string, value: string) =>
    api.get<Document[]>('/dms/documents/search/category', {
      params: { domain, value },
    }),
  
  // Containers
  createContainer: (name: string, description?: string, parentContainerId?: number) =>
    api.post<ContainerInfo>('/dms/containers', { name, description, parentContainerId }),
  
  getContainer: (id: number) =>
    api.get<ContainerInfo>(`/dms/containers/${id}`),
  
  getAllContainers: () =>
    api.get<ContainerInfo[]>('/dms/containers'),
  
  deleteContainer: (id: number) =>
    api.delete(`/dms/containers/${id}`),
};

// Crawler API
export const crawlerApi = {
  crawl: (request: CrawlRequest) =>
    api.post<CrawlResult>('/dms/crawler/crawl', null, {
      params: request,
    }),
};

// Type System API
export const typeSystemApi = {
  enrichJVS: (request: JVSEnrichRequest) =>
    api.post<JVSEnrichResponse>('/jvs/enrich', request),
  
  getTypeDefinition: (typeName: string) =>
    api.get<TypeDefinition>(`/jvs/types/${typeName}`),
  
  listTypes: () =>
    api.get<string[]>('/jvs/types'),
  
  getFieldValue: (json: string, fieldPath: string) =>
    api.post<any>('/jvs/field', { json, fieldPath }),
};

// CommandDef API - Uses Hitorro CommandRegistry with @CommandDef annotation scanning
export const commandApi = {
  listCommands: (includeInternal?: boolean) =>
    api.get<CommandDefInfo[]>('/commands/list', {
      params: { includeInternal: includeInternal || false }
    }),
  
  getCommand: (commandName: string) =>
    api.get<CommandDefInfo>(`/commands/${commandName}`),
  
  executeCommand: (request: CommandExecuteRequest) =>
    api.post<CommandExecuteResponse>('/commands/execute', request),
};

export default api;
