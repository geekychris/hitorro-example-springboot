import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { 
  FileText, 
  FolderTree, 
  Upload, 
  Download, 
  Plus, 
  Trash2, 
  Edit, 
  GitBranch,
  Tag,
  Search,
  Folder
} from 'lucide-react';
import { dmsApi } from '../services/api';
import type { Document, CreateDocumentRequest, UpdateDocumentRequest, QueryRequest, ContainerInfo } from '../types/api';

export default function DMSPage() {
  const [selectedDocument, setSelectedDocument] = useState<Document | null>(null);
  const [selectedContainer, setSelectedContainer] = useState<number | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [showCreateContainerForm, setShowCreateContainerForm] = useState(false);
  const [showEditForm, setShowEditForm] = useState(false);
  const [showQueryForm, setShowQueryForm] = useState(false);
  const queryClient = useQueryClient();

  // Query containers
  const { data: containers } = useQuery({
    queryKey: ['containers'],
    queryFn: () => dmsApi.getAllContainers().then(res => res.data),
  });

  // Query documents - either by container or all
  const [queryParams, setQueryParams] = useState<QueryRequest>({ maxResults: 100 });
  const { data: documents, isLoading } = useQuery({
    queryKey: ['documents', queryParams, selectedContainer],
    queryFn: () => {
      if (selectedContainer) {
        return dmsApi.getDocumentsInContainer(selectedContainer).then(res => res.data);
      }
      return dmsApi.queryDocuments(queryParams).then(res => res.data);
    },
  });

  // Mutations
  const createMutation = useMutation({
    mutationFn: (data: CreateDocumentRequest) => dmsApi.createDocument(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents'] });
      setShowCreateForm(false);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateDocumentRequest }) =>
      dmsApi.updateDocument(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents'] });
      setShowEditForm(false);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => dmsApi.deleteDocument(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents'] });
      setSelectedDocument(null);
    },
  });

  const createContainerMutation = useMutation({
    mutationFn: ({ name, description }: { name: string; description?: string }) =>
      dmsApi.createContainer(name, description),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['containers'] });
      setShowCreateContainerForm(false);
    },
  });

  const deleteContainerMutation = useMutation({
    mutationFn: (containerId: number) => dmsApi.deleteContainer(containerId),
    onSuccess: (_, containerId) => {
      queryClient.invalidateQueries({ queryKey: ['containers'] });
      if (selectedContainer === containerId) {
        setSelectedContainer(null);
      }
    },
  });

  return (
    <div>
      <div className="card">
        <div className="card-header">
          <span>Document Management System - Hierarchical View</span>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button
              className="button button-secondary"
              onClick={() => setShowCreateContainerForm(!showCreateContainerForm)}
            >
              <Folder size={16} />
              New Folder
            </button>
            <button
              className="button button-secondary"
              onClick={() => setShowQueryForm(!showQueryForm)}
            >
              <Search size={16} />
              Search
            </button>
            <button
              className="button button-primary"
              onClick={() => setShowCreateForm(!showCreateForm)}
            >
              <Plus size={16} />
              New Document
            </button>
          </div>
        </div>

        {showCreateContainerForm && (
          <CreateContainerForm
            onSubmit={(name, description) => createContainerMutation.mutate({ name, description })}
            onCancel={() => setShowCreateContainerForm(false)}
            isLoading={createContainerMutation.isPending}
          />
        )}

        {showQueryForm && (
          <QueryForm
            onSubmit={(query) => {
              setQueryParams(query);
              setSelectedContainer(null);
              setShowQueryForm(false);
            }}
            onCancel={() => setShowQueryForm(false)}
          />
        )}

        {showCreateForm && (
          <CreateDocumentForm
            onSubmit={(data) => createMutation.mutate(data)}
            onCancel={() => setShowCreateForm(false)}
            isLoading={createMutation.isPending}
          />
        )}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '300px 1fr 1fr', gap: '1.5rem' }}>
        <div className="card">
          <h3 style={{ marginBottom: '1rem' }}>
            <FolderTree size={18} style={{ marginRight: '0.5rem', display: 'inline' }} />
            Containers
          </h3>
          <ContainerTree
            containers={containers || []}
            selectedId={selectedContainer}
            onSelect={(id) => {
              setSelectedContainer(id);
              setSelectedDocument(null);
            }}
            onDelete={(id) => {
              if (confirm('Delete this container?')) {
                deleteContainerMutation.mutate(id);
              }
            }}
          />
        </div>
        <div className="card">
          <h3 style={{ marginBottom: '1rem' }}>Documents</h3>
          {isLoading ? (
            <div className="loading">Loading documents...</div>
          ) : (
            <DocumentList
              documents={documents || []}
              selectedId={selectedDocument?.id}
              onSelect={setSelectedDocument}
            />
          )}
        </div>

        <div className="card">
          <h3 style={{ marginBottom: '1rem' }}>Details</h3>
          {selectedDocument ? (
            <>
              {showEditForm ? (
                <EditDocumentForm
                  document={selectedDocument}
                  onSubmit={(data) =>
                    updateMutation.mutate({ id: selectedDocument.id, data })
                  }
                  onCancel={() => setShowEditForm(false)}
                  isLoading={updateMutation.isPending}
                />
              ) : (
                <DocumentDetails
                  document={selectedDocument}
                  onEdit={() => setShowEditForm(true)}
                  onDelete={() => {
                    if (confirm('Delete this document?')) {
                      deleteMutation.mutate(selectedDocument.id);
                    }
                  }}

                />
              )}
            </>
          ) : (
            <div style={{ color: 'var(--text-secondary)', padding: '2rem', textAlign: 'center' }}>
              Select a document to view details
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function DocumentList({
  documents,
  selectedId,
  onSelect,
}: {
  documents: Document[];
  selectedId?: number;
  onSelect: (doc: Document) => void;
}) {
  if (documents.length === 0) {
    return (
      <div style={{ color: 'var(--text-secondary)', padding: '2rem', textAlign: 'center' }}>
        No documents found
      </div>
    );
  }

  return (
    <div style={{ maxHeight: '600px', overflowY: 'auto' }}>
      <table className="table">
        <thead>
          <tr>
            <th>Title</th>
            <th>Version</th>
            <th>Modified</th>
          </tr>
        </thead>
        <tbody>
          {documents.map((doc) => (
            <tr
              key={doc.id}
              onClick={() => onSelect(doc)}
              style={{
                cursor: 'pointer',
                background: selectedId === doc.id ? 'var(--background)' : undefined,
              }}
            >
              <td>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <FileText size={16} />
                  {doc.title}
                </div>
              </td>
              <td>
                <span className="badge badge-primary">{doc.versionLabel || 'N/A'}</span>
              </td>
              <td>{new Date(doc.modifiedDate).toLocaleDateString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function DocumentDetails({
  document,
  onEdit,
  onDelete,
}: {
  document: Document;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const queryClient = useQueryClient();
  const [uploadingContent, setUploadingContent] = useState(false);

  const { data: versions } = useQuery({
    queryKey: ['versions', document.id],
    queryFn: () => dmsApi.getVersionHistory(document.id).then(res => res.data),
  });

  const { data: contents } = useQuery({
    queryKey: ['contents', document.id],
    queryFn: () => dmsApi.listContent(document.id).then(res => res.data),
  });

  const versionMutation = useMutation({
    mutationFn: () => dmsApi.createVersion(document.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['versions', document.id] });
      queryClient.invalidateQueries({ queryKey: ['documents'] });
    },
  });

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploadingContent(true);
    try {
      await dmsApi.uploadContent(document.id, file);
      queryClient.invalidateQueries({ queryKey: ['contents', document.id] });
    } catch (error) {
      console.error('Upload failed:', error);
      alert('Upload failed');
    } finally {
      setUploadingContent(false);
    }
  };

  const handleDownload = async () => {
    try {
      const response = await dmsApi.downloadContent(document.id);
      const blob = new Blob([response.data]);
      const url = window.URL.createObjectURL(blob);
      const a = window.document.createElement('a');
      a.href = url;
      a.download = document.title || 'download';
      a.click();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Download failed:', error);
      alert('Download failed');
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
        <button className="button button-secondary" onClick={onEdit}>
          <Edit size={16} />
          Edit
        </button>
        <button className="button button-danger" onClick={onDelete}>
          <Trash2 size={16} />
          Delete
        </button>
        <button className="button button-success" onClick={() => versionMutation.mutate()}>
          <GitBranch size={16} />
          New Version
        </button>
      </div>

      <div className="form-group">
        <strong>ID:</strong> {document.id}
      </div>
      <div className="form-group">
        <strong>GUID:</strong> {document.guid}
      </div>
      <div className="form-group">
        <strong>Title:</strong> {document.title}
      </div>
      {document.note && (
        <div className="form-group">
          <strong>Note:</strong> {document.note}
        </div>
      )}
      <div className="form-group">
        <strong>Version:</strong> {document.versionLabel || 'N/A'}
      </div>
      <div className="form-group">
        <strong>Created:</strong> {new Date(document.creationDate).toLocaleString()}
      </div>
      <div className="form-group">
        <strong>Modified:</strong> {new Date(document.modifiedDate).toLocaleString()}
      </div>

      {document.categories.length > 0 && (
        <div className="form-group">
          <strong>Categories:</strong>
          <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem', flexWrap: 'wrap' }}>
            {document.categories.map((cat, idx) => (
              <span key={idx} className="badge badge-primary">
                <Tag size={12} style={{ marginRight: '0.25rem' }} />
                {cat.domain}: {cat.value}
              </span>
            ))}
          </div>
        </div>
      )}

      <hr style={{ margin: '1rem 0', border: 'none', borderTop: '1px solid var(--border)' }} />

      <h4 style={{ marginBottom: '0.5rem' }}>Content ({contents?.length || 0})</h4>
      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
        <label className="button button-primary" style={{ cursor: 'pointer' }}>
          <Upload size={16} />
          Upload
          <input
            type="file"
            onChange={handleUpload}
            style={{ display: 'none' }}
            disabled={uploadingContent}
          />
        </label>
        {document.contentCount > 0 && (
          <button className="button button-secondary" onClick={handleDownload}>
            <Download size={16} />
            Download
          </button>
        )}
      </div>

      {contents && contents.length > 0 && (
        <div style={{ fontSize: '0.875rem' }}>
          {contents.map((content) => (
            <div key={content.id} style={{ padding: '0.5rem', background: 'var(--background)', marginBottom: '0.5rem', borderRadius: '0.25rem' }}>
              <div><strong>File:</strong> {content.originalFileName}</div>
              <div><strong>Size:</strong> {(content.contentSize / 1024).toFixed(2)} KB</div>
              <div><strong>Store:</strong> {content.storeName}</div>
            </div>
          ))}
        </div>
      )}

      <hr style={{ margin: '1rem 0', border: 'none', borderTop: '1px solid var(--border)' }} />

      <h4 style={{ marginBottom: '0.5rem' }}>Version History ({versions?.length || 0})</h4>
      {versions && versions.length > 0 && (
        <div style={{ fontSize: '0.875rem', maxHeight: '200px', overflowY: 'auto' }}>
          {versions.map((ver) => (
            <div key={ver.id} style={{ padding: '0.5rem', background: 'var(--background)', marginBottom: '0.5rem', borderRadius: '0.25rem' }}>
              <div><strong>Version:</strong> {ver.versionLabel}</div>
              <div><strong>Modified:</strong> {new Date(ver.modifiedDate).toLocaleString()}</div>
              {ver.note && <div><strong>Note:</strong> {ver.note}</div>}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function CreateDocumentForm({
  onSubmit,
  onCancel,
  isLoading,
}: {
  onSubmit: (data: CreateDocumentRequest) => void;
  onCancel: () => void;
  isLoading: boolean;
}) {
  const [formData, setFormData] = useState<CreateDocumentRequest>({
    title: '',
    note: '',
    creator: '',
  });

  return (
    <div style={{ border: '1px solid var(--border)', padding: '1rem', borderRadius: '0.5rem', marginBottom: '1rem' }}>
      <h4 style={{ marginBottom: '1rem' }}>Create New Document</h4>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          onSubmit(formData);
        }}
      >
        <div className="form-group">
          <label className="label">Title *</label>
          <input
            className="input"
            value={formData.title}
            onChange={(e) => setFormData({ ...formData, title: e.target.value })}
            required
          />
        </div>
        <div className="form-group">
          <label className="label">Note</label>
          <textarea
            className="textarea"
            value={formData.note}
            onChange={(e) => setFormData({ ...formData, note: e.target.value })}
          />
        </div>
        <div className="form-group">
          <label className="label">Creator</label>
          <input
            className="input"
            value={formData.creator}
            onChange={(e) => setFormData({ ...formData, creator: e.target.value })}
          />
        </div>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button type="submit" className="button button-primary" disabled={isLoading}>
            {isLoading ? 'Creating...' : 'Create'}
          </button>
          <button type="button" className="button button-secondary" onClick={onCancel}>
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}

function EditDocumentForm({
  document,
  onSubmit,
  onCancel,
  isLoading,
}: {
  document: Document;
  onSubmit: (data: UpdateDocumentRequest) => void;
  onCancel: () => void;
  isLoading: boolean;
}) {
  const [formData, setFormData] = useState<UpdateDocumentRequest>({
    title: document.title,
    note: document.note || '',
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        onSubmit(formData);
      }}
    >
      <div className="form-group">
        <label className="label">Title</label>
        <input
          className="input"
          value={formData.title}
          onChange={(e) => setFormData({ ...formData, title: e.target.value })}
        />
      </div>
      <div className="form-group">
        <label className="label">Note</label>
        <textarea
          className="textarea"
          value={formData.note}
          onChange={(e) => setFormData({ ...formData, note: e.target.value })}
        />
      </div>
      <div style={{ display: 'flex', gap: '0.5rem' }}>
        <button type="submit" className="button button-primary" disabled={isLoading}>
          {isLoading ? 'Saving...' : 'Save'}
        </button>
        <button type="button" className="button button-secondary" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </form>
  );
}

function QueryForm({
  onSubmit,
  onCancel,
}: {
  onSubmit: (query: QueryRequest) => void;
  onCancel: () => void;
}) {
  const [formData, setFormData] = useState<QueryRequest>({
    maxResults: 100,
  });

  return (
    <div style={{ border: '1px solid var(--border)', padding: '1rem', borderRadius: '0.5rem', marginBottom: '1rem' }}>
      <h4 style={{ marginBottom: '1rem' }}>Search Documents</h4>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          onSubmit(formData);
        }}
      >
        <div className="grid grid-2">
          <div className="form-group">
            <label className="label">Title</label>
            <input
              className="input"
              value={formData.title || ''}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              placeholder="Search by title..."
            />
          </div>
          <div className="form-group">
            <label className="label">Creator</label>
            <input
              className="input"
              value={formData.creator || ''}
              onChange={(e) => setFormData({ ...formData, creator: e.target.value })}
              placeholder="Search by creator..."
            />
          </div>
        </div>
        <div className="form-group">
          <label className="label">Max Results</label>
          <input
            className="input"
            type="number"
            value={formData.maxResults || 100}
            onChange={(e) => setFormData({ ...formData, maxResults: parseInt(e.target.value) })}
          />
        </div>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button type="submit" className="button button-primary">
            Search
          </button>
          <button type="button" className="button button-secondary" onClick={onCancel}>
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}

function ContainerTree({
  containers,
  selectedId,
  onSelect,
  onDelete,
}: {
  containers: ContainerInfo[];
  selectedId: number | null;
  onSelect: (id: number) => void;
  onDelete: (id: number) => void;
}) {
  if (containers.length === 0) {
    return (
      <div style={{ color: 'var(--text-secondary)', padding: '2rem', textAlign: 'center', fontSize: '0.875rem' }}>
        No containers yet. Create a folder to organize documents.
      </div>
    );
  }

  return (
    <div style={{ maxHeight: '600px', overflowY: 'auto' }}>
      {containers.map((container) => (
        <div
          key={container.id}
          style={{
            padding: '0.75rem',
            marginBottom: '0.25rem',
            cursor: 'pointer',
            background: selectedId === container.id ? 'var(--background)' : undefined,
            borderRadius: '0.375rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            border: selectedId === container.id ? '2px solid var(--primary)' : '1px solid var(--border)',
          }}
          onClick={() => onSelect(container.id)}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flex: 1 }}>
            <Folder size={16} color="var(--primary)" />
            <div>
              <div style={{ fontWeight: 500, fontSize: '0.875rem' }}>
                {container.description || `Container ${container.id}`}
              </div>
              {container.guid && (
                <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                  {container.type}
                </div>
              )}
            </div>
          </div>
          <button
            className="button button-danger"
            onClick={(e) => {
              e.stopPropagation();
              onDelete(container.id);
            }}
            style={{ padding: '0.25rem 0.5rem', fontSize: '0.75rem' }}
          >
            <Trash2 size={14} />
          </button>
        </div>
      ))}
    </div>
  );
}

function CreateContainerForm({
  onSubmit,
  onCancel,
  isLoading,
}: {
  onSubmit: (name: string, description?: string) => void;
  onCancel: () => void;
  isLoading: boolean;
}) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  return (
    <div style={{ border: '1px solid var(--border)', padding: '1rem', borderRadius: '0.5rem', marginBottom: '1rem' }}>
      <h4 style={{ marginBottom: '1rem' }}>Create New Container (Folder)</h4>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          onSubmit(name, description);
        }}
      >
        <div className="form-group">
          <label className="label">Name *</label>
          <input
            className="input"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            placeholder="e.g., My Documents"
          />
        </div>
        <div className="form-group">
          <label className="label">Description</label>
          <input
            className="input"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Optional description"
          />
        </div>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button type="submit" className="button button-primary" disabled={isLoading}>
            {isLoading ? 'Creating...' : 'Create'}
          </button>
          <button type="button" className="button button-secondary" onClick={onCancel}>
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
