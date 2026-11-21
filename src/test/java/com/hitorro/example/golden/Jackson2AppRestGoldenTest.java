package com.hitorro.example.golden;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * The app's REST/JSON responses, controller by controller, over a fixed scenario (create, read,
 * query, update, version, delete documents and containers; index and search; key/value store;
 * JVS types and fields; playground projections; data mapper; JVS SQL; file system; the starter's
 * /api/rest and /api/commands; actuator health).
 * <p>
 * {@code jackson2-app-rest.txt} was recorded by running this same test with
 * {@code -Dgolden.record=<file>} on main, where HTTP JSON went through Spring Boot's
 * auto-configured Jackson 2 mapper (spring-boot-jackson2, preferred-json-mapper=jackson2). The
 * migrated app serves JSON through Boot's Jackson 3 mapper and must return the same status,
 * content type and body. Run-specific values are masked: generated ids and guids, timestamps,
 * durations, object hashes and absolute paths. The source has no Jackson types, so it compiles on
 * both. Ollama is pointed at a closed port, so AI endpoints answer with their (deterministic)
 * error responses.
 */
@SpringBootTest(properties = {
        "spring.ai.ollama.base-url=http://127.0.0.1:1",
        "spring.ai.retry.max-attempts=1",
        "spring.datasource.url=jdbc:h2:mem:goldendb;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "hitorro.filesystem.local.enabled=true",
        "hitorro.filesystem.local.base-path=./target/golden-files"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class Jackson2AppRestGoldenTest {

    static final String[] UNORDERED_KEYS = {"jvssql.examples", "lucene.stats", "openapi"};

    @Autowired
    private MockMvc mvc;

    /**
     * This test's context registers an AI service pointing at a closed port in the static
     * AIServiceRegistry; put back the one other test classes registered, so tests that check
     * {@code AIServiceRegistry.isAvailable()} afterwards see the same registry as without this test.
     */
    private static com.hitorro.basedms.transformer.ai.AIService previousAiService;

    @org.junit.jupiter.api.BeforeAll
    static void saveAiRegistry() {
        previousAiService = com.hitorro.basedms.transformer.ai.AIServiceRegistry.getInstance();
    }

    @org.junit.jupiter.api.AfterAll
    static void restoreAiRegistry() {
        com.hitorro.basedms.transformer.ai.AIServiceRegistry.setInstance(previousAiService);
    }

    private final Map<String, String> out = new LinkedHashMap<>();
    private String last = "";

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    private String call(String key, MockHttpServletRequestBuilder request) throws Exception {
        MvcResult r;
        try {
            r = mvc.perform(request.accept(MediaType.APPLICATION_JSON, MediaType.ALL)).andReturn();
        } catch (Exception e) {
            Throwable t = e;
            while (t.getCause() != null) t = t.getCause();
            out.put(key, "!" + t.getClass().getSimpleName());
            last = "";
            return "";
        }
        if (r.getRequest().isAsyncStarted()) {
            r = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(r)).andReturn();
        }
        String body = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        last = body;
        if (UNORDERED.contains(key)) {
            body = Canonical.sortKeys(body);
        }
        // actuator.health is compared strictly. On main (Boot's Jackson 2 endpoint mapper) a health
        // component's "status"/"details" order changed between JVM runs (IndicatedHealthDescriptor has
        // only getters, ordered by Class.getDeclaredMethods); the golden holds main's usual order,
        // status first, which hitorro-spring-boot-starter 4.0.0's endpoint mapper now always writes.
        if (key.startsWith("actuator.metric")) {
            body = body.replaceAll("\"value\":[0-9.E-]+", "\"value\":<n>");   // the metric's current value
        }
        // A JSON parse error's own message text and location differ between Jackson 2 and 3
        // ("field name" / "property name", "line: 1, column: 2" / "byte offset"): not pinned.
        body = body.replaceAll("Unexpected character \\('[^)]*\\)\\): was expecting double-quote to start (field|property) name\\\\n at \\[Source: [^\\]]*\\]",
                "<JSON parse error: unexpected character, expecting a name>");
        out.put(key, r.getResponse().getStatus() + " " + r.getResponse().getContentType() + " " + mask(body));
        return body;
    }

    /**
     * Bodies built from {@code Map.of}/{@code HashMap}, whose key order changes from one JVM run to the
     * next on main too (found by recording main repeatedly): compared with object keys sorted.
     * (actuator.health is compared strictly, see call()).
     * "openapi": swagger-core's schema property order differs between main's test run and main's
     * packaged jar; the jars' /api-docs were compared byte for byte separately (identical).
     */
    private static final java.util.Set<String> UNORDERED = java.util.Set.of(UNORDERED_KEYS);

    private String postJson(String key, String path, String json) throws Exception {
        return call(key, post(path).contentType(JSON).content(json));
    }

    static String mask(String body) {
        String cwd = new File("").getAbsolutePath();
        String s = body.replace(cwd, "<cwd>");
        for (String p : new String[]{System.getProperty("HT_BIN"), System.getenv("HT_BIN"), System.getProperty("HT_HOME"), System.getenv("HT_HOME"),
                System.getProperty("java.io.tmpdir"), System.getProperty("user.home")}) {
            if (p != null && p.length() > 3) s = s.replace(p, "<dir>");
        }
        s = s.replaceAll("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "<uuid>")
                .replaceAll("\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:?\\d{2})?", "<datetime>")
                .replaceAll("\\b1[6-9]\\d{11}\\b", "<epoch>")
                .replaceAll("\"([A-Za-z]*([Tt]ime|Ms|[Mm]illis|[Dd]uration|[Ee]lapsed|[Tt]ook|[Uu]ptime)[A-Za-z]*)\":\\s*-?\\d+(\\.\\d+)?(E-?\\d+)?", "\"$1\":<n>")
                .replaceAll("@[0-9a-f]{5,}\\b", "@<hash>")
                .replaceAll("\"(guid|canonicalGuid|contentGuid|documentGuid|did)\":\"[^\"]*\"", "\"$1\":<guid>")
                .replaceAll("(?<=[iI][dD]\":)\\d+", "<id>")
                .replaceAll("\"(total|free|threshold)\":\\d+", "\"$1\":<bytes>")
                .replaceAll("\\\\r\\\\n", "\\\\n");
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String firstId(String body) {
        Matcher m = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(body);
        return m.find() ? m.group(1) : "0";
    }

    @Test
    void responsesMatchJackson2() throws Exception {
        // ---- actuator and app status
        call("actuator.health", get("/actuator/health"));
        call("actuator.health.ping", get("/actuator/health/ping"));
        call("actuator.info", get("/actuator/info"));
        call("actuator.metric", get("/actuator/metrics/jvm.threads.daemon"));
        call("example.status", get("/api/example/status"));
        call("example.basic-service", get("/api/example/basic-service"));
        call("example.services", get("/api/example/services"));
        call("example.health", get("/api/example/health"));
        call("health.ollama", get("/api/health/ollama"));

        // ---- DocumentManagementController
        String c1 = postJson("dms.container.create", "/api/dms/containers", "{\"name\":\"Golden box\",\"description\":\"d\\u00e9sc\"}");
        String cid = firstId(c1);
        String d1 = postJson("dms.doc.create", "/api/dms/documents",
                "{\"title\":\"Golden doc \\u00e9 \\uD83D\\uDE00\",\"note\":\"a note\",\"creator\":\"chris\",\"realm\":\"r\",\"unknownField\":1,"
                        + "\"categories\":[{\"domain\":\"topic\",\"value\":\"food\"}]}");
        String did = firstId(d1);
        postJson("dms.doc.create2", "/api/dms/documents", "{\"title\":\"Second\",\"note\":null}");
        call("dms.doc.get", get("/api/dms/documents/" + did));
        call("dms.doc.get.missing", get("/api/dms/documents/999999"));
        call("dms.doc.update", put("/api/dms/documents/" + did).contentType(JSON).content("{\"title\":\"Golden doc v2\",\"note\":\"changed\"}"));
        call("dms.doc.checkout", put("/api/dms/documents/" + did + "/checkout").contentType(JSON).content("{\"note\":\"checked out\"}"));
        postJson("dms.doc.version", "/api/dms/documents/" + did + "/version", "{\"note\":\"v\",\"title\":\"versioned\"}");
        call("dms.doc.versions", get("/api/dms/documents/" + did + "/versions"));
        call("dms.doc.container.add", post("/api/dms/documents/" + did + "/containers/" + cid));
        call("dms.doc.containers", get("/api/dms/documents/" + did + "/containers"));
        call("dms.container.get", get("/api/dms/containers/" + cid));
        call("dms.container.list", get("/api/dms/containers"));
        call("dms.container.documents", get("/api/dms/containers/" + cid + "/documents"));
        postJson("dms.doc.categories.add", "/api/dms/documents/" + did + "/categories", "{\"domain\":\"topic\",\"value\":\"wine\"}");
        call("dms.doc.search.category", get("/api/dms/documents/search/category").param("domain", "topic").param("value", "food"));
        postJson("dms.doc.query", "/api/dms/documents/query", "{\"title\":\"Second\"}");
        postJson("dms.doc.query.dates", "/api/dms/documents/query", "{\"createdAfter\":\"2020-01-01T00:00:00.000+00:00\",\"createdBefore\":1700000000000}");
        call("dms.doc.content.list", get("/api/dms/documents/" + did + "/content/list"));
        call("dms.doc.container.remove", delete("/api/dms/documents/" + did + "/containers/" + cid));
        call("dms.doc.categories.remove", delete("/api/dms/documents/" + did + "/categories").contentType(JSON).content("{\"domain\":\"topic\",\"value\":\"wine\"}"));
        call("dms.doc.delete.missing", delete("/api/dms/documents/999999"));

        // ---- DMS -> JVS
        call("dmsjvs.convert", get("/api/dms/jvs/convert/nope-guid"));
        postJson("dmsjvs.batch", "/api/dms/jvs/convert/batch", "{\"guids\":[\"a\",\"b\"]}");

        // ---- JVSController and TranslationController
        call("jvs.types", get("/api/jvs/types"));
        call("jvs.type.missing", get("/api/jvs/types/no_such_type"));
        postJson("jvs.field", "/api/jvs/field", "{\"json\":\"{\\\"title\\\":\\\"x\\\",\\\"n\\\":1.10,\\\"o\\\":{\\\"k\\\":[1,2]}}\",\"fieldPath\":\"o.k\"}");
        postJson("jvs.field.bad", "/api/jvs/field", "{\"json\":\"{bad\",\"fieldPath\":\"a\"}");
        postJson("jvs.enrich", "/api/jvs/enrich", "{\"json\":\"{\\\"type\\\":\\\"demo_article\\\",\\\"title\\\":\\\"Hello world\\\"}\",\"typeName\":\"demo_article\",\"tags\":\"\"}");
        call("translate.languages", get("/api/jvs/translate/languages"));
        call("translate.status", get("/api/jvs/translate/status"));

        // ---- SearchController and KVStoreController
        call("search.stats", get("/api/search/stats"));
        call("search.indexes", get("/api/search/indexes"));
        call("search.fields", get("/api/search/fields"));
        call("search.facets", get("/api/search/facets"));
        call("search.query", get("/api/search/query").param("q", "hello"));
        call("kv.stats", get("/api/kvstore/stats"));
        postJson("kv.put", "/api/kvstore/golden-key", "{\"v\":1.10,\"s\":\"\\u00e9\",\"a\":[1,null]}");
        call("kv.get", get("/api/kvstore/golden-key"));
        call("kv.get.missing", get("/api/kvstore/no-such-key"));
        postJson("kv.batch", "/api/kvstore/batch", "{\"k1\":{\"x\":1},\"k2\":\"str\"}");
        call("kv.delete", delete("/api/kvstore/golden-key"));

        // ---- PlaygroundController
        call("playground.samples", get("/api/playground/samples"));
        String doc = "{\"document\":{\"type\":\"demo_article\",\"title\":\"Hello\",\"body\":\"Some text.\"},\"lang\":\"en\",\"dimensions\":8,\"redactionKey\":\"k\"}";
        for (String p : new String[]{"redact", "validate", "fingerprint", "materialize", "i18n", "vectorize"}) {
            postJson("playground.projection." + p, "/api/playground/projection/" + p, doc);
        }
        postJson("playground.validate", "/api/playground/validate", "{\"document\":{\"type\":\"demo_article\",\"title\":5}}");
        postJson("playground.detect-lang", "/api/playground/detect-lang", "{\"text\":\"Ceci est une phrase en fran\\u00e7ais.\"}");
        postJson("playground.lemmatize", "/api/playground/lemmatize", "{\"text\":\"The cats were running\",\"lang\":\"en\"}");
        postJson("playground.dsl", "/api/playground/dsl-transform", "{\"source\":{\"a\":1,\"b\":[1,2]},\"script\":\"\"}");

        // ---- DataMapperController and JvsSqlPlaygroundController
        call("datamapper.scripts", get("/api/datamapper/scripts"));
        postJson("datamapper.validate", "/api/datamapper/validate", "{\"script\":\"\"}");
        postJson("datamapper.execute", "/api/datamapper/execute", "{\"script\":\"\",\"inputData\":\"{\\\"a\\\":1}\"}");
        call("jvssql.examples", get("/api/jvssql/examples"));
        postJson("jvssql.execute", "/api/jvssql/execute",
                "{\"sql\":\"select name, n from t where n > 1 order by n\",\"typeJson\":null,"
                        + "\"rowsJson\":\"[{\\\"name\\\":\\\"a\\\",\\\"n\\\":1},{\\\"name\\\":\\\"b\\\",\\\"n\\\":2.5},{\\\"name\\\":\\\"c\\\",\\\"n\\\":3}]\",\"tableName\":\"t\"}");

        // ---- FileSystemExampleController, UserActivityDemoController, TransformationTestController
        call("fs.status", get("/api/filesystem/status"));
        postJson("fs.write", "/api/filesystem/local/write", "{\"path\":\"golden/a.txt\",\"content\":\"hello \\u00e9\"}");
        call("fs.list", get("/api/filesystem/local/list").param("path", "golden"));
        call("fs.read", get("/api/filesystem/local/read/golden/a.txt"));
        call("demo.info", get("/api/demo/info"));
        call("demo.api-access", get("/api/demo/api-access"));
        call("transform.available", get("/api/test/transformations/available"));

        // ---- LuceneViewer, GitTools (read-only, no paths of this machine)
        call("lucene.stats", get("/api/luceneviewer/stats"));
        call("gittools.status", get("/api/gittools/status"));

        // ---- the OpenAPI document (springdoc; JsonNode must stay the free-form "JsonNode" schema)
        call("openapi", get("/api-docs"));

        // ---- the starter's controllers
        call("starter.rest.discovery", get("/api/rest"));
        call("starter.commands.list", get("/api/commands/list"));

        // ---- clean up
        call("dms.doc.delete", delete("/api/dms/documents/" + did));
        call("dms.container.delete", delete("/api/dms/containers/" + cid));

        String record = System.getProperty("golden.record");
        if (record != null) {
            StringBuilder sb = new StringBuilder();
            out.forEach((k, v) -> sb.append(k).append('=').append(v).append('\n'));
            Files.writeString(Path.of(record), sb.toString());
            return;
        }
        Map<String, String> golden = new LinkedHashMap<>();
        try (InputStream in = getClass().getResourceAsStream("jackson2-app-rest.txt")) {
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (line.isEmpty()) continue;
                int eq = line.indexOf('=');
                golden.put(line.substring(0, eq), line.substring(eq + 1));
            }
        }
        for (Map.Entry<String, String> e : golden.entrySet()) {
            assertEquals(e.getValue(), out.get(e.getKey()), e.getKey());
        }
        assertEquals(golden.keySet(), out.keySet());
    }
}
