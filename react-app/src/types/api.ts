// API Type Definitions for Hitorro Spring Boot Example

export interface Document {
  id: number;
  guid: string;
  title: string;
  note?: string;
  creator?: string;
  realm?: string;
  versionLabel?: string;
  creationDate: string;
  modifiedDate: string;
  authoredDate?: string;
  authorId?: number;
  authorName?: string;
  categories: CategoryInfo[];
  containers: ContainerInfo[];
  contentCount: number;
  canonicalId?: number;
  parentVersionId?: number;
}

export interface CategoryInfo {
  domain: string;
  value: string;
}

export interface ContentInfo {
  id: number;
  originalFileName: string;
  contentSize: number;
  storeName: string;
  creationDate: string;
  width: number;
  height: number;
  durationSeconds: number;
  resolutionAux?: string;
  parentRenditionId?: number;
  renditionCount: number;
}

export interface VersionInfo {
  id: number;
  versionLabel: string;
  creationDate: string;
  modifiedDate: string;
  note?: string;
}

export interface ContainerInfo {
  id: number;
  guid: string;
  name?: string;
  description: string;
  type: string;
  parentContainerIds?: number[];  // Multiple parents supported (many-to-many)
  documentCount?: number;
}

export interface CreateDocumentRequest {
  title: string;
  note?: string;
  authorId?: number;
  creator?: string;
  realm?: string;
  categories?: CategoryInfo[];
}

export interface UpdateDocumentRequest {
  title?: string;
  note?: string;
  authorId?: number;
}

export interface QueryRequest {
  title?: string;
  authorId?: number;
  creator?: string;
  realm?: string;
  createdAfter?: string;
  createdBefore?: string;
  orderBy?: string;
  descending?: boolean;
  maxResults?: number;
}

export interface CrawlResult {
  sourcePath: string;
  storeName: string;
  rootContainerId: string;
  rootContainerName: string;
  filesProcessed: number;
  directoriesProcessed: number;
  errors: string[];
  filePaths: string[];
  startTime: string;
  endTime: string;
  success: boolean;
  durationMs: number;
}

export interface CrawlRequest {
  path: string;
  recursive?: boolean;
  maxDepth?: number;
  storeName?: string;
}

export interface JVSEnrichRequest {
  json: string;
  typeName?: string;
  tags?: string;  // Comma-separated tags: "ner,answers,segmented,parsed"
}

export interface JVSEnrichResponse {
  original: any;
  enriched: any;
  typeName: string;
  fields: FieldInfo[];
}

export interface FieldInfo {
  name: string;
  value: any;
  type: string;
  path: string;
}

export interface TypeDefinition {
  name: string;
  baseType?: string;
  fields: TypeField[];
  extends?: string[];
}

export interface TypeField {
  name: string;
  type: string;
  description?: string;
  defaultValue?: any;
  vector: boolean;
  i18n: boolean;
  dynamic: boolean;
  dynamicInfo?: DynamicInfo;
  groups?: GroupInfo[];
  primitive: boolean;
  primitiveType?: string;
}

export interface DynamicInfo {
  className: string;
  dependsOn?: string[];
}

export interface GroupInfo {
  name: string;
  method: string;
  tags?: string[];
}

export interface CommandDefInfo {
  name: string;
  description: string;
  parameters: CommandParameter[];
  returnType: string;
  internal: boolean;
  restOperations?: string[];  // HTTP methods this command supports (GET, POST, etc.)
}

export interface CommandParameter {
  name: string;
  type: string;
  required: boolean;
  defaultValue?: any;
  description?: string;
}

export interface CommandExecuteRequest {
  commandName: string;
  parameters: Record<string, any>;
}

export interface CommandExecuteResponse {
  success: boolean;
  result?: any;
  error?: string;
  executionTimeMs: number;
}
