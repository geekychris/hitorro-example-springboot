/*
 * Copyright (c) 2006-2025 Chris Collins
 */
package com.hitorro.example.jvssql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.jsontypesystem.Type;
import com.hitorro.jvssql.JvsSqlEngine;
import com.hitorro.jvssql.PreparedQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for the in-app JVS-SQL playground. Serves preset examples
 * (query + input data + type) and executes SQL against an on-the-fly-registered
 * stream constructed from a JSON array.
 *
 * <p>Endpoints live under {@code /api/jvssql/} alongside the other DMS /
 * playground / kvstore APIs.</p>
 */
@RestController
@RequestMapping("/api/jvssql")
@Tag(name = "JVS-SQL Playground", description = "Streaming SQL over JVS documents")
public class JvsSqlPlaygroundController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Preset example — id, title, description, SQL, sample JVS array, JVS Type JSON, and the SQL table name. */
    public record Example(String id, String title, String description,
                          String sql, String rowsJson, String typeJson, String tableName) {}

    private final List<Example> examples = List.of(
        example("01-basic", "Basic SELECT + WHERE",
            "Filter and project columns from a stream of document metadata.",
            String.join("\n",
                "SELECT filename, file_size",
                "FROM docs",
                "WHERE classification = 'public' AND file_size > 100"),
            docsRows("[" +
                "{\"filename\":\"report.pdf\",  \"classification\":\"public\",   \"file_size\":1500}," +
                "{\"filename\":\"invoice.pdf\", \"classification\":\"internal\", \"file_size\":800}," +
                "{\"filename\":\"notes.txt\",   \"classification\":\"public\",   \"file_size\":200}," +
                "{\"filename\":\"secret.pdf\",  \"classification\":\"restricted\",\"file_size\":50}" +
                "]"),
            docsTypeJson(), "docs"),

        example("02-expressions", "Expressions — CASE, LIKE, IN, arithmetic",
            "Case-bucket file sizes; filter on multiple predicates.",
            String.join("\n",
                "SELECT filename,",
                "  CASE WHEN file_size < 1000 THEN 'small'",
                "       WHEN file_size < 100000 THEN 'medium'",
                "       ELSE 'large' END AS bucket,",
                "  file_size / 1024 AS size_kb",
                "FROM docs",
                "WHERE filename LIKE '%.pdf'",
                "  AND classification IN ('public', 'internal')"),
            docsRows("[" +
                "{\"filename\":\"annual.pdf\",   \"classification\":\"public\",   \"file_size\":50000}," +
                "{\"filename\":\"invoice.pdf\",  \"classification\":\"internal\", \"file_size\":800}," +
                "{\"filename\":\"handbook.pdf\", \"classification\":\"public\",   \"file_size\":1500000}," +
                "{\"filename\":\"notes.txt\",    \"classification\":\"public\",   \"file_size\":200}" +
                "]"),
            docsTypeJson(), "docs"),

        example("03-aggregates", "GROUP BY + aggregates (SUM/COUNT/AVG/DISTINCT)",
            "Per-department totals and averages, plus COUNT(DISTINCT).",
            String.join("\n",
                "SELECT dept, COUNT(*) AS n, SUM(file_size) AS total,",
                "  AVG(file_size) AS mean,",
                "  COUNT(DISTINCT filename) AS distinct_files",
                "FROM docs",
                "GROUP BY dept",
                "ORDER BY total DESC"),
            docsRows("[" +
                "{\"filename\":\"a.pdf\",\"dept\":\"eng\",  \"file_size\":100}," +
                "{\"filename\":\"b.pdf\",\"dept\":\"eng\",  \"file_size\":200}," +
                "{\"filename\":\"b.pdf\",\"dept\":\"eng\",  \"file_size\":300}," +
                "{\"filename\":\"c.pdf\",\"dept\":\"sales\",\"file_size\":1000}," +
                "{\"filename\":\"d.pdf\",\"dept\":\"sales\",\"file_size\":2000}," +
                "{\"filename\":\"e.pdf\",\"dept\":\"finance\",\"file_size\":50}" +
                "]"),
            docsTypeJson(), "docs"),

        example("04-jpath", "JPATH — read undeclared fields",
            "Escape hatch for fields not on the type schema.",
            String.join("\n",
                "SELECT filename,",
                "  JPATH('metadata.experimental') AS experimental,",
                "  JPATH('metadata.ttl_seconds')  AS ttl",
                "FROM docs"),
            docsRows("[" +
                "{\"filename\":\"a.pdf\", \"metadata\":{\"experimental\":\"yes\",\"ttl_seconds\":42}}," +
                "{\"filename\":\"b.pdf\", \"metadata\":{\"experimental\":\"no\", \"ttl_seconds\":7}}," +
                "{\"filename\":\"c.pdf\"}" +
                "]"),
            docsTypeJson(), "docs"),

        example("05-mls", "MLS — multi-language accessor",
            "Pull specific language text from an MLS envelope; COALESCE for fallback.",
            String.join("\n",
                "SELECT filename,",
                "  MLS(content, 'en') AS body_en,",
                "  COALESCE(MLS(content, 'fr'), MLS(content, 'en'), '(no text)') AS fr_or_en",
                "FROM docs"),
            docsRows("[" +
                "{\"filename\":\"a.md\",\"content\":{\"mls\":[" +
                    "{\"lang\":\"en\",\"text\":\"Hello world\"}," +
                    "{\"lang\":\"fr\",\"text\":\"Bonjour le monde\"}]}}," +
                "{\"filename\":\"b.md\",\"content\":{\"mls\":[" +
                    "{\"lang\":\"en\",\"text\":\"Goodbye\"}]}}" +
                "]"),
            docsTypeJson(), "docs"),

        example("06-windowed", "Tumbling window aggregation",
            "Bucket events into 1-hour windows via WIN_START and aggregate.",
            String.join("\n",
                "SELECT WIN_START(event_time, 3600000) AS window_start,",
                "       dept, COUNT(*) AS n, SUM(file_size) AS total",
                "FROM   events",
                "GROUP BY WIN_START(event_time, 3600000), dept",
                "ORDER BY window_start, dept"),
            eventsRows("[" +
                "{\"dept\":\"eng\",  \"file_size\":100,\"event_time\":0}," +
                "{\"dept\":\"eng\",  \"file_size\":200,\"event_time\":900000}," +
                "{\"dept\":\"sales\",\"file_size\":300,\"event_time\":3900000}," +
                "{\"dept\":\"eng\",  \"file_size\":400,\"event_time\":7800000}" +
                "]"),
            eventsTypeJson(), "events"),

        example("07-setops", "UNION / EXCEPT / INTERSECT",
            "Set operations across selects.",
            String.join("\n",
                "SELECT filename FROM docs",
                "EXCEPT",
                "SELECT filename FROM docs WHERE filename LIKE '%.txt'"),
            docsRows("[" +
                "{\"filename\":\"a.pdf\"}," +
                "{\"filename\":\"b.pdf\"}," +
                "{\"filename\":\"notes.txt\"}" +
                "]"),
            docsTypeJson(), "docs")
    );

    // -- endpoints ------------------------------------------------------------

    /** List all available examples (id + title + description). */
    @GetMapping("/examples")
    @Operation(summary = "List preset SQL examples")
    public List<Map<String, String>> listExamples() {
        List<Map<String, String>> out = new ArrayList<>();
        for (Example e : examples) {
            out.add(Map.of("id", e.id(), "title", e.title(), "description", e.description()));
        }
        return out;
    }

    /** Fetch a full example. */
    @GetMapping("/examples/{id}")
    @Operation(summary = "Get one example's SQL, rows, and type")
    public ResponseEntity<Example> getExample(@PathVariable String id) {
        return examples.stream()
                .filter(e -> e.id().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Compile + execute an ad-hoc SQL query against inline JSON data. */
    @PostMapping("/execute")
    @Operation(summary = "Compile and execute SQL against inline data")
    public Map<String, Object> execute(@RequestBody ExecuteRequest req) {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            Type type = new Type();
            type.init(MAPPER.readTree(req.typeJson()));
            JsonNode rowsNode = MAPPER.readTree(req.rowsJson());
            if (!rowsNode.isArray()) {
                out.put("error", "input rows must be a JSON array");
                return out;
            }
            List<JVS> rows = new ArrayList<>(rowsNode.size());
            for (JsonNode r : rowsNode) rows.add(new JVS(r));

            String tableName = (req.tableName() == null || req.tableName().isBlank())
                    ? "docs" : req.tableName();

            var scBuilder = com.hitorro.jvssql.config.StreamConfig.builder();
            if (hasField(rowsNode, "event_time")) scBuilder.eventTimeField("event_time");
            JvsSqlEngine engine = JvsSqlEngine.builder()
                .registerStream(tableName, rows.iterator(), type, scBuilder.build())
                .build();

            long t0 = System.nanoTime();
            PreparedQuery q = engine.compile(req.sql());
            String plan = q.explain();
            Iterator<JsonNode> it = q.asIterator();
            List<JsonNode> results = new ArrayList<>();
            while (it.hasNext()) results.add(it.next());
            long dt = System.nanoTime() - t0;

            out.put("results", results);
            out.put("plan", plan);
            out.put("rowCount", results.size());
            out.put("elapsedMs", dt / 1_000_000.0);
        } catch (Exception e) {
            out.put("error", e.getMessage());
            out.put("errorType", e.getClass().getSimpleName());
        }
        return out;
    }

    // -- helpers --------------------------------------------------------------

    public record ExecuteRequest(String sql, String typeJson, String rowsJson, String tableName) {}

    private static Example example(String id, String title, String desc, String sql,
                                   String rows, String typeJson, String tableName) {
        return new Example(id, title, desc, sql, rows, typeJson, tableName);
    }

    private static String docsRows(String s) { return s; }
    private static String eventsRows(String s) { return s; }

    private static boolean hasField(JsonNode arr, String name) {
        if (!arr.isArray() || arr.isEmpty()) return false;
        return arr.get(0).has(name);
    }

    private static String docsTypeJson() {
        return "{\"name\":\"docs\",\"fields\":[" +
            "{\"name\":\"filename\",      \"type\":\"core_string\"}," +
            "{\"name\":\"classification\",\"type\":\"core_string\"}," +
            "{\"name\":\"file_size\",     \"type\":\"core_long\"}," +
            "{\"name\":\"dept\",          \"type\":\"core_string\"}," +
            "{\"name\":\"author\",        \"type\":\"core_string\"}," +
            "{\"name\":\"content\",       \"type\":\"core_string\"}" +
            "]}";
    }

    private static String eventsTypeJson() {
        return "{\"name\":\"events\",\"fields\":[" +
            "{\"name\":\"dept\",       \"type\":\"core_string\"}," +
            "{\"name\":\"file_size\",  \"type\":\"core_long\"}," +
            "{\"name\":\"event_time\", \"type\":\"core_long\"}" +
            "]}";
    }
}
