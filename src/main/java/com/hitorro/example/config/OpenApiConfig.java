/*
 * Copyright (c) 2006-2025 Chris Collins
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
package com.hitorro.example.config;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Iterator;
import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 * 
 * <p>Access the interactive API documentation at:
 * <ul>
 *   <li>Swagger UI: <a href="http://localhost:8080/swagger-ui.html">http://localhost:8080/swagger-ui.html</a></li>
 *   <li>OpenAPI JSON: <a href="http://localhost:8080/api-docs">http://localhost:8080/api-docs</a></li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    /**
     * swagger-core knows Jackson 2's {@code JsonNode} (documented as the free-form {@code JsonNode}
     * schema, {@code {}}) but not Jackson 3's, which it would document as a bean with the node's
     * getters ({@code array}, {@code empty}, {@code null}, ...). Keep the API docs as they were: any
     * Jackson 3 {@code JsonNode} is a reference to an empty {@code JsonNode} schema.
     */
    @Bean
    public ModelConverter jackson3JsonNodeSchema() {
        return new ModelConverter() {
            @Override
            @SuppressWarnings("rawtypes")
            public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
                Class<?> raw = type.getType() == null ? null : Json.mapper().constructType(type.getType()).getRawClass();
                if (raw != null && tools.jackson.databind.JsonNode.class.isAssignableFrom(raw)) {
                    context.defineModel("JsonNode", new Schema<>());
                    return new Schema<>().$ref("#/components/schemas/JsonNode");
                }
                return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
            }
        };
    }
    
    @Bean
    public OpenAPI hitorroExampleOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Hitorro Spring Boot Example API")
                .description("""
                    REST API for Hitorro Spring Boot integration example.
                    
                    Features demonstrated:
                    - Document Management System (DMS) operations
                    - JSON Type System (JVS) with NLP support
                    - Abstract File System (Local, JAR, S3-compatible)
                    - Command execution via REST
                    - Interactive CLI (Telnet/SSH)
                    
                    For more information, see the README.md file.
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("Hitorro")
                    .email("support@hitorro.com"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Local development server")
            ));
    }
}
