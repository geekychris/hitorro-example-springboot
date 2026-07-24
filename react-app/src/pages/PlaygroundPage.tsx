import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import ReactJson from '@microlink/react-json-view';
import {
  Sparkles,
  Play,
  RefreshCw,
  ShieldAlert,
  CheckSquare,
  Fingerprint,
  Link2,
  Languages,
  Binary,
  FileCheck,
  Wand2,
  Blocks,
} from 'lucide-react';
import {
  playgroundApi,
  PlaygroundProjectionResponse,
} from '../services/api';

type SectionId =
  | 'redact'
  | 'validate-projection'
  | 'fingerprint'
  | 'materialize'
  | 'i18n'
  | 'vectorize'
  | 'validate-doc'
  | 'detect-lang'
  | 'lemmatize'
  | 'dsl';

interface Section {
  id: SectionId;
  label: string;
  blurb: string;
  Icon: any;
}

const SECTIONS: Section[] = [
  { id: 'redact', label: 'Redact', blurb: 'mask / hash / hmac / null modes for PII fields', Icon: ShieldAlert },
  { id: 'validate-projection', label: 'Validate (projection)', blurb: 'group-scoped constraint check', Icon: CheckSquare },
  { id: 'fingerprint', label: 'Fingerprint', blurb: 'stable SHA-256 over projected fields', Icon: Fingerprint },
  { id: 'materialize', label: 'Materialize', blurb: 'dereference IDs via DocumentStore', Icon: Link2 },
  { id: 'i18n', label: 'i18n', blurb: 'flatten MLS envelopes per language', Icon: Languages },
  { id: 'vectorize', label: 'Vectorize', blurb: 'embed text via HashingEmbeddingProvider', Icon: Binary },
  { id: 'validate-doc', label: 'JVSValidator', blurb: 'whole-document validation with FieldConstraints', Icon: FileCheck },
  { id: 'detect-lang', label: 'Detect Language', blurb: 'OpenNLP + n-gram ISO-639-1 detection', Icon: Languages },
  { id: 'lemmatize', label: 'Lemmatize', blurb: 'OpenNLP LemmatizerModel', Icon: Wand2 },
  { id: 'dsl', label: 'DSL Fill-Ins', blurb: 'deleteIf / mapArray / ifMissing / coalesce', Icon: Blocks },
];

const BEFORE_AFTER_STYLE: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: '1fr 1fr',
  gap: '12px',
};

const PANEL_STYLE: React.CSSProperties = {
  border: '1px solid #e5e7eb',
  borderRadius: '6px',
  padding: '12px',
  background: '#fafafa',
  minHeight: '220px',
  overflow: 'auto',
};

const PRIMARY_BTN: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: '6px',
  padding: '6px 14px',
  background: '#2563eb',
  color: 'white',
  border: 'none',
  borderRadius: '4px',
  cursor: 'pointer',
};

export default function PlaygroundPage() {
  const [activeSection, setActiveSection] = useState<SectionId>('redact');
  const [docText, setDocText] = useState('');
  const [typeText, setTypeText] = useState('');
  const [refsText, setRefsText] = useState('');

  const { data: samples } = useQuery({
    queryKey: ['playground-samples'],
    queryFn: () => playgroundApi.samples().then(r => r.data),
  });

  useEffect(() => {
    if (samples && !docText) {
      setDocText(JSON.stringify(samples.document, null, 2));
      setTypeText(JSON.stringify(samples.typeDefinition, null, 2));
      setRefsText(JSON.stringify(samples.references, null, 2));
    }
  }, [samples]);

  const parsedDoc = useMemo(() => tryParse(docText), [docText]);
  const parsedType = useMemo(() => tryParse(typeText), [typeText]);
  const parsedRefs = useMemo(() => tryParse(refsText), [refsText]);

  const active = SECTIONS.find(s => s.id === activeSection)!;

  return (
    <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        <Sparkles size={22} />
        <h2 style={{ margin: 0 }}>JVS Playground</h2>
        <span style={{ color: '#666', fontSize: '14px' }}>
          Interactive showcase for the new projection / validation / NLP features
        </span>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '260px 1fr', gap: '12px' }}>
        {/* Left rail — feature picker */}
        <nav style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
          {SECTIONS.map(s => {
            const Icon = s.Icon;
            const activeStyle = s.id === activeSection ? {
              background: '#eef2ff',
              borderColor: '#c7d2fe',
              color: '#1e3a8a',
            } : { background: 'white', borderColor: '#e5e7eb' };
            return (
              <button
                key={s.id}
                onClick={() => setActiveSection(s.id)}
                style={{
                  textAlign: 'left',
                  padding: '8px 10px',
                  border: '1px solid',
                  borderRadius: '4px',
                  cursor: 'pointer',
                  ...activeStyle,
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontWeight: 500 }}>
                  <Icon size={14} /> {s.label}
                </div>
                <div style={{ fontSize: '11px', color: '#666', marginTop: '2px' }}>{s.blurb}</div>
              </button>
            );
          })}
        </nav>

        {/* Right pane */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <active.Icon size={18} /> <h3 style={{ margin: 0 }}>{active.label}</h3>
            <span style={{ color: '#666', fontSize: '13px' }}>— {active.blurb}</span>
          </div>

          {/* Document / type editors — shared across most sections */}
          {NEEDS_DOC_EDITORS.has(activeSection) && (
            <details open>
              <summary style={{ cursor: 'pointer', fontWeight: 500 }}>
                Input document & type definition (edit to try your own)
              </summary>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', marginTop: '8px' }}>
                <div>
                  <label style={{ fontSize: '12px', color: '#666' }}>Document (JVS)</label>
                  <textarea
                    value={docText}
                    onChange={e => setDocText(e.target.value)}
                    rows={10}
                    style={{ width: '100%', fontFamily: 'monospace', fontSize: '12px' }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: '12px', color: '#666' }}>Type definition (optional override)</label>
                  <textarea
                    value={typeText}
                    onChange={e => setTypeText(e.target.value)}
                    rows={10}
                    style={{ width: '100%', fontFamily: 'monospace', fontSize: '12px' }}
                  />
                </div>
              </div>
            </details>
          )}

          {activeSection === 'redact' && (
            <RedactSection doc={parsedDoc} type={parsedType} />
          )}
          {activeSection === 'validate-projection' && (
            <ValidateProjectionSection doc={parsedDoc} type={parsedType} />
          )}
          {activeSection === 'fingerprint' && (
            <FingerprintSection doc={parsedDoc} type={parsedType} />
          )}
          {activeSection === 'materialize' && (
            <MaterializeSection
              doc={parsedDoc}
              type={parsedType}
              refs={parsedRefs}
              refsText={refsText}
              onRefsChange={setRefsText}
            />
          )}
          {activeSection === 'i18n' && (
            <I18nSection doc={parsedDoc} type={parsedType} />
          )}
          {activeSection === 'vectorize' && (
            <VectorizeSection doc={parsedDoc} type={parsedType} />
          )}
          {activeSection === 'validate-doc' && (
            <ValidateDocSection doc={parsedDoc} type={parsedType} />
          )}
          {activeSection === 'detect-lang' && <DetectLangSection />}
          {activeSection === 'lemmatize' && <LemmatizeSection />}
          {activeSection === 'dsl' && <DslSection />}
        </div>
      </div>
    </div>
  );
}

const NEEDS_DOC_EDITORS = new Set<SectionId>([
  'redact', 'validate-projection', 'fingerprint', 'materialize',
  'i18n', 'vectorize', 'validate-doc',
]);

function tryParse(text: string): any | null {
  try { return JSON.parse(text); } catch { return null; }
}

// ---------------------------------------------------------------------------------------
// Section components
// ---------------------------------------------------------------------------------------

function BeforeAfter({ result }: { result: PlaygroundProjectionResponse | undefined }) {
  if (!result) return null;
  return (
    <div style={BEFORE_AFTER_STYLE}>
      <div style={PANEL_STYLE}>
        <div style={{ fontSize: '12px', color: '#666', marginBottom: '6px' }}>source (after projection)</div>
        {result.source && (
          <ReactJson src={result.source} name={false} collapsed={2} displayDataTypes={false} />
        )}
      </div>
      <div style={PANEL_STYLE}>
        <div style={{ fontSize: '12px', color: '#666', marginBottom: '6px' }}>target</div>
        {result.target && (
          <ReactJson src={result.target} name={false} collapsed={2} displayDataTypes={false} />
        )}
      </div>
    </div>
  );
}

function ErrorBanner({ msg }: { msg?: string }) {
  if (!msg) return null;
  return (
    <div style={{ background: '#fef2f2', color: '#991b1b', padding: '8px 12px',
                  borderRadius: '4px', border: '1px solid #fecaca' }}>
      {msg}
    </div>
  );
}

function RedactSection({ doc, type }: any) {
  const [redactionKey, setRedactionKey] = useState('tenant-A-secret');
  const mut = useMutation({
    mutationFn: () => playgroundApi.projectionRedact({
      document: doc, typeDefinition: type, redactionKey,
    }).then(r => r.data),
  });
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
      <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
        <label style={{ fontSize: '13px' }}>hmac key:</label>
        <input value={redactionKey} onChange={e => setRedactionKey(e.target.value)}
               style={{ padding: '4px 8px', fontFamily: 'monospace' }} />
        <button style={PRIMARY_BTN} onClick={() => mut.mutate()} disabled={!doc || mut.isPending}>
          {mut.isPending ? <RefreshCw size={14} className="spin" /> : <Play size={14} />} Run redact
        </button>
      </div>
      <ErrorBanner msg={mut.data?.error} />
      <BeforeAfter result={mut.data} />
      <p style={{ fontSize: '12px', color: '#666' }}>
        The playground type marks <code>author</code> with <code>method: "hmac"</code> (needs the key above) and
        <code> checksum</code> with <code>method: "hash"</code>. Both modes preserve the KEY at the destination so
        downstream consumers can prove the field existed with the value scrubbed.
      </p>
    </div>
  );
}

function ValidateProjectionSection({ doc, type }: any) {
  const mut = useMutation({
    mutationFn: () => playgroundApi.projectionValidate({
      document: doc, typeDefinition: type,
    }).then(r => r.data),
  });
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
      <button style={PRIMARY_BTN} onClick={() => mut.mutate()} disabled={!doc || mut.isPending}>
        {mut.isPending ? <RefreshCw size={14} className="spin" /> : <Play size={14} />} Run validate projection
      </button>
      <ErrorBanner msg={mut.data?.error} />
      {mut.data?.violations && (
        <div style={PANEL_STYLE}>
          <div style={{ fontSize: '12px', color: '#666', marginBottom: '6px' }}>
            violations ({mut.data.violations.length})
          </div>
          {mut.data.violations.length === 0 ? (
            <div style={{ color: '#059669' }}>No violations — all fields in the <code>validate</code> group pass their constraints.</div>
          ) : (
            <ul style={{ margin: 0, paddingLeft: '18px' }}>
              {mut.data.violations.map((v, i) => (
                <li key={i}><b>[{v.level}]</b> <code>{v.path}</code>: {v.message}</li>
              ))}
            </ul>
          )}
        </div>
      )}
      <p style={{ fontSize: '12px', color: '#666' }}>
        This projection walks fields tagged with the <code>validate</code> group and enforces their
        JSON-Schema-style constraints (<code>minLength</code>, <code>maximum</code>, <code>pattern</code>,
        <code>enum</code>, <code>format</code>, …). Tweak the document above (bad email, out-of-range size,
        non-enum classification) to see failures.
      </p>
    </div>
  );
}

function FingerprintSection({ doc, type }: any) {
  const mut = useMutation({
    mutationFn: () => playgroundApi.projectionFingerprint({
      document: doc, typeDefinition: type,
    }).then(r => r.data),
  });
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
      <button style={PRIMARY_BTN} onClick={() => mut.mutate()} disabled={!doc || mut.isPending}>
        {mut.isPending ? <RefreshCw size={14} className="spin" /> : <Play size={14} />} Run fingerprint
      </button>
      <ErrorBanner msg={mut.data?.error} />
      {mut.data?.digest && (
        <div style={PANEL_STYLE}>
          <div style={{ fontSize: '12px', color: '#666', marginBottom: '6px' }}>SHA-256 digest</div>
          <code style={{ wordBreak: 'break-all', fontSize: '13px' }}>{mut.data.digest}</code>
        </div>
      )}
      <p style={{ fontSize: '12px', color: '#666' }}>
        The fingerprint projection produces a stable hash over the fields tagged with the <code>fingerprint</code>
        group (<code>filename</code>, <code>file_size</code>, <code>version</code>). Change any of those values
        and the digest changes; leave them alone and it stays the same across runs.
      </p>
    </div>
  );
}

function MaterializeSection({ doc, type, refs, refsText, onRefsChange }: any) {
  const mut = useMutation({
    mutationFn: () => playgroundApi.projectionMaterialize({
      document: doc, typeDefinition: type, references: refs,
    }).then(r => r.data),
  });
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
      <div>
        <label style={{ fontSize: '12px', color: '#666' }}>
          Reference store — id → document (the resolver used by MaterializeAction)
        </label>
        <textarea value={refsText} onChange={e => onRefsChange(e.target.value)} rows={5}
                  style={{ width: '100%', fontFamily: 'monospace', fontSize: '12px' }} />
      </div>
      <button style={PRIMARY_BTN} onClick={() => mut.mutate()} disabled={!doc || mut.isPending}>
        {mut.isPending ? <RefreshCw size={14} className="spin" /> : <Play size={14} />} Run materialize
      </button>
      <ErrorBanner msg={mut.data?.error} />
      <BeforeAfter result={mut.data} />
      <p style={{ fontSize: '12px', color: '#666' }}>
        The <code>author</code> string in the document is treated as a foreign-key ID. When it matches
        a key in the reference store above, the field is replaced with the referenced document.
        Unresolved references are left untouched — never silently dropped.
      </p>
    </div>
  );
}

function I18nSection({ doc, type }: any) {
  const [lang, setLang] = useState('fr');
  const mut = useMutation({
    mutationFn: () => playgroundApi.projectionI18n({
      document: doc, typeDefinition: type, lang,
    }).then(r => r.data),
  });
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
      <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
        <label style={{ fontSize: '13px' }}>Target lang:</label>
        <select value={lang} onChange={e => setLang(e.target.value)}
                style={{ padding: '4px 8px' }}>
          <option value="en">en</option>
          <option value="fr">fr</option>
          <option value="de">de (falls back)</option>
          <option value="ja">ja (falls back)</option>
        </select>
        <button style={PRIMARY_BTN} onClick={() => mut.mutate()} disabled={!doc || mut.isPending}>
          {mut.isPending ? <RefreshCw size={14} className="spin" /> : <Play size={14} />} Run i18n
        </button>
      </div>
      <ErrorBanner msg={mut.data?.error} />
      <BeforeAfter result={mut.data} />
      <p style={{ fontSize: '12px', color: '#666' }}>
        Flattens the MLS envelope at <code>content</code> to a single scalar for the requested language.
        Fallback order: requested → en → first entry. Source is preserved so you can re-project into another
        language.
      </p>
    </div>
  );
}

function VectorizeSection({ doc, type }: any) {
  const [dims, setDims] = useState(64);
  const [lang, setLang] = useState('en');
  const mut = useMutation({
    mutationFn: () => playgroundApi.projectionVectorize({
      document: doc, typeDefinition: type, dimensions: dims, lang,
    }).then(r => r.data),
  });
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
      <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
        <label style={{ fontSize: '13px' }}>Dimensions:</label>
        <input type="number" min={4} max={512} value={dims}
               onChange={e => setDims(parseInt(e.target.value) || 64)}
               style={{ padding: '4px 8px', width: '80px' }} />
        <label style={{ fontSize: '13px' }}>Lang:</label>
        <select value={lang} onChange={e => setLang(e.target.value)}
                style={{ padding: '4px 8px' }}>
          <option value="en">en</option>
          <option value="fr">fr</option>
        </select>
        <button style={PRIMARY_BTN} onClick={() => mut.mutate()} disabled={!doc || mut.isPending}>
          {mut.isPending ? <RefreshCw size={14} className="spin" /> : <Play size={14} />} Run vectorize
        </button>
      </div>
      <ErrorBanner msg={mut.data?.error} />
      <BeforeAfter result={mut.data} />
      <p style={{ fontSize: '12px', color: '#666' }}>
        Runs the <code>content</code> MLS through <code>HashingEmbeddingProvider</code> (deterministic,
        no ML dependency) and writes an L2-normalised float vector to <code>content_vector</code> on the target.
        Swap the provider for an ONNX-backed one in production.
      </p>
    </div>
  );
}

function ValidateDocSection({ doc, type }: any) {
  const mut = useMutation({
    mutationFn: () => playgroundApi.validateDocument({
      document: doc, typeDefinition: type,
    }).then(r => r.data),
  });
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
      <button style={PRIMARY_BTN} onClick={() => mut.mutate()} disabled={!doc || mut.isPending}>
        {mut.isPending ? <RefreshCw size={14} className="spin" /> : <Play size={14} />} Run JVSValidator
      </button>
      <ErrorBanner msg={mut.data?.error} />
      {mut.data && (
        <div style={PANEL_STYLE}>
          <div style={{ fontSize: '12px', color: '#666', marginBottom: '6px' }}>
            valid: <b style={{ color: mut.data.valid ? '#059669' : '#dc2626' }}>{String(mut.data.valid)}</b>
          </div>
          <pre style={{ margin: 0, whiteSpace: 'pre-wrap', fontSize: '12px' }}>{mut.data.report}</pre>
        </div>
      )}
      <p style={{ fontSize: '12px', color: '#666' }}>
        Whole-document validation — runs <code>JVSValidator</code> against the type definition, applying
        <code>FieldConstraints</code> (minLength/maximum/pattern/enum/format) plus the type-level checks for
        missing/extra fields.
      </p>
    </div>
  );
}

function DetectLangSection() {
  const [text, setText] = useState(
    'The quick brown fox jumps over the lazy dog. This is a longer paragraph so that the language detector has enough signal to make an accurate call.'
  );
  const mut = useMutation({
    mutationFn: () => playgroundApi.detectLang({ text }).then(r => r.data),
  });
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
      <textarea value={text} onChange={e => setText(e.target.value)} rows={6}
                style={{ width: '100%', fontFamily: 'monospace', fontSize: '13px' }} />
      <button style={PRIMARY_BTN} onClick={() => mut.mutate()} disabled={mut.isPending}>
        {mut.isPending ? <RefreshCw size={14} className="spin" /> : <Play size={14} />} Detect
      </button>
      {mut.data && (
        <div style={PANEL_STYLE}>
          <div><b>Language:</b> <code>{mut.data.language ?? 'null'}</code></div>
          <div><b>Detector:</b> {mut.data.detector}</div>
          {mut.data.confidence !== undefined && mut.data.confidence !== null && (
            <div><b>Confidence:</b> {mut.data.confidence.toFixed(4)}</div>
          )}
          {mut.data.note && (
            <div style={{ marginTop: '6px', color: '#92400e', fontSize: '12px' }}>ℹ {mut.data.note}</div>
          )}
        </div>
      )}
    </div>
  );
}

function LemmatizeSection() {
  const [lang, setLang] = useState('en');
  const [tokens, setTokens] = useState('The dogs were running quickly');
  const [tags, setTags] = useState('DT NNS VBD VBG RB');
  const mut = useMutation({
    mutationFn: () => playgroundApi.lemmatize({
      lang,
      tokens: tokens.split(/\s+/).filter(Boolean),
      posTags: tags.split(/\s+/).filter(Boolean),
    }).then(r => r.data),
  });
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
      <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
        <label>Lang:</label>
        <input value={lang} onChange={e => setLang(e.target.value)}
               style={{ width: '60px', padding: '4px 8px' }} />
      </div>
      <label style={{ fontSize: '12px', color: '#666' }}>Tokens (space-separated)</label>
      <input value={tokens} onChange={e => setTokens(e.target.value)}
             style={{ padding: '4px 8px', fontFamily: 'monospace' }} />
      <label style={{ fontSize: '12px', color: '#666' }}>POS tags (Penn Treebank, same count)</label>
      <input value={tags} onChange={e => setTags(e.target.value)}
             style={{ padding: '4px 8px', fontFamily: 'monospace' }} />
      <button style={PRIMARY_BTN} onClick={() => mut.mutate()} disabled={mut.isPending}>
        {mut.isPending ? <RefreshCw size={14} className="spin" /> : <Play size={14} />} Lemmatize
      </button>
      {mut.data && (
        <div style={PANEL_STYLE}>
          {mut.data.lemmas && (
            <div><b>Lemmas:</b> <code>[{mut.data.lemmas.map(l => `"${l}"`).join(', ')}]</code></div>
          )}
          {mut.data.error && <ErrorBanner msg={mut.data.error} />}
          {mut.data.note && (
            <div style={{ marginTop: '6px', color: '#92400e', fontSize: '12px' }}>ℹ {mut.data.note}</div>
          )}
        </div>
      )}
      <p style={{ fontSize: '12px', color: '#666' }}>
        Requires an OpenNLP <code>{`{lang}`}-lemmatizer.bin</code> file under the configured model directory.
        When the model is absent, the endpoint reports it and the mapper degrades gracefully to null
        (callers typically fall back to Snowball stemming).
      </p>
    </div>
  );
}

function DslSection() {
  const [script, setScript] = useState('');
  const [input, setInput] = useState('');
  const [loadedDefaults, setLoadedDefaults] = useState(false);

  // Prime editors with the canned defaults on first render by calling the endpoint with no body.
  useEffect(() => {
    if (!loadedDefaults) {
      playgroundApi.dslTransform({}).then(r => {
        setInput(JSON.stringify(r.data.input, null, 2));
        setLoadedDefaults(true);
      }).catch(() => setLoadedDefaults(true));
    }
  }, [loadedDefaults]);

  useEffect(() => {
    if (!script) {
      setScript(DEFAULT_DSL_SCRIPT_UI);
    }
  }, []);

  const mut = useMutation({
    mutationFn: () => playgroundApi.dslTransform({
      script,
      input: tryParse(input),
    }).then(r => r.data),
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
      <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1fr', gap: '8px' }}>
        <div>
          <label style={{ fontSize: '12px', color: '#666' }}>Groovy script</label>
          <textarea value={script} onChange={e => setScript(e.target.value)} rows={14}
                    style={{ width: '100%', fontFamily: 'monospace', fontSize: '12px' }} />
        </div>
        <div>
          <label style={{ fontSize: '12px', color: '#666' }}>Input document</label>
          <textarea value={input} onChange={e => setInput(e.target.value)} rows={14}
                    style={{ width: '100%', fontFamily: 'monospace', fontSize: '12px' }} />
        </div>
      </div>
      <button style={PRIMARY_BTN} onClick={() => mut.mutate()} disabled={mut.isPending}>
        {mut.isPending ? <RefreshCw size={14} className="spin" /> : <Play size={14} />} Run transform
      </button>
      <ErrorBanner msg={mut.data?.error} />
      {mut.data?.output && (
        <div style={PANEL_STYLE}>
          <div style={{ fontSize: '12px', color: '#666', marginBottom: '6px' }}>output</div>
          <ReactJson src={mut.data.output} name={false} collapsed={2} displayDataTypes={false} />
        </div>
      )}
    </div>
  );
}

const DEFAULT_DSL_SCRIPT_UI = `// DSL fill-ins showcase
copyAll()

// coalesce — first non-null across candidate paths
set "target.display_name", coalesce("source.nickname", "source.name", "source.email")

// ifMissing — default only when currently absent
ifMissing "target.status", "draft"
ifMissing "target.priority", 3

// mapArray — element-wise transform
mapArray("target.tags") { tag -> tag.textValue().toLowerCase() }

// deleteIf — conditional removal
deleteIf("target.internal_note") { it?.textValue() == "REMOVE" }
`;
