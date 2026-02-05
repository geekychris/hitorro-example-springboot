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
package com.hitorro.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.spring.autoconfigure.jvs.JsonTypeSystemManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

/**
 * Test JVS MLS bracket notation setter with full Spring context.
 * 
 * This test verifies that:
 * 1. JVS.set("field.mls[lang].text", value) actually works
 * 2. The resulting JSON structure is correct
 * 3. Values can be retrieved after setting
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("JVS MLS Setter Integration Test")
public class JVSMLSSetterTest {
    
    @Autowired
    private JsonTypeSystemManager jtsManager;
    
    @BeforeEach
    void setUp() {
        assertThat(jtsManager).isNotNull();
    }
    
    @Test
    @DisplayName("Should set MLS field using bracket notation")
    void shouldSetMLSFieldUsingBracketNotation() throws Exception {
        String json = """
            {
                "type": "article",
                "title": {
                    "mls": [
                        {
                            "lang": "en",
                            "text": "Hello World"
                        }
                    ]
                }
            }
            """;
        
        JVS jvs = jtsManager.createJVS(json);
        
        System.out.println("\n=== Initial JSON ===");
        System.out.println(jvs.getJsonNode().toPrettyString());
        
        // Try to set German translation
        System.out.println("\n=== Setting title.mls[de].text = 'Hallo Welt' ===");
        jvs.set("title.mls[de].text", "Hallo Welt");
        
        System.out.println("\n=== After set() call ===");
        System.out.println(jvs.getJsonNode().toPrettyString());
        
        // Check if it was actually set
        String germanText = jvs.getString("title.mls[de].text");
        System.out.println("\n=== Retrieved value ===");
        System.out.println("  title.mls[de].text: " + germanText);
        
        if (germanText == null) {
            System.out.println("\n❌ JVS.set() did not create the German translation!");
            System.out.println("   This indicates the MLS bracket notation setter is not working.");
            
            // Let's inspect the JSON structure to see what happened
            JsonNode titleNode = jvs.getJsonNode().get("title");
            if (titleNode != null) {
                System.out.println("\n   Title node structure:");
                System.out.println(titleNode.toPrettyString());
                
                JsonNode mlsNode = titleNode.get("mls");
                if (mlsNode != null && mlsNode.isArray()) {
                    System.out.println("\n   MLS array has " + mlsNode.size() + " entries:");
                    for (int i = 0; i < mlsNode.size(); i++) {
                        JsonNode entry = mlsNode.get(i);
                        System.out.println("     [" + i + "]: lang=" + entry.get("lang") + 
                                         ", text=" + entry.get("text"));
                    }
                }
            }
        }
        
        assertThat(germanText)
            .as("German translation should be set and retrievable")
            .isNotNull()
            .isEqualTo("Hallo Welt");
    }
    
    @Test
    @DisplayName("Should set multiple MLS translations")
    void shouldSetMultipleMLSTranslations() throws Exception {
        String json = """
            {
                "type": "article",
                "title": {
                    "mls": [
                        {
                            "lang": "en",
                            "text": "Hello World"
                        }
                    ]
                }
            }
            """;
        
        JVS jvs = jtsManager.createJVS(json);
        
        System.out.println("\n=== Setting multiple translations ===");
        
        // Set multiple languages
        jvs.set("title.mls[de].text", "Hallo Welt");
        jvs.set("title.mls[es].text", "Hola Mundo");
        jvs.set("title.mls[fr].text", "Bonjour Monde");
        
        System.out.println("\n=== Final JSON ===");
        System.out.println(jvs.getJsonNode().toPrettyString());
        
        // Verify all translations
        String en = jvs.getString("title.mls[en].text");
        String de = jvs.getString("title.mls[de].text");
        String es = jvs.getString("title.mls[es].text");
        String fr = jvs.getString("title.mls[fr].text");
        
        System.out.println("\n=== Retrieved translations ===");
        System.out.println("  [en]: " + en);
        System.out.println("  [de]: " + de);
        System.out.println("  [es]: " + es);
        System.out.println("  [fr]: " + fr);
        
        assertThat(en).isEqualTo("Hello World");
        assertThat(de).isEqualTo("Hallo Welt");
        assertThat(es).isEqualTo("Hola Mundo");
        assertThat(fr).isEqualTo("Bonjour Monde");
    }
    
    @Test
    @DisplayName("Should work with mock translation service")
    void shouldWorkWithMockTranslationService() throws Exception {
        String json = """
            {
                "type": "article",
                "title": {
                    "mls": [
                        {
                            "lang": "en",
                            "text": "Hello World",
                            "clean": "Hello World"
                        }
                    ]
                },
                "description": {
                    "mls": [
                        {
                            "lang": "en",
                            "text": "This is a test",
                            "clean": "This is a test"
                        }
                    ]
                }
            }
            """;
        
        JVS jvs = jtsManager.createJVS(json);
        
        System.out.println("\n=== Mock Translation Test ===");
        System.out.println("Initial state:");
        System.out.println(jvs.getJsonNode().toPrettyString());
        
        // Simulate what MLSTranslationService does
        String[] fields = {"title", "description"};
        String[] targetLanguages = {"de", "es"};
        
        // Mock translations (in real service, these come from AI)
        String[][] mockTranslations = {
            {"Hallo Welt", "Hola Mundo"},  // title translations
            {"Das ist ein Test", "Esto es una prueba"}  // description translations
        };
        
        System.out.println("\n=== Applying mock translations ===");
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            
            for (int j = 0; j < targetLanguages.length; j++) {
                String lang = targetLanguages[j];
                String translation = mockTranslations[i][j];
                
                String path = field + ".mls[" + lang + "].text";
                System.out.println("  Setting: " + path + " = \"" + translation + "\"");
                jvs.set(path, translation);
            }
        }
        
        System.out.println("\n=== After mock translations ===");
        System.out.println(jvs.getJsonNode().toPrettyString());
        
        // Verify all translations were set
        System.out.println("\n=== Verification ===");
        for (String field : fields) {
            System.out.println("\n" + field + ":");
            System.out.println("  [en]: " + jvs.getString(field + ".mls[en].text"));
            
            for (String lang : targetLanguages) {
                String value = jvs.getString(field + ".mls[" + lang + "].text");
                System.out.println("  [" + lang + "]: " + value);
                
                assertThat(value)
                    .as(field + " should have " + lang + " translation")
                    .isNotNull()
                    .isNotEmpty();
            }
        }
    }
    
    @Test
    @DisplayName("Should read existing MLS values correctly")
    void shouldReadExistingMLSValues() throws Exception {
        String json = """
            {
                "type": "article",
                "title": {
                    "mls": [
                        {
                            "lang": "en",
                            "text": "Hello World",
                            "clean": "Hello World"
                        },
                        {
                            "lang": "de",
                            "text": "Hallo Welt"
                        }
                    ]
                }
            }
            """;
        
        JVS jvs = jtsManager.createJVS(json);
        
        System.out.println("\n=== Reading existing MLS values ===");
        
        String enText = jvs.getString("title.mls[en].text");
        String enClean = jvs.getString("title.mls[en].clean");
        String deText = jvs.getString("title.mls[de].text");
        
        System.out.println("  title.mls[en].text: " + enText);
        System.out.println("  title.mls[en].clean: " + enClean);
        System.out.println("  title.mls[de].text: " + deText);
        
        assertThat(enText).isEqualTo("Hello World");
        assertThat(enClean).isEqualTo("Hello World");
        assertThat(deText).isEqualTo("Hallo Welt");
        
        System.out.println("  ✓ Reading works correctly!");
    }
}
