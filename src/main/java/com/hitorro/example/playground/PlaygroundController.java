/*
 * Copyright (c) 2006-2026 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hitorro.example.playground;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hitorro.jsontypesystem.BaseT;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.jsontypesystem.JVSValidator;
import com.hitorro.jsontypesystem.Type;
import com.hitorro.jsontypesystem.datamapper.DataGenerators;
import com.hitorro.jsontypesystem.datamapper.GroovyTransformMapper;
import com.hitorro.jsontypesystem.dynamic.LanguageDetectorMapper;
import com.hitorro.jsontypesystem.executors.ExecutionBuilder;
import com.hitorro.jsontypesystem.executors.FingerprintAction;
import com.hitorro.jsontypesystem.executors.FingerprintFactory;
import com.hitorro.jsontypesystem.executors.I18nAction;
import com.hitorro.jsontypesystem.executors.I18nFactory;
import com.hitorro.jsontypesystem.executors.MaterializeAction;
import com.hitorro.jsontypesystem.executors.MaterializeFactory;
import com.hitorro.jsontypesystem.executors.ProjectionContext;
import com.hitorro.jsontypesystem.executors.RedactAction;
import com.hitorro.jsontypesystem.executors.RedactFactory;
import com.hitorro.jsontypesystem.executors.ValidateAction;
import com.hitorro.jsontypesystem.executors.ValidateFactory;
import com.hitorro.jsontypesystem.executors.VectorizeAction;
import com.hitorro.jsontypesystem.executors.VectorizeFactory;
import com.hitorro.jsontypesystem.grouppredicates.GroupNameFilter;
import com.hitorro.jsontypesystem.projections.EmbeddingProvider;
import com.hitorro.jsontypesystem.projections.HashingEmbeddingProvider;
import com.hitorro.jsontypesystem.projections.InMemoryDocumentStore;
import com.hitorro.language.LanguageDetectorSingleton;
import com.hitorro.language.LemmatizerModelSingleton;
import com.hitorro.language.Iso639Table;
import com.hitorro.language.IsoLanguage;
import com.hitorro.util.core.Env;
import com.hitorro.util.json.keys.propaccess.Propaccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import opennlp.tools.langdetect.Language;
import opennlp.tools.langdetect.LanguageDetectorME;
import opennlp.tools.lemmatizer.LemmatizerME;
import opennlp.tools.lemmatizer.LemmatizerModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Interactive showcase for the JVS features added in the projection / validation / NLP
 * enhancements. Every endpoint operates on a caller-supplied JSON document (or a canned
 * sample from {@link #samples()}) so the UI can render before/after side-by-side.
 *
 * <p>The playground ships its type definition inline (see {@link #PLAYGROUND_TYPE_JSON})
 * so the demo works without touching HT_BIN/config/types. Callers can override with their
 * own type-def JSON per request to explore group-annotation combinations.
 */
@RestController
@RequestMapping("/api/playground")
@Tag(name = "JVS Playground", description = "Interactive showcase for the new projection/validation/NLP features")
public class PlaygroundController {

	private static final Logger logger = LoggerFactory.getLogger(PlaygroundController.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();

	// A demo_document variant with every projection group and constraint attached to at least one
	// field. Kept in one place so the UI can display it and users can copy/paste to tweak.
	static final String PLAYGROUND_TYPE_JSON = """
			{
			  "name": "demo_document_playground",
			  "description": "demo_document annotated with every projection group + JSON-Schema constraint",
			  "fields": [
			    {"name": "filename", "type": "core_string", "minLength": 1, "maxLength": 255,
			     "groups": [
			       {"name": "validate",    "method": "check"},
			       {"name": "fingerprint", "method": "sha256"}
			     ]},
			    {"name": "file_type", "type": "core_string",
			     "enum": ["application/pdf","text/plain","text/html","application/msword","image/png","image/jpeg"],
			     "groups": [{"name": "validate", "method": "check"}]},
			    {"name": "file_size", "type": "core_long", "minimum": 0, "maximum": 1073741824,
			     "groups": [
			       {"name": "validate",    "method": "check"},
			       {"name": "fingerprint", "method": "sha256"}
			     ]},
			    {"name": "version", "type": "core_string", "pattern": "^\\\\d+\\\\.\\\\d+(\\\\.\\\\d+)?$",
			     "groups": [
			       {"name": "validate",    "method": "check"},
			       {"name": "fingerprint", "method": "sha256"}
			     ]},
			    {"name": "author", "type": "core_string", "format": "email",
			     "groups": [
			       {"name": "validate",    "method": "check"},
			       {"name": "redact",      "method": "hmac"},
			       {"name": "materialize", "method": "ref"}
			     ]},
			    {"name": "department", "type": "core_string"},
			    {"name": "content", "type": "core_mls",
			     "groups": [
			       {"name": "i18n",      "method": "flatten"},
			       {"name": "vectorize", "method": "embed"}
			     ]},
			    {"name": "keywords", "type": "core_string", "vector": true},
			    {"name": "classification", "type": "core_string",
			     "enum": ["public","internal","restricted","confidential"],
			     "groups": [{"name": "validate", "method": "check"}]},
			    {"name": "checksum", "type": "core_string",
			     "groups": [{"name": "redact", "method": "hash"}]},
			    {"name": "download_url", "type": "core_url", "format": "uri",
			     "groups": [{"name": "validate", "method": "check"}]}
			  ]
			}""";

	private static final String SAMPLE_DOC_JSON = """
			{
			  "filename": "annual-report.pdf",
			  "file_type": "application/pdf",
			  "file_size": 12345,
			  "version": "2.1",
			  "author": "chris.collins@hitorro.com",
			  "department": "engineering",
			  "content": {"mls": [
			    {"lang": "en", "text": "The quick brown fox jumps over the lazy dog. This report summarises Q3 results."},
			    {"lang": "fr", "text": "Le renard brun rapide saute par-dessus le chien paresseux. Ce rapport résume les résultats du T3."}
			  ]},
			  "keywords": ["quarterly", "report", "engineering"],
			  "classification": "internal",
			  "checksum": "abc123",
			  "download_url": "https://example.com/annual-report.pdf"
			}""";

	// ---------------------------------------------------------------------------------------
	// Samples endpoint — the UI calls this once on load to populate the editable JSON pane
	// with a working document + type definition. Also exposes a canned reference-document map
	// so the materialize demo has something to resolve.
	// ---------------------------------------------------------------------------------------

	@GetMapping("/samples")
	@Operation(summary = "Sample document, playground type, and reference-store contents")
	public ResponseEntity<SamplesResponse> samples() throws Exception {
		SamplesResponse r = new SamplesResponse();
		r.document = objectMapper.readTree(SAMPLE_DOC_JSON);
		r.typeDefinition = objectMapper.readTree(PLAYGROUND_TYPE_JSON);
		Map<String, JsonNode> refs = new LinkedHashMap<>();
		refs.put("chris.collins@hitorro.com", objectMapper.readTree(
				"{\"name\":\"Chris Collins\",\"role\":\"Engineer\",\"email\":\"chris@hitorro.com\"}"));
		refs.put("user-42", objectMapper.readTree(
				"{\"name\":\"Alex Doe\",\"role\":\"Analyst\",\"email\":\"alex@example.com\"}"));
		r.references = refs;
		return ResponseEntity.ok(r);
	}

	// ---------------------------------------------------------------------------------------
	// Projection endpoints — one per action. Each accepts a document (and optional per-action
	// state) and returns { source, target, notes } so the UI can diff before/after.
	// ---------------------------------------------------------------------------------------

	@PostMapping("/projection/redact")
	@Operation(summary = "Run the redact projection (mask/hash/hmac/null modes) on a document")
	public ResponseEntity<ProjectionResponse> redact(@RequestBody ProjectionRequest req) {
		return runProjection(req, GroupNameFilter.redactFilter, new RedactFactory(), (pc, r) -> {
			if (r.redactionKey != null && !r.redactionKey.isBlank()) {
				pc.redactionKey = new SecretKeySpec(
						r.redactionKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
			}
		});
	}

	@PostMapping("/projection/validate")
	@Operation(summary = "Run the validate projection — group-scoped constraint check, returns violations")
	public ResponseEntity<ProjectionResponse> projectionValidate(@RequestBody ProjectionRequest req) {
		return runProjection(req, GroupNameFilter.validateFilter, new ValidateFactory(),
				(pc, r) -> pc.violations = new ArrayList<>());
	}

	@PostMapping("/projection/fingerprint")
	@Operation(summary = "Run the fingerprint projection — SHA-256 digest over projected fields")
	public ResponseEntity<ProjectionResponse> fingerprint(@RequestBody ProjectionRequest req) {
		return runProjection(req, GroupNameFilter.fingerprintFilter, new FingerprintFactory(), (pc, r) -> {
			try {
				pc.fingerprint = MessageDigest.getInstance("SHA-256");
			} catch (Exception e) {
				throw new RuntimeException("SHA-256 unavailable", e);
			}
		});
	}

	@PostMapping("/projection/materialize")
	@Operation(summary = "Run the materialize projection — dereference IDs via an in-memory DocumentStore")
	public ResponseEntity<ProjectionResponse> materialize(@RequestBody ProjectionRequest req) {
		return runProjection(req, GroupNameFilter.materializeFilter, new MaterializeFactory(), (pc, r) -> {
			InMemoryDocumentStore store = new InMemoryDocumentStore();
			if (r.references != null) {
				for (Map.Entry<String, JsonNode> e : r.references.entrySet()) {
					store.put(e.getKey(), e.getValue());
				}
			}
			pc.documentStore = store;
		});
	}

	@PostMapping("/projection/i18n")
	@Operation(summary = "Run the i18n projection — flatten MLS envelopes to per-language scalars")
	public ResponseEntity<ProjectionResponse> i18n(@RequestBody ProjectionRequest req) {
		return runProjectionWithLang(req, GroupNameFilter.i18nFilter, new I18nFactory(),
				req.lang == null ? "en" : req.lang);
	}

	@PostMapping("/projection/vectorize")
	@Operation(summary = "Run the vectorize projection — embed text fields via HashingEmbeddingProvider")
	public ResponseEntity<ProjectionResponse> vectorize(@RequestBody ProjectionRequest req) {
		int dims = req.dimensions == null || req.dimensions <= 0 ? 64 : req.dimensions;
		EmbeddingProvider embedder = new HashingEmbeddingProvider(dims);
		if (req.lang != null && !req.lang.isBlank()) {
			return runProjectionWithLangAndEmbedder(req, GroupNameFilter.vectorizeFilter,
					new VectorizeFactory(), req.lang, embedder);
		}
		return runProjection(req, GroupNameFilter.vectorizeFilter, new VectorizeFactory(),
				(pc, r) -> pc.embeddingProvider = embedder);
	}

	// ---------------------------------------------------------------------------------------
	// Non-projection features: document-level validation, language detection, lemmatization,
	// and a canned DSL-fill-ins transform.
	// ---------------------------------------------------------------------------------------

	@PostMapping("/validate")
	@Operation(summary = "JVSValidator — whole-document validation against a Type, using FieldConstraints")
	public ResponseEntity<ValidateResponse> validateDocument(@RequestBody ValidateRequest req) {
		try {
			Type type = buildTypeOrDefault(req.typeDefinition);
			JVS doc = new JVS(req.document);
			List<JVSValidator.Violation> violations = JVSValidator.validate(doc, type);

			ValidateResponse r = new ValidateResponse();
			r.violations = violations.stream().map(v -> new ViolationDTO(
					v.path(), v.message(), v.level().name())).toList();
			r.report = JVSValidator.report(doc, type);
			r.valid = violations.stream().noneMatch(v -> v.level() == JVSValidator.Level.ERROR);
			return ResponseEntity.ok(r);
		} catch (Exception e) {
			logger.error("validateDocument failed", e);
			return ResponseEntity.badRequest().body(new ValidateResponse().withError(e.getMessage()));
		}
	}

	@PostMapping("/detect-lang")
	@Operation(summary = "Detect the ISO-639-1 language code of a text string")
	public ResponseEntity<DetectLangResponse> detectLang(@RequestBody DetectLangRequest req) {
		DetectLangResponse r = new DetectLangResponse();
		if (req.text == null || req.text.length() < 20) {
			r.language = null;
			r.detector = "skipped";
			r.note = "Text under 20 chars — LanguageDetectorMapper skips detection to avoid false positives.";
			return ResponseEntity.ok(r);
		}
		try {
			// Prefer OpenNLP's langdetect model if present; fall back to the module's n-gram table.
			LanguageDetectorME me = LanguageDetectorSingleton.get();
			if (me != null) {
				Language best = me.predictLanguage(req.text);
				if (best != null && best.getLang() != null) {
					r.language = toTwoLetter(best.getLang());
					r.detector = "opennlp-langdetect";
					r.confidence = best.getConfidence();
					return ResponseEntity.ok(r);
				}
			}
			IsoLanguage lang = Iso639Table.getInstance().getLanguageFromContent(req.text);
			r.language = lang == null ? null : lang.getTwo();
			r.detector = "iso639-ngram";
			r.note = me == null
					? "OpenNLP langdetect-183.bin not installed — using the built-in n-gram profile detector."
					: null;
			return ResponseEntity.ok(r);
		} catch (Throwable t) {
			logger.warn("detect-lang failed", t);
			r.language = null;
			r.detector = "unavailable";
			r.note = "Detection failed: " + t.getMessage();
			return ResponseEntity.ok(r);
		}
	}

	@PostMapping("/lemmatize")
	@Operation(summary = "Lemmatize tokens using the OpenNLP LemmatizerModel for the given language")
	public ResponseEntity<LemmatizeResponse> lemmatize(@RequestBody LemmatizeRequest req) {
		LemmatizeResponse r = new LemmatizeResponse();
		if (req.tokens == null || req.posTags == null || req.tokens.length != req.posTags.length) {
			r.error = "tokens and posTags must be non-null and the same length";
			return ResponseEntity.badRequest().body(r);
		}
		try {
			IsoLanguage iso = Iso639Table.getInstance().getRow(req.lang == null ? "en" : req.lang);
			if (iso == null) {
				r.error = "unknown language code: " + req.lang;
				return ResponseEntity.badRequest().body(r);
			}
			LemmatizerModel model = LemmatizerModelSingleton.singleton.get(iso);
			if (model == null) {
				r.error = "no lemmatizer model installed for language " + iso.getTwo()
						+ " — expected " + iso.getTwo() + "-lemmatizer.bin under the OpenNLP model directory";
				r.note = "LemmatizerMapper degrades to null in this case; callers typically fall back to Snowball stemming.";
				return ResponseEntity.ok(r);
			}
			LemmatizerME lem = new LemmatizerME(model);
			r.lemmas = lem.lemmatize(req.tokens, req.posTags);
			return ResponseEntity.ok(r);
		} catch (Throwable t) {
			logger.warn("lemmatize failed", t);
			r.error = t.getMessage();
			return ResponseEntity.badRequest().body(r);
		}
	}

	@PostMapping("/dsl-transform")
	@Operation(summary = "Run a Groovy transform showcasing the new fill-ins (deleteIf / mapArray / ifMissing / coalesce)")
	public ResponseEntity<DslTransformResponse> dslTransform(@RequestBody DslTransformRequest req) {
		DslTransformResponse r = new DslTransformResponse();
		try {
			File genDir = envBin() == null ? null : new File(envBin(), "config/generators");
			GroovyTransformMapper mapper = GroovyTransformMapper.fromString(
					req.script == null ? DEFAULT_DSL_SCRIPT : req.script, genDir);
			JVS input = new JVS(req.input == null ? objectMapper.readTree(SAMPLE_DSL_INPUT) : req.input);
			JVS output = mapper.apply(input);
			r.input = input.getJsonNode();
			r.output = output == null ? null : output.getJsonNode();
			return ResponseEntity.ok(r);
		} catch (Exception e) {
			logger.warn("dsl-transform failed", e);
			r.error = e.getMessage();
			return ResponseEntity.badRequest().body(r);
		}
	}

	// A canned demo script that touches every new fill-in. The UI loads it as the default script.
	static final String DEFAULT_DSL_SCRIPT = """
			// Showcase the DSL fill-ins added alongside the projection work.
			copyAll()

			// coalesce — pick the first non-null across candidate paths
			set "target.display_name", coalesce("source.nickname", "source.name", "source.email")

			// ifMissing — set a default only if the target is currently absent
			ifMissing "target.status", "draft"
			ifMissing "target.priority", 3

			// mapArray — element-wise transform over an array
			mapArray("target.tags") { tag -> tag.textValue().toLowerCase() }

			// deleteIf — conditional removal
			deleteIf("target.internal_note") { it?.textValue() == "REMOVE" }
			""";

	static final String SAMPLE_DSL_INPUT = """
			{
			  "name": "Chris Collins",
			  "email": "chris@hitorro.com",
			  "tags": ["Search", "NLP", "TypeSystem"],
			  "internal_note": "REMOVE",
			  "priority": null
			}""";

	// ---------------------------------------------------------------------------------------
	// Internal helpers
	// ---------------------------------------------------------------------------------------

	private <A extends com.hitorro.jsontypesystem.executors.ExecutorAction<ExecutionBuilder>>
	ResponseEntity<ProjectionResponse> runProjection(
			ProjectionRequest req,
			@SuppressWarnings("rawtypes") Predicate groupFilter,
			com.hitorro.jsontypesystem.executors.ExecutorFactory<A> factory,
			ContextConfigurer configurer) {
		ProjectionResponse resp = new ProjectionResponse();
		try {
			Type type = buildTypeOrDefault(req.typeDefinition);
			@SuppressWarnings("unchecked")
			ExecutionBuilder<A> plan = new ExecutionBuilder<>(factory);
			type.visit(plan, (Predicate<BaseT>) groupFilter, new Propaccess(""));
			plan.finalizeNode();

			JVS source = new JVS(req.document.deepCopy());
			JVS target = new JVS();
			ProjectionContext pc = new ProjectionContext();
			pc.source = source;
			pc.target = target;
			configurer.configure(pc, req);
			plan.getExecutor().project(pc);

			resp.source = source.getJsonNode();
			resp.target = target.getJsonNode();
			resp.digest = pc.fingerprint == null ? null
					: HexFormat.of().formatHex(pc.fingerprint.digest());
			if (pc.violations != null) {
				resp.violations = pc.violations.stream().map(v -> new ViolationDTO(
						v.path(), v.message(), v.level().name())).toList();
			}
			return ResponseEntity.ok(resp);
		} catch (RedactAction.RedactionFailedException e) {
			resp.error = "Redaction failed: " + e.getMessage()
					+ " (hmac mode requires a non-empty redactionKey)";
			return ResponseEntity.badRequest().body(resp);
		} catch (Exception e) {
			logger.warn("projection failed", e);
			resp.error = e.getMessage();
			return ResponseEntity.badRequest().body(resp);
		}
	}

	private <A extends com.hitorro.jsontypesystem.executors.ExecutorAction<ExecutionBuilder>>
	ResponseEntity<ProjectionResponse> runProjectionWithLang(
			ProjectionRequest req,
			@SuppressWarnings("rawtypes") Predicate groupFilter,
			com.hitorro.jsontypesystem.executors.ExecutorFactory<A> factory,
			String lang) {
		ProjectionResponse resp = new ProjectionResponse();
		try {
			Type type = buildTypeOrDefault(req.typeDefinition);
			@SuppressWarnings("unchecked")
			ExecutionBuilder<A> plan = new ExecutionBuilder<>(factory);
			type.visit(plan, (Predicate<BaseT>) groupFilter, new Propaccess(""));
			plan.finalizeNode();

			JVS source = new JVS(req.document.deepCopy());
			JVS target = new JVS();
			ProjectionContext pc = new ProjectionContext();
			pc.source = source;
			pc.target = target;
			plan.getExecutor().project(pc, pc.path, false, lang);

			resp.source = source.getJsonNode();
			resp.target = target.getJsonNode();
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			logger.warn("projection failed", e);
			resp.error = e.getMessage();
			return ResponseEntity.badRequest().body(resp);
		}
	}

	private ResponseEntity<ProjectionResponse> runProjectionWithLangAndEmbedder(
			ProjectionRequest req,
			@SuppressWarnings("rawtypes") Predicate groupFilter,
			com.hitorro.jsontypesystem.executors.ExecutorFactory<VectorizeAction> factory,
			String lang,
			EmbeddingProvider embedder) {
		ProjectionResponse resp = new ProjectionResponse();
		try {
			Type type = buildTypeOrDefault(req.typeDefinition);
			ExecutionBuilder<VectorizeAction> plan = new ExecutionBuilder<>(factory);
			type.visit(plan, (Predicate<BaseT>) groupFilter, new Propaccess(""));
			plan.finalizeNode();

			JVS source = new JVS(req.document.deepCopy());
			JVS target = new JVS();
			ProjectionContext pc = new ProjectionContext();
			pc.source = source;
			pc.target = target;
			pc.embeddingProvider = embedder;
			plan.getExecutor().project(pc, pc.path, false, lang);

			resp.source = source.getJsonNode();
			resp.target = target.getJsonNode();
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			logger.warn("vectorize projection failed", e);
			resp.error = e.getMessage();
			return ResponseEntity.badRequest().body(resp);
		}
	}

	private Type buildTypeOrDefault(JsonNode override) {
		Type t = new Type();
		JsonNode def;
		if (override != null && !override.isNull() && override.isObject() && override.size() > 0) {
			def = override;
		} else {
			try {
				def = objectMapper.readTree(PLAYGROUND_TYPE_JSON);
			} catch (Exception e) {
				throw new RuntimeException("failed to parse default playground type", e);
			}
		}
		t.init(def);
		return t;
	}

	private static String toTwoLetter(String code) {
		if (code == null) return null;
		try {
			IsoLanguage lang = Iso639Table.getInstance().getRow(code);
			if (lang != null && lang.getTwo() != null && !lang.getTwo().isEmpty()) {
				return lang.getTwo();
			}
		} catch (Throwable ignored) { /* fall through */ }
		return code;
	}

	private static File envBin() {
		try {
			return Env.getBin();
		} catch (Throwable t) {
			return null;
		}
	}

	@FunctionalInterface
	private interface ContextConfigurer {
		void configure(ProjectionContext pc, ProjectionRequest req);
	}

	// ---------------------------------------------------------------------------------------
	// DTOs — kept as public static classes so Jackson can bind them and Swagger can document.
	// ---------------------------------------------------------------------------------------

	public static class SamplesResponse {
		public JsonNode document;
		public JsonNode typeDefinition;
		public Map<String, JsonNode> references;
	}

	public static class ProjectionRequest {
		public JsonNode document;
		public JsonNode typeDefinition; // optional override
		public String lang;             // i18n + vectorize
		public Integer dimensions;      // vectorize
		public String redactionKey;     // redact/hmac
		public Map<String, JsonNode> references; // materialize
	}

	public static class ProjectionResponse {
		public JsonNode source;
		public JsonNode target;
		public String digest;
		public List<ViolationDTO> violations;
		public String error;
	}

	public static class ValidateRequest {
		public JsonNode document;
		public JsonNode typeDefinition; // optional override
	}

	public static class ValidateResponse {
		public boolean valid;
		public List<ViolationDTO> violations;
		public String report;
		public String error;

		public ValidateResponse withError(String msg) {
			this.error = msg;
			return this;
		}
	}

	public record ViolationDTO(String path, String message, String level) {}

	public static class DetectLangRequest {
		public String text;
	}

	public static class DetectLangResponse {
		public String language;
		public String detector; // "opennlp-langdetect" | "iso639-ngram" | "skipped" | "unavailable"
		public Double confidence;
		public String note;
	}

	public static class LemmatizeRequest {
		public String lang;
		public String[] tokens;
		public String[] posTags;
	}

	public static class LemmatizeResponse {
		public String[] lemmas;
		public String error;
		public String note;
	}

	public static class DslTransformRequest {
		public String script;
		public JsonNode input;
	}

	public static class DslTransformResponse {
		public JsonNode input;
		public JsonNode output;
		public String error;
	}
}
