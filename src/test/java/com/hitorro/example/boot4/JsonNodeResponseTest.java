package com.hitorro.example.boot4;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import com.hitorro.example.config.H2ConsoleConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HiTorro 4.0.0 and Spring Boot 4 are both on Jackson 3, and the app no longer uses the Jackson 2
 * bridge (preferred-json-mapper=jackson2 is gone). These tests prove JsonNode, ObjectNode, and maps
 * holding JsonNodes (the controllers' usual {@code Map<String, Object>} response) come out as plain
 * JSON through Boot's Jackson 3 mapper, which hitorro-spring-boot-starter gives the Jackson 2 format,
 * through the app's configuration and security filter chain.
 */
@WebMvcTest(JsonNodeResponseTest.JsonNodeController.class)
@Import({JsonNodeResponseTest.JsonNodeController.class, H2ConsoleConfig.class})
class JsonNodeResponseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Test
    void jackson2BridgeIsGoneAndBootsMapperHasTheJackson2Format() {
        assertNull(environment.getProperty("spring.http.converters.preferred-json-mapper"));
        assertEquals("true", environment.getProperty("spring.jackson.use-jackson2-defaults"),
                "the starter's default (HitorroEnvironmentPostProcessor)");
    }

    @Test
    void objectNodeIsPlainJson() throws Exception {
        mockMvc.perform(get("/api/boot4-test/object-node").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"type\":\"sysobject\",\"id\":{\"did\":\"d1\",\"domain\":\"demo\"},"
                                + "\"title\":{\"mls\":[{\"lang\":\"en\",\"text\":\"Hello\"}]},\"rank\":2.5}",
                        JsonCompareMode.STRICT));
    }

    @Test
    void mapHoldingJsonNodeIsPlainJson() throws Exception {
        mockMvc.perform(get("/api/boot4-test/map").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"status\":\"success\",\"document\":{\"a\":[1,2,{\"b\":null}]}}",
                        JsonCompareMode.STRICT));
    }

    @RestController
    static class JsonNodeController {

        private final ObjectMapper mapper = com.hitorro.util.json.JsonMappers.json();

        @GetMapping("/api/boot4-test/object-node")
        ObjectNode objectNode() {
            ObjectNode node = mapper.createObjectNode();
            node.put("type", "sysobject");
            node.putObject("id").put("did", "d1").put("domain", "demo");
            node.putObject("title").putArray("mls").addObject().put("lang", "en").put("text", "Hello");
            node.put("rank", 2.5);
            return node;
        }

        @GetMapping("/api/boot4-test/map")
        Map<String, Object> map() throws Exception {
            JsonNode document = mapper.readTree("{\"a\":[1,2,{\"b\":null}]}");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", "success");
            body.put("document", document);
            return body;
        }
    }
}
