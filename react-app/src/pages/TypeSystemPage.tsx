import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Sparkles, Code, Search, ChevronRight, ChevronDown, Tag, Database, Layers, Languages, Globe } from 'lucide-react';
import ReactJson from '@microlink/react-json-view';
import { typeSystemApi } from '../services/api';
import type { JVSEnrichRequest, TypeField, TranslateRequest } from '../types/api';

// Supported languages for translation
const SUPPORTED_LANGUAGES: Record<string, string> = {
  en: 'English',
  de: 'German',
  es: 'Spanish',
  fr: 'French',
  it: 'Italian',
  pt: 'Portuguese',
  ja: 'Japanese',
  zh: 'Chinese (Simplified)',
  ko: 'Korean',
  ar: 'Arabic',
  ru: 'Russian',
  nl: 'Dutch',
  pl: 'Polish',
  sv: 'Swedish',
};

export default function TypeSystemPage() {
  const [selectedTags, setSelectedTags] = useState<string[]>(['ner', 'segmented']);
  const [jsonInput, setJsonInput] = useState(`{
  "id": {
    "domain": "sysobject",
    "did": "1234"
  },
  "type": "core_sysobject",
  "dates": {
    "created": "2018-04-23T18:25:43Z",
    "modified": "2018-04-23T18:25:43Z"
  },
  "title": {
    "mls": [
      {
        "lang": "en",
        "text": "The quick brown fox jumped over the lazy dogs."
      },
      {
        "lang": "de",
        "text": "Der schnelle braune Fuchs sprang über die faulen Hunde."
      }
    ]
  }
}`);
  const [enrichedData, setEnrichedData] = useState<any>(null);
  
  // Translation state
  const [translatedData, setTranslatedData] = useState<any>(null);
  const [sourceLanguage, setSourceLanguage] = useState('en');
  const [targetLanguages, setTargetLanguages] = useState<string[]>(['de', 'es', 'ja', 'zh']);
  const [mlsFields, setMlsFields] = useState('title');

  const enrichMutation = useMutation({
    mutationFn: (request: JVSEnrichRequest) => typeSystemApi.enrichJVS(request),
    onSuccess: (response) => {
      setEnrichedData(response.data);
    },
  });
  
  const translateMutation = useMutation({
    mutationFn: (request: TranslateRequest) => typeSystemApi.translateJVS(request),
    onSuccess: (response) => {
      setTranslatedData(response.data);
    },
  });

  const { data: typesList } = useQuery({
    queryKey: ['types'],
    queryFn: () => typeSystemApi.listTypes().then(res => res.data),
  });

  const handleEnrich = () => {
    try {
      // Validate JSON
      JSON.parse(jsonInput);
      const tags = selectedTags.length > 0 ? selectedTags.join(',') : undefined;
      enrichMutation.mutate({ json: jsonInput, tags });
    } catch (e) {
      alert('Invalid JSON: ' + (e as Error).message);
    }
  };

  const toggleTag = (tag: string) => {
    setSelectedTags(prev =>
      prev.includes(tag)
        ? prev.filter(t => t !== tag)
        : [...prev, tag]
    );
  };
  
  const toggleTargetLanguage = (lang: string) => {
    setTargetLanguages(prev =>
      prev.includes(lang)
        ? prev.filter(l => l !== lang)
        : [...prev, lang]
    );
  };
  
  const handleTranslate = () => {
    try {
      JSON.parse(jsonInput);
      const fields = mlsFields.split(',').map(f => f.trim()).filter(f => f);
      if (fields.length === 0) {
        alert('Please specify at least one MLS field');
        return;
      }
      if (targetLanguages.length === 0) {
        alert('Please select at least one target language');
        return;
      }
      translateMutation.mutate({
        json: jsonInput,
        mlsFields: fields,
        sourceLanguage,
        targetLanguages: targetLanguages.filter(l => l !== sourceLanguage),
      });
    } catch (e) {
      alert('Invalid JSON: ' + (e as Error).message);
    }
  };

  return (
    <div>
      <div className="card">
        <div className="card-header">
          <span>
            <Code size={20} style={{ marginRight: '0.5rem', display: 'inline' }} />
            JSON Type System (JVS)
          </span>
        </div>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
          Enrich JSON objects using the Hitorro <strong>JSON Type System (JVS)</strong>. The JVS2JVSEnrichMapper will apply
          type definitions, add computed fields, and expand the object based on its type. This uses the type system 
          from <code>hitorro-util</code> (not DMS types).
        </p>

        <div className="grid grid-2" style={{ marginBottom: '1.5rem' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
              <h4>Input JSON</h4>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button
                  className="button button-secondary"
                  onClick={() => setJsonInput(`{
  "id": {
    "domain": "sysobject",
    "did": "1234"
  },
  "type": "core_sysobject",
  "dates": {
    "created": "2018-04-23T18:25:43Z",
    "modified": "2018-04-23T18:25:43Z"
  },
  "title": {
    "mls": [
      {
        "lang": "en",
        "text": "The quick brown fox jumped over the lazy dogs."
      }
    ]
  }
}`)}
                  style={{ fontSize: '0.75rem', padding: '0.25rem 0.5rem' }}
                >
                  Load Example
                </button>
                <button
                  className="button button-secondary"
                  onClick={() => setJsonInput('{}')}
                  style={{ fontSize: '0.75rem', padding: '0.25rem 0.5rem' }}
                >
                  Clear
                </button>
              </div>
            </div>
            <textarea
              className="textarea"
              value={jsonInput}
              onChange={(e) => setJsonInput(e.target.value)}
              style={{ minHeight: '300px', fontFamily: 'monospace', fontSize: '0.875rem' }}
              placeholder='{"type": "core_sysobject", ...}'
            />
            
            {/* Tag Selection */}
            <div style={{ marginTop: '1rem', padding: '0.75rem', background: 'var(--background)', borderRadius: '0.375rem' }}>
              <strong style={{ display: 'block', marginBottom: '0.5rem', fontSize: '0.875rem' }}>Enrichment Tags:</strong>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                {['segmented', 'ner', 'answers', 'parsed', 'pos', 'hash'].map(tag => (
                  <label key={tag} style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={selectedTags.includes(tag)}
                      onChange={() => toggleTag(tag)}
                      style={{ marginRight: '0.25rem' }}
                    />
                    <span style={{ fontSize: '0.875rem' }}>{tag}</span>
                  </label>
                ))}
              </div>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.5rem', marginBottom: 0 }}>
                <strong>segmented_ner</strong> requires both <strong>segmented</strong> and <strong>ner</strong> tags
              </p>
            </div>

            <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.75rem' }}>
              <button
                className="button button-primary"
                onClick={handleEnrich}
                disabled={enrichMutation.isPending}
                style={{ flex: 1 }}
              >
                <Sparkles size={16} />
                {enrichMutation.isPending ? 'Enriching...' : 'Enrich'}{selectedTags.length > 0 && ` (${selectedTags.join(', ')})`}
              </button>
            </div>
            
            {/* Translation Section */}
            <div style={{ marginTop: '1rem', padding: '0.75rem', background: '#e0f2fe', borderRadius: '0.375rem', border: '1px solid #0ea5e9' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
                <Languages size={18} style={{ color: '#0284c7' }} />
                <strong style={{ fontSize: '0.875rem', color: '#0284c7' }}>MLS Translation (AI)</strong>
              </div>
              
              {/* MLS Fields Input */}
              <div style={{ marginBottom: '0.75rem' }}>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                  MLS Fields (comma-separated):
                </label>
                <input
                  type="text"
                  value={mlsFields}
                  onChange={(e) => setMlsFields(e.target.value)}
                  placeholder="title, description"
                  style={{
                    width: '100%',
                    padding: '0.5rem',
                    fontSize: '0.875rem',
                    border: '1px solid #0ea5e9',
                    borderRadius: '0.25rem',
                    fontFamily: 'monospace',
                  }}
                />
              </div>
              
              {/* Source Language */}
              <div style={{ marginBottom: '0.75rem' }}>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                  Source Language:
                </label>
                <select
                  value={sourceLanguage}
                  onChange={(e) => setSourceLanguage(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '0.5rem',
                    fontSize: '0.875rem',
                    border: '1px solid #0ea5e9',
                    borderRadius: '0.25rem',
                  }}
                >
                  {Object.entries(SUPPORTED_LANGUAGES).map(([code, name]) => (
                    <option key={code} value={code}>{name} ({code})</option>
                  ))}
                </select>
              </div>
              
              {/* Target Languages */}
              <div style={{ marginBottom: '0.75rem' }}>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                  Target Languages:
                </label>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                  {Object.entries(SUPPORTED_LANGUAGES).map(([code, name]) => (
                    code !== sourceLanguage && (
                      <label key={code} style={{ display: 'flex', alignItems: 'center', cursor: 'pointer', fontSize: '0.8rem' }}>
                        <input
                          type="checkbox"
                          checked={targetLanguages.includes(code)}
                          onChange={() => toggleTargetLanguage(code)}
                          style={{ marginRight: '0.25rem' }}
                        />
                        <span>{name}</span>
                      </label>
                    )
                  ))}
                </div>
              </div>
              
              <button
                className="button"
                onClick={handleTranslate}
                disabled={translateMutation.isPending}
                style={{ 
                  width: '100%', 
                  background: '#0284c7', 
                  color: 'white',
                  border: 'none',
                  padding: '0.5rem 1rem',
                  borderRadius: '0.375rem',
                  cursor: translateMutation.isPending ? 'wait' : 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '0.5rem',
                }}
              >
                <Globe size={16} />
                {translateMutation.isPending ? 'Translating...' : `Translate to ${targetLanguages.filter(l => l !== sourceLanguage).length} languages`}
              </button>
              
              {translateMutation.isPending && (
                <p style={{ fontSize: '0.75rem', color: '#0369a1', marginTop: '0.5rem', marginBottom: 0 }}>
                  ⏳ Translation may take 10-30 seconds depending on text length and number of languages...
                </p>
              )}
            </div>
          </div>

          <div>
            <h4 style={{ marginBottom: '0.5rem' }}>
              {translatedData ? 'Translated Result' : 'Enriched Result'}
            </h4>
            <div
              style={{
                minHeight: '300px',
                background: 'var(--background)',
                padding: '1rem',
                borderRadius: '0.375rem',
                overflow: 'auto',
              }}
            >
              {translatedData ? (
                <ReactJson
                  src={translatedData.translated || translatedData.original}
                  theme="rjv-default"
                  collapsed={2}
                  displayDataTypes={false}
                  displayObjectSize={false}
                  enableClipboard={true}
                  name={null}
                />
              ) : enrichedData ? (
                <ReactJson
                  src={enrichedData.enriched || enrichedData.original}
                  theme="rjv-default"
                  collapsed={2}
                  displayDataTypes={false}
                  displayObjectSize={false}
                  enableClipboard={true}
                  name={null}
                />
              ) : (
                <div style={{ color: 'var(--text-secondary)' }}>
                  Enriched or translated JSON will appear here...
                </div>
              )}
            </div>
            
            {/* Translation Details */}
            {translatedData && (
              <div style={{ marginTop: '1rem' }}>
                <div style={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  justifyContent: 'space-between',
                  marginBottom: '0.5rem' 
                }}>
                  <strong>Translation Details</strong>
                  <button
                    className="button button-secondary"
                    onClick={() => setTranslatedData(null)}
                    style={{ fontSize: '0.75rem', padding: '0.25rem 0.5rem' }}
                  >
                    Clear
                  </button>
                </div>
                
                <div style={{ fontSize: '0.875rem', background: '#f0fdf4', padding: '0.75rem', borderRadius: '0.375rem', border: '1px solid #22c55e' }}>
                  <p style={{ margin: '0 0 0.5rem 0' }}>
                    <strong>Status:</strong> {translatedData.success ? '✅ Success' : '❌ Failed'}
                  </p>
                  <p style={{ margin: '0 0 0.5rem 0' }}>
                    <strong>Source:</strong> {SUPPORTED_LANGUAGES[translatedData.sourceLanguage] || translatedData.sourceLanguage}
                  </p>
                  <p style={{ margin: '0 0 0.5rem 0' }}>
                    <strong>Targets:</strong> {translatedData.targetLanguages?.map((l: string) => SUPPORTED_LANGUAGES[l] || l).join(', ')}
                  </p>
                  <p style={{ margin: 0 }}>
                    <strong>Time:</strong> {translatedData.translationTimeMs}ms
                  </p>
                </div>
                
                {translatedData.fieldTranslations?.map((ft: any, idx: number) => (
                  <div key={idx} style={{ marginTop: '0.75rem', background: '#f8fafc', padding: '0.75rem', borderRadius: '0.375rem', border: '1px solid #e2e8f0' }}>
                    <strong style={{ fontSize: '0.875rem' }}>Field: {ft.fieldPath}</strong>
                    {ft.error ? (
                      <p style={{ color: '#dc2626', fontSize: '0.875rem', margin: '0.5rem 0 0 0' }}>Error: {ft.error}</p>
                    ) : (
                      <>
                        <p style={{ fontSize: '0.8rem', color: '#64748b', margin: '0.5rem 0' }}>
                          <em>Source:</em> {ft.sourceText?.substring(0, 100)}{ft.sourceText?.length > 100 ? '...' : ''}
                        </p>
                        {ft.translations && Object.entries(ft.translations).map(([lang, text]) => (
                          <div key={lang} style={{ fontSize: '0.8rem', marginTop: '0.25rem' }}>
                            <strong>{SUPPORTED_LANGUAGES[lang] || lang}:</strong> {(text as string)?.substring(0, 100)}{(text as string)?.length > 100 ? '...' : ''}
                          </div>
                        ))}
                      </>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {enrichMutation.error && (
          <div className="alert alert-error">
            Error: {(enrichMutation.error as any)?.response?.data?.message || (enrichMutation.error as Error).message}
          </div>
        )}
        
        {translateMutation.error && (
          <div className="alert alert-error">
            Translation Error: {(translateMutation.error as any)?.response?.data?.error || (translateMutation.error as Error).message}
          </div>
        )}

        {enrichedData && !translatedData && (
          <EnrichedDataDetails data={enrichedData} />
        )}
      </div>

      <div className="card">
        <div className="card-header">
          <span>
            <Search size={20} style={{ marginRight: '0.5rem', display: 'inline' }} />
            JSON Type System - Type Explorer
          </span>
        </div>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
          Explore available types in the <strong>JSON Type System</strong> (from <code>JsonTypeSystem.getMe()</code>).
          These are the types used by JVS objects, <strong>not</strong> DMS entity types. Types include things like 
          <code>core_sysobject</code>, <code>document</code>, custom content types, etc.
        </p>

        {typesList && typesList.length > 0 ? (
          <TypesList types={typesList} />
        ) : (
          <div style={{ color: 'var(--text-secondary)', padding: '2rem', textAlign: 'center' }}>
            <p><strong>Note:</strong> Type listing is currently simplified. The JsonTypeSystem may not expose all types via API.</p>
            <p style={{ marginTop: '0.5rem' }}>Types are loaded from <code>HT_BIN/types/</code> directory and registered in the JsonTypeSystem.</p>
          </div>
        )}
      </div>
    </div>
  );
}

function EnrichedDataDetails({ data }: { data: any }) {
  return (
    <div style={{ marginTop: '1.5rem' }}>
      <h4 style={{ marginBottom: '0.5rem' }}>Enrichment Details</h4>
      
      {data.typeName && (
        <div className="form-group">
          <strong>Type Name:</strong> <span className="badge badge-primary">{data.typeName}</span>
        </div>
      )}

      {data.fields && data.fields.length > 0 && (
        <div>
          <strong>Fields ({data.fields.length}):</strong>
          <div style={{ marginTop: '0.5rem', maxHeight: '300px', overflowY: 'auto' }}>
            <table className="table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Type</th>
                  <th>Path</th>
                  <th>Value</th>
                </tr>
              </thead>
              <tbody>
                {data.fields.map((field: any, idx: number) => (
                  <tr key={idx}>
                    <td>{field.name}</td>
                    <td><span className="badge badge-primary">{field.type}</span></td>
                    <td style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                      {field.path}
                    </td>
                    <td style={{ maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {JSON.stringify(field.value)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

function TypesList({ types }: { types: string[] }) {
  const [selectedType, setSelectedType] = useState<string | null>(null);

  const { data: typeDefinition } = useQuery({
    queryKey: ['type', selectedType],
    queryFn: () => selectedType ? typeSystemApi.getTypeDefinition(selectedType).then(res => res.data) : null,
    enabled: !!selectedType,
  });

  return (
    <div className="grid grid-2">
      <div>
        <div
          style={{
            maxHeight: '500px',
            overflowY: 'auto',
            border: '1px solid var(--border)',
            borderRadius: '0.375rem',
          }}
        >
          {types.map((type) => (
            <div
              key={type}
              onClick={() => {
                setSelectedType(type);
              }}
              style={{
                padding: '0.75rem',
                cursor: 'pointer',
                borderBottom: '1px solid var(--border)',
                background: selectedType === type ? 'var(--background)' : undefined,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
              }}
            >
              <span>{type}</span>
              <ChevronRight size={16} />
            </div>
          ))}
        </div>
      </div>

      <div>
        {typeDefinition ? (
          <div>
            <h4 style={{ marginBottom: '1rem' }}>{typeDefinition.name}</h4>
            
            {typeDefinition.baseType && (
              <div className="form-group">
                <strong>Base Type:</strong> <span className="badge badge-primary">{typeDefinition.baseType}</span>
              </div>
            )}

            {typeDefinition.extends && typeDefinition.extends.length > 0 && (
              <div className="form-group">
                <strong>Extends:</strong>
                <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem', flexWrap: 'wrap' }}>
                  {typeDefinition.extends.map((ext, idx) => (
                    <span key={idx} className="badge badge-primary">{ext}</span>
                  ))}
                </div>
              </div>
            )}

            {typeDefinition.fields && typeDefinition.fields.length > 0 ? (
              <div>
                <strong>Fields ({typeDefinition.fields.length}):</strong>
                <div style={{ marginTop: '0.5rem', maxHeight: '500px', overflowY: 'auto' }}>
                  {typeDefinition.fields.map((field, idx) => (
                    <FieldDisplay key={idx} field={field} depth={0} />
                  ))}
                </div>
              </div>
            ) : (
              <div style={{ padding: '1rem', background: 'var(--background)', borderRadius: '0.375rem', marginTop: '0.5rem' }}>
                <em style={{ color: 'var(--text-secondary)' }}>
                  {typeDefinition.name.includes('string') || typeDefinition.name.includes('long') || typeDefinition.name.includes('boolean') ? 
                    'Primitive type - no fields' : 
                    'No fields defined'}
                </em>
              </div>
            )}
          </div>
        ) : (
          <div style={{ color: 'var(--text-secondary)', padding: '2rem', textAlign: 'center' }}>
            Select a type to view its definition
          </div>
        )}
      </div>
    </div>
  );
}

/**
 * Hierarchical field display showing nested types, dynamics, groups, and tags
 */
function FieldDisplay({ field, depth = 0 }: { field: TypeField; depth?: number }) {
  const [expanded, setExpanded] = useState(depth < 2);
  const [showingDetails, setShowingDetails] = useState(true); // Show details by default!
  
  const { data: nestedType } = useQuery({
    queryKey: ['type', field.type],
    queryFn: () => field.type && !field.primitive ? 
      typeSystemApi.getTypeDefinition(field.type).then(res => res.data) : null,
    enabled: expanded && !field.primitive && !!field.type,
  });
  
  const hasNested = !field.primitive && field.type && field.type.indexOf('_') > 0;
  const indentSize = depth * 1.5;
  
  return (
    <div style={{ marginLeft: `${indentSize}rem`, marginBottom: '0.5rem' }}>
      <div
        style={{
          padding: '0.75rem',
          background: depth % 2 === 0 ? 'var(--background)' : '#f9fafb',
          borderRadius: '0.375rem',
          border: field.dynamic ? '2px solid #f59e0b' : field.primitive ? '1px solid #10b981' : '1px solid var(--border)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flex: 1 }}>
            {hasNested && (
              <button
                onClick={() => setExpanded(!expanded)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '0.25rem', display: 'flex', alignItems: 'center' }}
              >
                {expanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
              </button>
            )}
            <span style={{ fontWeight: 600, fontSize: '0.95rem' }}>{field.name}</span>
            
            <div style={{ display: 'flex', gap: '0.25rem', flexWrap: 'wrap' }}>
              {field.primitive && (
                <span className="badge" style={{ background: '#10b981', color: 'white', fontSize: '0.7rem' }}>
                  <Database size={10} style={{ marginRight: '0.2rem', display: 'inline' }} />
                  {field.primitiveType}
                </span>
              )}
              {field.vector && (
                <span className="badge" style={{ background: '#3b82f6', color: 'white', fontSize: '0.7rem' }}>
                  <Layers size={10} style={{ marginRight: '0.2rem', display: 'inline' }} />
                  vector
                </span>
              )}
              {field.i18n && <span className="badge" style={{ background: '#8b5cf6', color: 'white', fontSize: '0.7rem' }}>i18n</span>}
              {field.dynamic && <span className="badge" style={{ background: '#f59e0b', color: 'white', fontSize: '0.7rem' }}>⚡ dynamic</span>}
            </div>
          </div>
          
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span 
              className="badge badge-primary" 
              style={{ fontSize: '0.8rem', cursor: field.type && !field.primitive ? 'pointer' : 'default' }}
              title={field.type && !field.primitive ? 'Click to view type definition' : ''}
            >
              {field.type}
            </span>
            {(field.dynamic || (field.groups && field.groups.length > 0)) && (
              <button 
                onClick={() => setShowingDetails(!showingDetails)} 
                className="button button-secondary" 
                style={{ fontSize: '0.7rem', padding: '0.2rem 0.4rem' }}
              >
                {showingDetails ? '▼' : '▶'} {field.dynamic || field.groups ? 'Advanced' : 'Details'}
              </button>
            )}
          </div>
        </div>
        
        {showingDetails && field.dynamicInfo && (
          <div style={{ marginTop: '0.75rem', padding: '0.75rem', background: '#fef3c7', borderRadius: '0.375rem', fontSize: '0.85rem', border: '1px solid #fbbf24' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
              <span style={{ fontSize: '1.2rem' }}>⚡</span>
              <strong style={{ fontSize: '0.95rem' }}>Dynamic Field (Computed)</strong>
            </div>
            <div style={{ marginLeft: '1.5rem' }}>
              <div style={{ marginBottom: '0.5rem' }}>
                <strong>Implementation:</strong>
                <div style={{ fontSize: '0.75rem', fontFamily: 'monospace', marginTop: '0.25rem', padding: '0.35rem', background: 'white', borderRadius: '0.25rem', border: '1px solid #fbbf24' }}>
                  {field.dynamicInfo.className}
                </div>
                <div style={{ fontSize: '0.7rem', color: '#92400e', marginTop: '0.25rem', fontStyle: 'italic' }}>
                  Mapper: {field.dynamicInfo.className.split('.').pop()}
                </div>
              </div>
              {field.dynamicInfo.dependsOn && field.dynamicInfo.dependsOn.length > 0 && (
                <div>
                  <strong>Dependencies:</strong>
                  <div style={{ marginTop: '0.35rem', display: 'flex', gap: '0.35rem', flexWrap: 'wrap' }}>
                    {field.dynamicInfo.dependsOn.map(d => (
                      <code key={d} style={{ padding: '0.25rem 0.5rem', background: 'white', borderRadius: '0.25rem', border: '1px solid #fbbf24', fontSize: '0.75rem', fontWeight: 600 }}>
                        {d}
                      </code>
                    ))}
                  </div>
                  <div style={{ fontSize: '0.7rem', color: '#92400e', marginTop: '0.35rem', fontStyle: 'italic' }}>
                    ℹ️ This field is computed from the values of these fields
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
        
        {showingDetails && field.groups && field.groups.length > 0 && (
          <div style={{ marginTop: '0.75rem', padding: '0.75rem', background: '#ede9fe', borderRadius: '0.375rem', border: '1px solid #a78bfa' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
              <Tag size={16} style={{ display: 'inline' }} />
              <strong style={{ fontSize: '0.95rem' }}>Groups & Tags ({field.groups.length})</strong>
            </div>
            <div style={{ marginLeft: '1.5rem' }}>
              {field.groups.map((group, idx) => (
                <div key={idx} style={{ padding: '0.65rem', background: 'white', borderRadius: '0.375rem', border: '1px solid #a78bfa', fontSize: '0.85rem', marginBottom: '0.5rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: group.tags && group.tags.length > 0 ? '0.5rem' : '0' }}>
                    <strong style={{ fontSize: '0.9rem' }}>{group.name}</strong>
                    <span className="badge" style={{ background: '#6366f1', color: 'white', fontSize: '0.75rem' }}>
                      method: {group.method}
                    </span>
                  </div>
                  {group.tags && group.tags.length > 0 && (
                    <div>
                      <div style={{ fontSize: '0.75rem', color: '#6b21a8', marginBottom: '0.3rem' }}>
                        <strong>Tags:</strong>
                      </div>
                      <div style={{ display: 'flex', gap: '0.35rem', flexWrap: 'wrap' }}>
                        {group.tags.map((tag, i) => (
                          <span key={i} className="badge" style={{ background: '#ec4899', color: 'white', fontSize: '0.7rem', padding: '0.25rem 0.5rem' }}>
                            🏷️ {tag}
                          </span>
                        ))}
                      </div>
                      {group.name === 'enrich' && group.tags && group.tags.length > 0 && (
                        <div style={{ fontSize: '0.7rem', color: '#6b21a8', marginTop: '0.35rem', fontStyle: 'italic' }}>
                          ℹ️ Use JVS2JVSEnrichMapper with tags: <code style={{ background: '#f3e8ff', padding: '0.1rem 0.3rem', borderRadius: '0.2rem' }}>{group.tags.join(', ')}</code>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
      
      {expanded && nestedType && nestedType.fields && nestedType.fields.length > 0 && (
        <div style={{ marginTop: '0.5rem', paddingLeft: '1rem', borderLeft: '2px solid var(--border)' }}>
          {nestedType.fields.map((nestedField, idx) => (
            <FieldDisplay key={idx} field={nestedField} depth={depth + 1} />
          ))}
        </div>
      )}
    </div>
  );
}
