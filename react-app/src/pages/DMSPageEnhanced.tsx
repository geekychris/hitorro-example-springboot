import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  FileText,
  FolderTree,
  Plus,
  Trash2,
  Edit,
  Edit2,
  Upload,
  Download,
  GitBranch,
  Tag,
  Search,
  Folder,
  ChevronRight,
  ChevronDown,
  File,
  Hash,
  Calendar,
  User,
  Database,
  RefreshCw,
  Check,
  AlertCircle,
  Layers,
  ExternalLink,
  Settings
} from 'lucide-react';
import { dmsApi } from '../services/api';
import type { Document, CreateDocumentRequest, ContainerInfo, VersionInfo } from '../types/api';

export default function DMSPageEnhanced() {
  const [selectedDocument, setSelectedDocument] = useState<Document | null>(null);
  const [selectedContainerId, setSelectedContainerId] = useState<number | null>(null);
  const [expandedContainers, setExpandedContainers] = useState<Set<number>>(new Set());
  const [showCreateDocument, setShowCreateDocument] = useState(false);
  const [showCreateContainer, setShowCreateContainer] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState<{ title: string; note: string }>({ title: '', note: '' });
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [showUpload, setShowUpload] = useState(false);
  const [showTransformer, setShowTransformer] = useState(false);
  const [selectedContent, setSelectedContent] = useState<any>(null);
  const [transformations, setTransformations] = useState<any[]>([]);
  const [selectedTemplateGuid, setSelectedTemplateGuid] = useState<string | null>(null);
  const [transformParameters, setTransformParameters] = useState<string>('');
  const queryClient = useQueryClient();

  // Query all containers
  const { data: allContainers = [] } = useQuery({
    queryKey: ['containers'],
    queryFn: () => dmsApi.getAllContainers().then(res => res.data),
  });

  // Query documents in selected container
  const { data: documents = [], isLoading: docsLoading } = useQuery({
    queryKey: ['documents', selectedContainerId],
    queryFn: () => {
      if (selectedContainerId === null) {
        // Show all root documents (no container)
        return dmsApi.queryDocuments({ maxResults: 1000 }).then(res => res.data);
      }
      return dmsApi.getDocumentsInContainer(selectedContainerId).then(res => res.data);
    },
  });

  // Query versions for selected document
  const { data: versions = [] } = useQuery({
    queryKey: ['versions', selectedDocument?.id],
    queryFn: () =>
      selectedDocument
        ? dmsApi.getVersionHistory(selectedDocument.id).then(res => res.data)
        : Promise.resolve([]),
    enabled: !!selectedDocument,
  });

  // Fetch content list for selected document
  const { data: contentList } = useQuery({
    queryKey: ['content', selectedDocument?.id],
    queryFn: () => selectedDocument ? dmsApi.listContent(selectedDocument.id) : Promise.resolve({ data: [] }),
    enabled: !!selectedDocument && (selectedDocument.contentCount || 0) > 0,
  });

  // Build hierarchical container structure
  // Supports many-to-many relationships (folders can have multiple parents)
  const buildContainerTree = (containers: ContainerInfo[]) => {
    const map = new Map<number, ContainerInfo & { children: ContainerInfo[] }>();
    const roots: (ContainerInfo & { children: ContainerInfo[] })[] = [];

    // Initialize all containers with children array
    containers.forEach(c => {
      map.set(c.id, { ...c, children: [] });
    });

    // Build tree structure using parentContainerIds array
    containers.forEach(c => {
      const node = map.get(c.id)!;
      // Check if this container has any parents
      if (c.parentContainerIds && c.parentContainerIds.length > 0) {
        // Add this node as a child to each of its parents
        c.parentContainerIds.forEach(parentId => {
          const parent = map.get(parentId);
          if (parent && !parent.children.includes(node)) {
            parent.children.push(node);
          }
        });
      } else {
        // No parents means it's a root container
        roots.push(node);
      }
    });

    return roots;
  };

  const containerTree = buildContainerTree(allContainers);

  const toggleContainer = (id: number) => {
    const newExpanded = new Set(expandedContainers);
    if (newExpanded.has(id)) {
      newExpanded.delete(id);
    } else {
      newExpanded.add(id);
    }
    setExpandedContainers(newExpanded);
  };

  const getContainerDisplayName = (container: ContainerInfo): string => {
    // If container/folder has a name, use it (Folder type has name field)
    if (container.name && container.name.trim()) {
      return container.name;
    }

    // Otherwise, extract from description (format: "Directory: /path/to/folder")
    if (container.description) {
      const match = container.description.match(/Directory: (.+)$/);
      if (match) {
        const fullPath = match[1];
        const parts = fullPath.split('/');
        const folderName = parts[parts.length - 1] || parts[parts.length - 2] || fullPath;
        return folderName;
      }
      // If description doesn't match pattern, use it directly
      return container.description.substring(0, 50); // Truncate long descriptions
    }

    // Fallback to type and ID
    return `${container.type} ${container.id}`;
  };

  const renderContainerNode = (
    container: ContainerInfo & { children: ContainerInfo[] },
    level: number = 0
  ) => {
    const isExpanded = expandedContainers.has(container.id);
    const isSelected = selectedContainerId === container.id;
    const hasChildren = container.children.length > 0;
    const displayName = getContainerDisplayName(container);

    return (
      <div key={container.id}>
        <div
          style={{
            padding: '0.5rem',
            paddingLeft: `${level * 1.5 + 0.5}rem`,
            cursor: 'pointer',
            background: isSelected ? 'var(--primary-light)' : 'transparent',
            borderRadius: '4px',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
          }}
          onClick={() => {
            setSelectedContainerId(container.id);
            setSelectedDocument(null);
            if (hasChildren) toggleContainer(container.id);
          }}
          title={container.description || displayName}
        >
          {hasChildren ? (
            isExpanded ? (
              <ChevronDown size={16} />
            ) : (
              <ChevronRight size={16} />
            )
          ) : (
            <span style={{ width: '16px' }} />
          )}
          <Folder size={16} color="var(--primary)" />
          <span style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {displayName}
          </span>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
            {container.documentCount || 0}
          </span>
        </div>
        {isExpanded && container.children.map(child => renderContainerNode(child, level + 1))}
      </div>
    );
  };

  return (
    <div>
      <div className="card">
        <div className="card-header">
          <span>Document Management System</span>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button
              className="button button-secondary"
              onClick={() => setShowCreateContainer(true)}
            >
              <Folder size={16} />
              New Folder
            </button>
            <button
              className="button button-primary"
              onClick={() => setShowCreateDocument(true)}
            >
              <Plus size={16} />
              New Document
            </button>
          </div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '300px 1fr 1fr', gap: '1.5rem', marginTop: '1rem' }}>
        {/* Left Panel - Container Tree */}
        <div className="card">
          <h3 style={{ marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <FolderTree size={18} />
            Containers
          </h3>
          <div
            style={{
              padding: '0.5rem',
              marginBottom: '0.5rem',
              cursor: 'pointer',
              background: selectedContainerId === null ? 'var(--primary-light)' : 'transparent',
              borderRadius: '4px',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
            }}
            onClick={() => {
              setSelectedContainerId(null);
              setSelectedDocument(null);
            }}
          >
            <Database size={16} />
            <span>All Documents</span>
          </div>
          <div style={{ maxHeight: '600px', overflowY: 'auto' }}>
            {containerTree.map(c => renderContainerNode(c))}
          </div>
        </div>

        {/* Middle Panel - Documents */}
        <div className="card">
          <h3 style={{ marginBottom: '1rem' }}>
            Documents
            {selectedContainerId && ` in ${allContainers.find(c => c.id === selectedContainerId)?.name}`}
          </h3>
          {docsLoading ? (
            <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
              Loading documents...
            </div>
          ) : documents.length === 0 ? (
            <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
              No documents found
            </div>
          ) : (
            <div style={{ maxHeight: '600px', overflowY: 'auto' }}>
              {documents.map(doc => (
                <div
                  key={doc.id}
                  style={{
                    padding: '0.75rem',
                    marginBottom: '0.5rem',
                    background: selectedDocument?.id === doc.id ? 'var(--primary-light)' : 'var(--surface)',
                    border: '1px solid var(--border)',
                    borderRadius: '4px',
                    cursor: 'pointer',
                  }}
                  onClick={() => setSelectedDocument(doc)}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
                    <File size={16} />
                    <strong>{doc.title}</strong>
                  </div>
                  <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginLeft: '1.5rem' }}>
                    <div>Version: {doc.versionLabel || '1.0'}</div>
                    <div>Modified: {new Date(doc.modifiedDate).toLocaleDateString()}</div>
                    {doc.categories.length > 0 && (
                      <div style={{ marginTop: '0.25rem', display: 'flex', gap: '0.25rem', flexWrap: 'wrap' }}>
                        {doc.categories.map((cat, idx) => (
                          <span
                            key={idx}
                            style={{
                              padding: '0.125rem 0.375rem',
                              background: 'var(--primary-light)',
                              borderRadius: '3px',
                              fontSize: '0.75rem',
                            }}
                          >
                            {cat.domain}: {cat.value}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Right Panel - Document Details */}
        <div className="card">
          <h3 style={{ marginBottom: '1rem' }}>Document Details</h3>
          {selectedDocument ? (
            <div style={{ maxHeight: '600px', overflowY: 'auto' }}>
              {/* Basic Info */}
              <div style={{ marginBottom: '1.5rem' }}>
                <h4 style={{ marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <FileText size={16} />
                  {selectedDocument.title}
                </h4>
                {selectedDocument.note && (
                  <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '0.5rem' }}>
                    {selectedDocument.note}
                  </p>
                )}
              </div>

              {/* Action Buttons */}
              <div style={{ marginBottom: '1.5rem', display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                <button
                  style={{
                    padding: '0.5rem 1rem',
                    background: 'var(--primary)',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    fontSize: '0.85rem',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem'
                  }}
                  onClick={() => {
                    setEditForm({ title: selectedDocument.title, note: selectedDocument.note || '' });
                    setIsEditing(true);
                  }}
                >
                  <Edit2 size={14} />
                  Edit
                </button>
                <button
                  style={{
                    padding: '0.5rem 1rem',
                    background: 'var(--success, #28a745)',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    fontSize: '0.85rem',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem'
                  }}
                  onClick={() => {
                    window.open(`/api/dms/documents/${selectedDocument.id}/content`, '_blank');
                  }}
                >
                  <FileText size={14} />
                  View Content ({selectedDocument.contentCount || 0})
                </button>
                <button
                  style={{
                    padding: '0.5rem 1rem',
                    background: 'var(--info, #17a2b8)',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    fontSize: '0.85rem',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem'
                  }}
                  onClick={() => setShowUpload(true)}
                >
                  <Upload size={14} />
                  Upload Content
                </button>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button
                    style={{
                      padding: '0.5rem 1rem',
                      background: 'var(--warning, #ffc107)',
                      color: 'black',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: 'pointer',
                      fontSize: '0.85rem',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.5rem'
                    }}
                    onClick={async () => {
                      if (!confirm('Create a new MAJOR version? (e.g., 1.0 → 2.0)')) return;
                      try {
                        const response = await dmsApi.checkoutDocument(selectedDocument.id, 'major');
                        alert(`New major version created!\nOld: ${selectedDocument.versionLabel}\nNew: ${response.data.versionLabel}\nID: ${response.data.id}`);
                        window.location.reload();
                      } catch (error) {
                        alert('Error creating major version: ' + error);
                      }
                    }}
                  >
                    <GitBranch size={14} />
                    Major Version
                  </button>
                  <button
                    style={{
                      padding: '0.5rem 1rem',
                      background: 'var(--info, #17a2b8)',
                      color: 'white',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: 'pointer',
                      fontSize: '0.85rem',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.5rem'
                    }}
                    onClick={async () => {
                      if (!confirm('Create a new MINOR version? (e.g., 1.0 → 1.1)')) return;
                      try {
                        const response = await dmsApi.checkoutDocument(selectedDocument.id, 'minor');
                        alert(`New minor version created!\nOld: ${selectedDocument.versionLabel}\nNew: ${response.data.versionLabel}\nID: ${response.data.id}`);
                        window.location.reload();
                      } catch (error) {
                        alert('Error creating minor version: ' + error);
                      }
                    }}
                  >
                    <GitBranch size={14} />
                    Minor Version
                  </button>
                </div>
                <button
                  style={{
                    padding: '0.5rem 1rem',
                    background: 'var(--secondary, #6c757d)',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    fontSize: '0.85rem',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem'
                  }}
                  onClick={() => {
                    // Download first content item or show list
                    if (selectedDocument.contentCount > 0) {
                      window.open(`/api/dms/documents/${selectedDocument.id}/content`, '_blank');
                    } else {
                      alert('No content available to download');
                    }
                  }}
                >
                  <Download size={14} />
                  Download
                </button>
              </div>

              {/* Edit Dialog */}
              {isEditing && (
                <div style={{
                  position: 'fixed',
                  top: 0,
                  left: 0,
                  right: 0,
                  bottom: 0,
                  background: 'rgba(0,0,0,0.5)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  zIndex: 1000
                }}>
                  <div style={{
                    background: 'var(--background)',
                    padding: '2rem',
                    borderRadius: '8px',
                    maxWidth: '500px',
                    width: '90%'
                  }}>
                    <h3 style={{ marginBottom: '1rem' }}>Edit Document</h3>
                    <div style={{ marginBottom: '1rem' }}>
                      <label style={{ display: 'block', marginBottom: '0.5rem' }}>Title:</label>
                      <input
                        type="text"
                        value={editForm.title}
                        onChange={(e) => setEditForm({ ...editForm, title: e.target.value })}
                        style={{
                          width: '100%',
                          padding: '0.5rem',
                          border: '1px solid var(--border)',
                          borderRadius: '4px'
                        }}
                      />
                    </div>
                    <div style={{ marginBottom: '1rem' }}>
                      <label style={{ display: 'block', marginBottom: '0.5rem' }}>Note:</label>
                      <textarea
                        value={editForm.note}
                        onChange={(e) => setEditForm({ ...editForm, note: e.target.value })}
                        rows={4}
                        style={{
                          width: '100%',
                          padding: '0.5rem',
                          border: '1px solid var(--border)',
                          borderRadius: '4px',
                          fontFamily: 'inherit'
                        }}
                      />
                    </div>
                    <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                      <button
                        onClick={() => setIsEditing(false)}
                        style={{
                          padding: '0.5rem 1rem',
                          background: 'var(--secondary, #6c757d)',
                          color: 'white',
                          border: 'none',
                          borderRadius: '4px',
                          cursor: 'pointer'
                        }}
                      >
                        Cancel
                      </button>
                      <button
                        onClick={async () => {
                          try {
                            await dmsApi.updateDocument(selectedDocument.id, editForm);
                            setIsEditing(false);
                            // Refresh document
                            window.location.reload();
                          } catch (error) {
                            alert('Error updating document: ' + error);
                          }
                        }}
                        style={{
                          padding: '0.5rem 1rem',
                          background: 'var(--primary)',
                          color: 'white',
                          border: 'none',
                          borderRadius: '4px',
                          cursor: 'pointer'
                        }}
                      >
                        Save
                      </button>
                    </div>
                  </div>
                </div>
              )}

              {/* Upload Dialog */}
              {showUpload && (
                <div style={{
                  position: 'fixed',
                  top: 0,
                  left: 0,
                  right: 0,
                  bottom: 0,
                  background: 'rgba(0,0,0,0.5)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  zIndex: 1000
                }}>
                  <div style={{
                    background: 'var(--background)',
                    padding: '2rem',
                    borderRadius: '8px',
                    maxWidth: '500px',
                    width: '90%'
                  }}>
                    <h3 style={{ marginBottom: '1rem' }}>Upload Content</h3>
                    <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '1rem' }}>
                      Maximum file size: 500 MB
                    </p>
                    <div style={{ marginBottom: '1rem' }}>
                      <input
                        type="file"
                        onChange={(e) => setUploadFile(e.target.files?.[0] || null)}
                        style={{
                          width: '100%',
                          padding: '0.5rem',
                          border: '1px solid var(--border)',
                          borderRadius: '4px'
                        }}
                      />
                    </div>
                    {uploadFile && (
                      <p style={{ fontSize: '0.85rem', color: uploadFile.size > 500 * 1024 * 1024 ? 'var(--danger, red)' : 'var(--text-secondary)', marginBottom: '1rem' }}>
                        Selected: {uploadFile.name} ({uploadFile.size > 1024 * 1024 ? (uploadFile.size / (1024 * 1024)).toFixed(1) + ' MB' : (uploadFile.size / 1024).toFixed(1) + ' KB'})
                        {uploadFile.size > 500 * 1024 * 1024 && <strong> ⚠️ FILE TOO LARGE!</strong>}
                      </p>
                    )}
                    <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                      <button
                        onClick={() => {
                          setShowUpload(false);
                          setUploadFile(null);
                        }}
                        style={{
                          padding: '0.5rem 1rem',
                          background: 'var(--secondary, #6c757d)',
                          color: 'white',
                          border: 'none',
                          borderRadius: '4px',
                          cursor: 'pointer'
                        }}
                      >
                        Cancel
                      </button>
                      <button
                        onClick={async () => {
                          if (!uploadFile) {
                            alert('Please select a file');
                            return;
                          }
                          try {
                            await dmsApi.uploadContent(selectedDocument.id, uploadFile);
                            setShowUpload(false);
                            setUploadFile(null);
                            // Refresh only the content list for this document (keep document selected)
                            queryClient.invalidateQueries({ queryKey: ['content', selectedDocument.id] });
                            queryClient.invalidateQueries({ queryKey: ['documents', selectedContainerId] });
                            alert('✅ Content uploaded successfully!');
                          } catch (error) {
                            alert('Error uploading file: ' + error);
                          }
                        }}
                        disabled={!uploadFile}
                        style={{
                          padding: '0.5rem 1rem',
                          background: uploadFile ? 'var(--primary)' : '#ccc',
                          color: 'white',
                          border: 'none',
                          borderRadius: '4px',
                          cursor: uploadFile ? 'pointer' : 'not-allowed'
                        }}
                      >
                        Upload
                      </button>
                    </div>
                  </div>
                </div>
              )}

              {/* Metadata */}
              <div style={{ marginBottom: '1.5rem' }}>
                <h4 style={{ fontSize: '0.9rem', marginBottom: '0.5rem', color: 'var(--text-secondary)' }}>
                  Metadata
                </h4>
                <div style={{ fontSize: '0.85rem', display: 'grid', gap: '0.5rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <Hash size={14} />
                    <strong>ID:</strong> {selectedDocument.id}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <Hash size={14} />
                    <strong>GUID:</strong> <code style={{ fontSize: '0.8rem' }}>{selectedDocument.guid}</code>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <User size={14} />
                    <strong>Creator:</strong> {selectedDocument.creator || 'N/A'}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <Calendar size={14} />
                    <strong>Created:</strong> {new Date(selectedDocument.creationDate).toLocaleString()}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <Calendar size={14} />
                    <strong>Modified:</strong> {new Date(selectedDocument.modifiedDate).toLocaleString()}
                  </div>
                  {selectedDocument.realm && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <Database size={14} />
                      <strong>Realm:</strong> {selectedDocument.realm}
                    </div>
                  )}
                </div>
              </div>

              {/* Categories */}
              {selectedDocument.categories.length > 0 && (
                <div style={{ marginBottom: '1.5rem' }}>
                  <h4 style={{ fontSize: '0.9rem', marginBottom: '0.5rem', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <Tag size={14} />
                    Categories ({selectedDocument.categories.length})
                  </h4>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                    {selectedDocument.categories.map((cat, idx) => (
                      <span
                        key={idx}
                        style={{
                          padding: '0.25rem 0.5rem',
                          background: 'var(--primary-light)',
                          border: '1px solid var(--primary)',
                          borderRadius: '4px',
                          fontSize: '0.85rem',
                        }}
                      >
                        <strong>{cat.domain}:</strong> {cat.value}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* Folders/Containers */}
              {selectedDocument.containers && selectedDocument.containers.length > 0 && (
                <div style={{ marginBottom: '1.5rem' }}>
                  <h4 style={{ fontSize: '0.9rem', marginBottom: '0.5rem', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <Folder size={14} />
                    Folders ({selectedDocument.containers.length})
                  </h4>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                    {selectedDocument.containers.map((container) => (
                      <span
                        key={container.id}
                        onClick={() => {
                          setSelectedContainerId(container.id);
                          setSelectedDocument(null);
                        }}
                        style={{
                          padding: '0.25rem 0.5rem',
                          background: 'var(--surface)',
                          border: '1px solid var(--border)',
                          borderRadius: '4px',
                          fontSize: '0.85rem',
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '0.25rem'
                        }}
                      >
                        📁 {getContainerDisplayName(container)}
                      </span>
                    ))}
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.5rem', fontStyle: 'italic' }}>
                    Click a folder to navigate to it
                  </div>
                </div>
              )}

              {/* Version Info */}
              <div style={{ marginBottom: '1.5rem' }}>
                <h4 style={{ fontSize: '0.9rem', marginBottom: '0.5rem', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <GitBranch size={14} />
                  Version Information
                </h4>
                <div style={{ fontSize: '0.85rem', display: 'grid', gap: '0.5rem' }}>
                  <div><strong>Version Label:</strong> {selectedDocument.versionLabel || '1.0'}</div>
                  {selectedDocument.canonicalId && (
                    <div><strong>Canonical ID:</strong> {selectedDocument.canonicalId}</div>
                  )}
                  {selectedDocument.parentVersionId && (
                    <div><strong>Parent Version:</strong> {selectedDocument.parentVersionId}</div>
                  )}
                </div>
              </div>

              {/* Version History */}
              {versions.length > 0 && (
                <div style={{ marginBottom: '1.5rem' }}>
                  <h4 style={{ fontSize: '0.9rem', marginBottom: '0.5rem', color: 'var(--text-secondary)' }}>
                    Version History ({versions.length})
                  </h4>
                  <div style={{ fontSize: '0.85rem' }}>
                    {versions.map((ver, idx) => (
                      <div
                        key={ver.id}
                        style={{
                          padding: '0.5rem',
                          marginBottom: '0.25rem',
                          background: ver.id === selectedDocument.id ? 'var(--primary-light)' : 'var(--surface)',
                          border: '1px solid var(--border)',
                          borderRadius: '4px',
                        }}
                      >
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
                          <strong>{ver.versionLabel || `v${versions.length - idx}`}</strong>
                          <span style={{ color: 'var(--text-secondary)' }}>
                            {new Date(ver.creationDate).toLocaleDateString()}
                          </span>
                        </div>
                        {ver.note && (
                          <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem' }}>
                            {ver.note}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Content */}
              {selectedDocument.contentCount > 0 && (
                <div style={{ marginBottom: '1.5rem' }}>
                  <h4 style={{ fontSize: '0.9rem', marginBottom: '0.5rem', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <FileText size={14} />
                    Content ({selectedDocument.contentCount})
                  </h4>
                  {contentList && contentList.data && contentList.data.length > 0 ? (
                    <div style={{ fontSize: '0.85rem', display: 'grid', gap: '0.5rem' }}>
                      {contentList.data.map((content: any, idx: number) => (
                        <div
                          key={content.id || idx}
                          style={{
                            padding: '0.75rem',
                            background: 'var(--surface)',
                            border: '1px solid var(--border)',
                            borderRadius: '4px',
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center'
                          }}
                        >
                          <div style={{ flex: 1 }}>
                            <div style={{ marginBottom: '0.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                              <FileText size={12} />
                              <strong>{content.originalFileName || content.fileName || `Content ${idx + 1}`}</strong>
                            </div>
                            <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem' }}>
                              {content.contentType && <span>Type: {content.contentType} • </span>}
                              {content.size && <span>Size: {(content.size / 1024).toFixed(1)} KB • </span>}
                              {content.storeName && <span>Store: {content.storeName}</span>}
                            </div>
                            {content.creationDate && (
                              <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginTop: '0.25rem' }}>
                                Added: {new Date(content.creationDate).toLocaleString()}
                              </div>
                            )}
                          </div>
                          <div style={{ display: 'flex', gap: '0.5rem' }}>
                            <button
                              style={{
                                padding: '0.5rem 0.75rem',
                                background: 'var(--primary)',
                                color: 'white',
                                border: 'none',
                                borderRadius: '4px',
                                cursor: 'pointer',
                                fontSize: '0.8rem',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '0.35rem'
                              }}
                              onClick={() => {
                                window.open(`/api/dms/documents/${selectedDocument.id}/content/${content.id}/download`, '_blank');
                              }}
                            >
                              <Download size={12} />
                              Download
                            </button>
                            <button
                              style={{
                                padding: '0.5rem 0.75rem',
                                background: '#667eea',
                                color: 'white',
                                border: 'none',
                                borderRadius: '4px',
                                cursor: 'pointer',
                                fontSize: '0.8rem',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '0.35rem'
                              }}
                              onClick={async () => {
                                setSelectedContent(content);
                                // Fetch available transformations - use guid from backend
                                const contentGuid = content.guid;
                                console.log('Content object:', content);
                                console.log('Using content GUID:', contentGuid);

                                if (!contentGuid) {
                                  alert('Error: Content GUID not found. Please refresh the page.');
                                  return;
                                }

                                try {
                                  const response = await fetch(`/api/transformer/content/${contentGuid}/available-transformations`);
                                  if (!response.ok) {
                                    const errorText = await response.text();
                                    throw new Error(`HTTP ${response.status}: ${errorText}`);
                                  }
                                  const data = await response.json();
                                  console.log('Transformation response:', data);
                                  console.log('Content MIME type:', content.contentType);
                                  console.log('Available transformations:', data.transformations);
                                  setTransformations(data.transformations || []);
                                  setShowTransformer(true);
                                  if (!data.transformations || data.transformations.length === 0) {
                                    console.warn('No transformations available for content type:', data.sourceMimeType);
                                  }
                                } catch (error) {
                                  console.error('Error loading transformations:', error);
                                  alert('Error loading transformations: ' + error);
                                }
                              }}
                            >
                              <RefreshCw size={12} />
                              Transform
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                      Loading content details...
                    </p>
                  )}
                </div>
              )}
            </div>
          ) : (
            <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
              Select a document to view details
            </div>
          )}
        </div>
      </div>

      {/* Create Container Dialog */}
      {showCreateContainer && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000
        }}>
          <div style={{
            background: 'var(--background)',
            padding: '2rem',
            borderRadius: '8px',
            maxWidth: '500px',
            width: '90%'
          }}>
            <h3 style={{ marginBottom: '1rem' }}>Create New Folder</h3>
            {selectedContainerId && (
              <div style={{
                marginBottom: '1rem',
                padding: '0.75rem',
                background: 'var(--primary-light)',
                borderRadius: '4px',
                fontSize: '0.9rem'
              }}>
                📁 Will be created inside: <strong>{getContainerDisplayName(allContainers.find(c => c.id === selectedContainerId)!)}</strong>
              </div>
            )}
            {!selectedContainerId && (
              <div style={{
                marginBottom: '1rem',
                padding: '0.75rem',
                background: 'var(--surface)',
                border: '1px solid var(--border)',
                borderRadius: '4px',
                fontSize: '0.9rem',
                color: 'var(--text-secondary)'
              }}>
                📁 Will be created at root level (select a folder first to create a subfolder)
              </div>
            )}
            <div style={{ marginBottom: '1rem' }}>
              <label style={{ display: 'block', marginBottom: '0.5rem' }}>Folder Name:</label>
              <input
                type="text"
                placeholder="Enter folder name"
                id="folderNameInput"
                style={{
                  width: '100%',
                  padding: '0.5rem',
                  border: '1px solid var(--border)',
                  borderRadius: '4px'
                }}
              />
            </div>
            <div style={{ marginBottom: '1rem' }}>
              <label style={{ display: 'block', marginBottom: '0.5rem' }}>Description (optional):</label>
              <textarea
                placeholder="Enter folder description"
                id="folderDescInput"
                rows={3}
                style={{
                  width: '100%',
                  padding: '0.5rem',
                  border: '1px solid var(--border)',
                  borderRadius: '4px'
                }}
              />
            </div>
            <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
              <button
                onClick={() => setShowCreateContainer(false)}
                style={{
                  padding: '0.5rem 1rem',
                  background: 'var(--secondary, #6c757d)',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer'
                }}
              >
                Cancel
              </button>
              <button
                onClick={async () => {
                  const nameInput = document.getElementById('folderNameInput') as HTMLInputElement;
                  const descInput = document.getElementById('folderDescInput') as HTMLTextAreaElement;
                  const name = nameInput.value.trim();

                  if (!name) {
                    alert('Please enter a folder name');
                    return;
                  }

                  try {
                    await dmsApi.createContainer(name, descInput.value.trim() || undefined, selectedContainerId || undefined);
                    setShowCreateContainer(false);
                    nameInput.value = '';
                    descInput.value = '';
                    queryClient.invalidateQueries({ queryKey: ['containers'] });
                    alert('Folder created successfully!');
                  } catch (error) {
                    alert('Error creating folder: ' + error);
                  }
                }}
                style={{
                  padding: '0.5rem 1rem',
                  background: 'var(--primary)',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer'
                }}
              >
                Create Folder
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Create Document Dialog */}
      {showCreateDocument && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000
        }}>
          <div style={{
            background: 'var(--background)',
            padding: '2rem',
            borderRadius: '8px',
            maxWidth: '500px',
            width: '90%'
          }}>
            <h3 style={{ marginBottom: '1rem' }}>Create New Document</h3>
            {selectedContainerId && (
              <div style={{
                marginBottom: '1rem',
                padding: '0.75rem',
                background: 'var(--primary-light)',
                borderRadius: '4px',
                fontSize: '0.9rem'
              }}>
                📄 Will be created inside: <strong>{getContainerDisplayName(allContainers.find(c => c.id === selectedContainerId)!)}</strong>
              </div>
            )}
            {!selectedContainerId && (
              <div style={{
                marginBottom: '1rem',
                padding: '0.75rem',
                background: 'var(--surface)',
                border: '1px solid var(--border)',
                borderRadius: '4px',
                fontSize: '0.9rem',
                color: 'var(--text-secondary)'
              }}>
                📄 Document will not be added to any folder (select a folder first to add it automatically)
              </div>
            )}
            <div style={{ marginBottom: '1rem' }}>
              <label style={{ display: 'block', marginBottom: '0.5rem' }}>Title:</label>
              <input
                type="text"
                placeholder="Enter document title"
                id="docTitleInput"
                style={{
                  width: '100%',
                  padding: '0.5rem',
                  border: '1px solid var(--border)',
                  borderRadius: '4px'
                }}
              />
            </div>
            <div style={{ marginBottom: '1rem' }}>
              <label style={{ display: 'block', marginBottom: '0.5rem' }}>Description (optional):</label>
              <textarea
                placeholder="Enter document description"
                id="docDescInput"
                rows={3}
                style={{
                  width: '100%',
                  padding: '0.5rem',
                  border: '1px solid var(--border)',
                  borderRadius: '4px'
                }}
              />
            </div>
            <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
              <button
                onClick={() => setShowCreateDocument(false)}
                style={{
                  padding: '0.5rem 1rem',
                  background: 'var(--secondary, #6c757d)',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer'
                }}
              >
                Cancel
              </button>
              <button
                onClick={async () => {
                  const titleInput = document.getElementById('docTitleInput') as HTMLInputElement;
                  const descInput = document.getElementById('docDescInput') as HTMLTextAreaElement;
                  const title = titleInput.value.trim();

                  if (!title) {
                    alert('Please enter a document title');
                    return;
                  }

                  try {
                    const response = await dmsApi.createDocument({
                      title,
                      note: descInput.value.trim() || undefined,
                      containerIds: selectedContainerId ? [selectedContainerId] : undefined
                    });
                    setShowCreateDocument(false);
                    titleInput.value = '';
                    descInput.value = '';
                    queryClient.invalidateQueries({ queryKey: ['documents'] });
                    setSelectedDocument(response.data);
                    alert('Document created successfully!');
                  } catch (error) {
                    alert('Error creating document: ' + error);
                  }
                }}
                style={{
                  padding: '0.5rem 1rem',
                  background: 'var(--primary)',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer'
                }}
              >
                Create Document
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Transformer Modal */}
      {showTransformer && selectedContent && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000
        }}>
          <div style={{
            background: 'white',
            padding: '2rem',
            borderRadius: '12px',
            maxWidth: '600px',
            width: '90%',
            maxHeight: '80vh',
            overflow: 'auto'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
              <h3 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <RefreshCw size={20} />
                Transform Content
              </h3>
              <button
                onClick={() => {
                  setShowTransformer(false);
                  setSelectedContent(null);
                  setTransformations([]);
                }}
                style={{
                  background: 'none',
                  border: 'none',
                  fontSize: '1.5rem',
                  cursor: 'pointer',
                  color: 'var(--text-secondary)'
                }}
              >
                ×
              </button>
            </div>

            <div style={{
              padding: '1rem',
              background: 'var(--surface)',
              borderRadius: '8px',
              marginBottom: '1.5rem'
            }}>
              <div style={{ fontWeight: 'bold', marginBottom: '0.5rem' }}>
                {selectedContent.originalFileName || selectedContent.fileName || 'Content'}
              </div>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                Type: {selectedContent.contentType} • Size: {(selectedContent.size / 1024).toFixed(1)} KB
              </div>
            </div>

            <div style={{ marginBottom: '1.5rem' }}>
              <h4 style={{ marginBottom: '0.5rem', fontSize: '0.95rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <FileText size={16} />
                Template (Optional)
              </h4>
              <select
                value={selectedTemplateGuid || ''}
                onChange={(e) => setSelectedTemplateGuid(e.target.value || null)}
                style={{
                  width: '100%',
                  padding: '0.75rem',
                  border: '1px solid var(--border)',
                  borderRadius: '6px',
                  background: 'var(--surface)',
                  fontSize: '0.9rem'
                }}
              >
                <option value="">-- No Template --</option>
                {contentList?.data?.filter((c: any) => c.id !== selectedContent.id).map((c: any) => (
                  <option key={c.guid} value={c.guid}>
                    {c.originalFileName || c.fileName} ({c.contentType})
                  </option>
                ))}
              </select>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.35rem' }}>
                Select a document to use as a template (e.g., a PDF form for data injection).
              </p>
            </div>
            <div style={{ marginBottom: '1.5rem' }}>
              <h4 style={{ marginBottom: '0.5rem', fontSize: '0.95rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Settings size={16} />
                Transformation Parameters (Optional JSON/CSV)
              </h4>
              <textarea
                value={transformParameters}
                onChange={(e) => setTransformParameters(e.target.value)}
                placeholder='e.g., {"field1": "value1"}'
                style={{
                  width: '100%',
                  height: '80px',
                  padding: '0.75rem',
                  border: '1px solid var(--border)',
                  borderRadius: '6px',
                  background: 'var(--surface)',
                  fontSize: '0.85rem',
                  fontFamily: 'monospace'
                }}
              />
              <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.35rem' }}>
                Provide JSON data for template-based transformers or key=value pairs for others.
              </p>
            </div>

            {transformations.length === 0 ? (
              <div style={{
                padding: '2rem',
                textAlign: 'center',
                color: 'var(--text-secondary)'
              }}>
                <p>No transformations available for this content type.</p>
                <p style={{ fontSize: '0.85rem', marginTop: '0.5rem' }}>
                  Make sure transformer dependencies are installed.
                </p>
              </div>
            ) : (
              <>
                <h4 style={{ marginBottom: '1rem', fontSize: '0.95rem' }}>
                  Available Transformations ({transformations.length})
                </h4>
                <div style={{ display: 'grid', gap: '0.75rem' }}>
                  {transformations.map((trans: any, idx: number) => (
                    <div
                      key={idx}
                      style={{
                        padding: '1rem',
                        border: '2px solid var(--border)',
                        borderRadius: '8px',
                        cursor: 'pointer',
                        transition: 'all 0.2s',
                        ':hover': {
                          borderColor: 'var(--primary)',
                          background: 'var(--surface)'
                        }
                      }}
                      onClick={async () => {
                        if (!confirm(`Transform ${selectedContent.originalFileName || 'content'} to ${trans.targetMimeType}${selectedTemplateGuid ? ' using selected template' : ''}?`)) {
                          return;
                        }

                        try {
                          const contentGuid = selectedContent.guid;
                          if (!contentGuid) {
                            alert('Error: Content GUID not found');
                            return;
                          }

                          // Get document GUID from selected document
                          const documentGuid = selectedDocument?.guid;
                          if (!documentGuid) {
                            alert('Error: Document GUID not found');
                            return;
                          }

                          const response = await fetch('/api/transformer/queue', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({
                              documentGuid: documentGuid,
                              contentGuid: contentGuid,
                              targetMimeType: trans.targetMimeType,
                              addAsChild: true,
                              templateGuid: selectedTemplateGuid,
                              parameters: transformParameters
                            })
                          });

                          if (response.ok) {
                            const result = await response.json();
                            alert(`✅ Transformation queued successfully!\n\nJob ID: ${result.jobId}\nThe transformed content will be added as a rendition when processing completes.`);
                            setShowTransformer(false);
                            setSelectedContent(null);
                            setTransformations([]);
                            setSelectedTemplateGuid(null);
                            setTransformParameters('');
                          } else {
                            const error = await response.text();
                            alert('Error queuing transformation: ' + error);
                          }
                        } catch (error) {
                          alert('Error: ' + error);
                        }
                      }}
                    >
                      <div style={{ fontWeight: 'bold', marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <FileText size={16} />
                        Convert to: {trans.targetMimeType}
                      </div>
                      <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                        Method: {trans.method} ({trans.transformer})
                        {trans.methodArgs && <div style={{ marginTop: '0.25rem' }}>Args: {trans.methodArgs}</div>}
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}

            <div style={{ marginTop: '1.5rem', paddingTop: '1rem', borderTop: '1px solid var(--border)' }}>
              <button
                onClick={() => {
                  setShowTransformer(false);
                  setSelectedContent(null);
                  setTransformations([]);
                }}
                style={{
                  width: '100%',
                  padding: '0.75rem',
                  background: 'var(--secondary, #6c757d)',
                  color: 'white',
                  border: 'none',
                  borderRadius: '6px',
                  cursor: 'pointer',
                  fontSize: '0.95rem'
                }}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
