package com.hitorro.example.boot4;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring Boot 4 writes HTTP JSON with Jackson 3 by default, which would write a Jackson 2
 * (com.fasterxml.jackson) JsonNode as a dump of its getters. The app sets
 * spring.http.converters.preferred-json-mapper=jackson2 (application.yml); these tests
 * prove JsonNode, ObjectNode, and maps holding JsonNodes (the controllers' usual
 * {@code Map<String, Object>} response) come out as plain JSON, through the app's
 * configuration and security filter chain.
 */
@WebMvcTest(JsonNodeResponseTest.JsonNodeController.class)
@Import({JsonNodeResponseTest.JsonNodeController.class, H2ConsoleConfig.class})
class JsonNodeResponseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Test
    void applicationPrefersJackson2() {
        assertEquals("jackson2", environment.getProperty("spring.http.converters.preferred-json-mapper"));
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

        private final ObjectMapper mapper = new ObjectMapper();

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
