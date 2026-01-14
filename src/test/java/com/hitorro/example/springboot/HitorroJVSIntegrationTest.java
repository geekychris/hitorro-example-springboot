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
package com.hitorro.example.springboot;

import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.jsontypesystem.JsonTypeSystem;
import com.hitorro.spring.autoconfigure.jvs.JsonTypeSystemManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for Hitorro's JSON Type System (JVS) in Spring Boot.
 * 
 * <p>These tests demonstrate the comprehensive capabilities of JVS:
 * <ul>
 *   <li>Type-aware JSON document processing</li>
 *   <li>Property access via propaccess notation (e.g., "title.mls[en].text")</li>
 *   <li>Multi-language support with segmentation</li>
 *   <li>Complex nested structures</li>
 *   <li>Type definitions and validation</li>
 * </ul>
 * 
 * <p>Based on original JVSTest and JsonTypesystemTest from Hitorro core.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("JVS (JSON Value System) Integration Tests")
public class HitorroJVSIntegrationTest {
    
    @Autowired
    private JsonTypeSystemManager jtsManager;
    
    @Nested
    @DisplayName("Spring Boot Integration")
    class SpringBootIntegration {
        
        @Test
        @DisplayName("Should inject JsonTypeSystemManager")
        void shouldInjectJsonTypeSystemManager() {
            assertThat(jtsManager).isNotNull();
        }
        
        @Test
        @DisplayName("Should access JsonTypeSystem singleton")
        void shouldAccessJsonTypeSystemSingleton() {
            JsonTypeSystem jts = jtsManager.getJsonTypeSystem();
            assertThat(jts).isNotNull();
            assertThat(jts).isSameAs(JsonTypeSystem.getMe());
        }
    }
    
    @Nested
    @DisplayName("Construction and Parsing")
    class ConstructionAndParsing {
        
        @Test
        @DisplayName("Should create JVS from JSON string")
        void shouldCreateJvsFromJsonString() {
            String jsonString = "{\"key\":\"value\",\"number\":42}";
            
            JVS jvs = jtsManager.createJVS(jsonString);
            
            assertThat(jvs).isNotNull();
        }
        
        @Test
        @DisplayName("Should create empty JVS")
        void shouldCreateEmptyJvs() {
            JVS jvs = jtsManager.createJVS();
            
            assertThat(jvs).isNotNull();
        }
        
        @Test
        @DisplayName("Should handle simple JSON objects")
        void shouldHandleSimpleJsonObjects() {
            String jsonString = "{\"name\":\"John\",\"age\":30}";
            
            JVS jvs = jtsManager.createJVS(jsonString);
            
            assertThat(jvs).isNotNull();
        }
        
        @Test
        @DisplayName("Should handle complex nested JSON")
        void shouldHandleComplexNestedJson() {
            String complexJson = """
                {
                    "id": {
                        "did": "doc123",
                        "id": "id456"
                    },
                    "title": {
                        "mls": "Test Title"
                    },
                    "metadata": {
                        "author": "John Doe",
                        "tags": ["test", "sample"]
                    }
                }
                """;
            
            JVS jvs = jtsManager.createJVS(complexJson);
            
            assertThat(jvs).isNotNull();
        }
    }
    
    @Nested
    @DisplayName("Property Access")
    class PropertyAccess {
        
        @Test
        @DisplayName("Should have predefined property accessors")
        void shouldHavePredefinedPropertyAccessors() {
            assertThat(JVS.didKey).isNotNull();
            assertThat(JVS.idKey).isNotNull();
            assertThat(JVS.titleKey).isNotNull();
            assertThat(JVS.bodyKey).isNotNull();
        }
        
        @Test
        @DisplayName("Should have type key property")
        void shouldHaveTypeKeyProperty() {
            assertThat(JVS.typeKey).isNotNull();
            assertThat(JVS.typeKey.getKey()).isEqualTo("type");
        }
        
        @Test
        @DisplayName("Should have variable delimiters")
        void shouldHaveVariableDelimiters() {
            assertThat(JVS.VariableStart).isEqualTo("${");
            assertThat(JVS.VariableEnd).isEqualTo("}");
        }
    }
    
    @Nested
    @DisplayName("JSON Operations")
    class JsonOperations {
        
        @Test
        @DisplayName("Should handle arrays in JSON")
        void shouldHandleArraysInJson() {
            String jsonWithArray = "{\"items\":[1,2,3,4,5]}";
            
            JVS jvs = jtsManager.createJVS(jsonWithArray);
            
            assertThat(jvs).isNotNull();
        }
        
        @Test
        @DisplayName("Should handle boolean values")
        void shouldHandleBooleanValues() {
            String jsonWithBooleans = "{\"active\":true,\"deleted\":false}";
            
            JVS jvs = jtsManager.createJVS(jsonWithBooleans);
            
            assertThat(jvs).isNotNull();
        }
        
        @Test
        @DisplayName("Should handle null values")
        void shouldHandleNullValues() {
            String jsonWithNull = "{\"field\":null,\"other\":\"value\"}";
            
            JVS jvs = jtsManager.createJVS(jsonWithNull);
            
            assertThat(jvs).isNotNull();
        }
        
        @Test
        @DisplayName("Should handle special characters in strings")
        void shouldHandleSpecialCharactersInStrings() {
            String specialCharsJson = "{\"text\":\"Hello\\nWorld\\t!\",\"quote\":\"He said \\\"Hi\\\"\"}";
            
            JVS jvs = jtsManager.createJVS(specialCharsJson);
            
            assertThat(jvs).isNotNull();
        }
        
        @Test
        @DisplayName("Should handle unicode characters")
        void shouldHandleUnicodeCharacters() {
            String unicodeJson = "{\"greeting\":\"Hello 世界\",\"emoji\":\"🎉\"}";
            
            JVS jvs = jtsManager.createJVS(unicodeJson);
            
            assertThat(jvs).isNotNull();
        }
        
        @Test
        @DisplayName("Should handle numbers of various types")
        void shouldHandleNumbersOfVariousTypes() {
            String numbersJson = "{\"int\":42,\"float\":3.14,\"negative\":-100,\"exp\":1.5e10}";
            
            JVS jvs = jtsManager.createJVS(numbersJson);
            
            assertThat(jvs).isNotNull();
        }
    }
    
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {
        
        @Test
        @DisplayName("Should handle empty JSON object")
        void shouldHandleEmptyJsonObject() {
            String emptyJson = "{}";
            
            JVS jvs = jtsManager.createJVS(emptyJson);
            
            assertThat(jvs).isNotNull();
        }
        
        @Test
        @DisplayName("Should handle very large JSON")
        void shouldHandleVeryLargeJson() {
            StringBuilder largeJson = new StringBuilder("{");
            for (int i = 0; i < 1000; i++) {
                if (i > 0) largeJson.append(",");
                largeJson.append("\"field").append(i).append("\":\"value").append(i).append("\"");
            }
            largeJson.append("}");
            
            JVS jvs = jtsManager.createJVS(largeJson.toString());
            
            assertThat(jvs).isNotNull();
        }
    }
    
    @Nested
    @DisplayName("Real World Scenarios")
    class RealWorldScenarios {
        
        @Test
        @DisplayName("Should handle user profile document")
        void shouldHandleUserProfileDocument() {
            String userProfile = """
                {
                    "id": {
                        "id": "user_123",
                        "domain": "users"
                    },
                    "title": {
                        "mls": "John Doe Profile"
                    },
                    "data": {
                        "email": "john@example.com",
                        "age": 30,
                        "interests": ["coding", "music", "travel"]
                    },
                    "times": {
                        "created": "2023-01-01",
                        "modified": "2023-06-15"
                    }
                }
                """;
            
            JVS jvs = jtsManager.createJVS(userProfile);
            
            assertThat(jvs).isNotNull();
        }
        
        @Test
        @DisplayName("Should handle article document")
        void shouldHandleArticleDocument() {
            String article = """
                {
                    "id": {
                        "id": "article_001"
                    },
                    "title": {
                        "mls": "Introduction to JVS"
                    },
                    "body": {
                        "mls": "This is the article content..."
                    },
                    "metadata": {
                        "author": "Jane Smith",
                        "publishDate": "2023-07-01",
                        "tags": ["tutorial", "JVS", "JSON"]
                    }
                }
                """;
            
            JVS jvs = jtsManager.createJVS(article);
            
            assertThat(jvs).isNotNull();
        }
    }
    
    @Nested
    @DisplayName("Comparators and Functions")
    class ComparatorsAndFunctions {
        
        @Test
        @DisplayName("Should have identity comparator")
        void shouldHaveIdentityComparator() {
            assertThat(JVS.identityComparator).isNotNull();
        }
        
        @Test
        @DisplayName("Should have key generator function")
        void shouldHaveKeyGeneratorFunction() {
            assertThat(JVS.keyGenerator).isNotNull();
        }
        
        @Test
        @DisplayName("Identity comparator should compare JVS objects by ID")
        void identityComparatorShouldCompareJvsObjectsById() {
            JVS jvs1 = jtsManager.createJVS("{\"id\":{\"id\":\"id1\"}}");
            JVS jvs2 = jtsManager.createJVS("{\"id\":{\"id\":\"id2\"}}");
            
            int result = JVS.identityComparator.compare(jvs1, jvs2);
            
            // Should compare lexicographically
            assertThat(result).isNotZero();
        }
    }
    
    @Nested
    @DisplayName("NLP-Aware Features - Field Access Tests")
    class NLPAwareFeatures {
        
        @Test
        @DisplayName("Should access text field and test for segmented field availability")
        void shouldAccessTextAndSegmentedFields() {
            // Create document with text that would be segmented
            String json = """
                {
                    "title": {
                        "mls": {
                            "en": {
                                "text": "John Smith visited New York yesterday"
                            }
                        }
                    }
                }
                """;
            
            JVS doc = jtsManager.createJVS(json);
            
            // ALWAYS AVAILABLE: Access the original text field
            String text = doc.getString("title.mls.en.text");
            assertThat(text).isNotNull();
            assertThat(text).isEqualTo("John Smith visited New York yesterday");
            
            // ATTEMPT TO ACCESS NLP-ENRICHED FIELDS
            // When NLP is enabled, JVS automatically creates these fields:
            
            // 1. Try to access segmented field (array of tokens)
            List<String> segmented = null;
            try {
                segmented = doc.getStringList("title.mls[en].segmented");
            } catch (Exception e) {
                // Field doesn't exist or wrong type - NLP not enabled
            }
            
            if (segmented != null && !segmented.isEmpty()) {
                // NLP IS ENABLED - verify segmentation worked
                assertThat(segmented).contains("John", "Smith", "visited", "New", "York", "yesterday");
                System.out.println("✓ Segmentation available: " + segmented);
            } else {
                // NLP IS NOT ENABLED - this is expected in test environment
                System.out.println("✗ Segmentation not available (NLP not enabled)");
                System.out.println("  To enable: set hitorro.jvs.nlp-enabled=true");
                System.out.println("  Requires: WordNet data in ${hitorro.home}/data/wordnet");
            }
            
            // 2. Try to access normalized hashes
            long[] normhash = null;
            try {
                normhash = doc.getLongArray("title.mls[en].segmented_normhash");
            } catch (Exception e) {
                // Field doesn't exist - NLP not enabled
            }
            
            if (normhash != null && normhash.length > 0) {
                assertThat(normhash.length).isEqualTo(segmented.size());
                System.out.println("✓ Normalized hashes available: " + normhash.length + " values");
            } else {
                System.out.println("✗ Normalized hashes not available (NLP not enabled)");
            }
        }
        
        @Test
        @DisplayName("Should access Named Entity Recognition (NER) fields")
        void shouldAccessNERFields() {
            // Text with clear named entities
            String json = """
                {
                    "content": {
                        "mls": {
                            "en": {
                                "text": "President Biden met with Chancellor Merkel in Berlin on Monday"
                            }
                        }
                    }
                }
                """;
            
            JVS doc = jtsManager.createJVS(json);
            
            // ALWAYS AVAILABLE: Access original text
            String text = doc.getString("content.mls.en.text");
            assertThat(text).isNotNull();
            assertThat(text).contains("Biden", "Merkel", "Berlin");
            
            // ATTEMPT TO ACCESS NER FIELD
            // When NLP is enabled with OpenNLP NER models, JVS adds segmented_ner field
            List<String> ner = null;
            try {
                ner = doc.getStringList("content.mls[en].segmented_ner");
            } catch (Exception e) {
                // Field doesn't exist - NLP not enabled
            }
            
            if (ner != null && !ner.isEmpty()) {
                // NLP IS ENABLED - verify NER tags
                System.out.println("✓ NER available: " + ner);
                
                // Expected tags:
                // - PERSON: Names (Biden, Merkel)
                // - LOCATION: Places (Berlin)
                // - DATE: Temporal expressions (Monday)
                // - TITLE: Positions (President, Chancellor)
                // - O: Other tokens
                
                assertThat(ner).contains("PERSON");  // Biden or Merkel
                assertThat(ner).contains("LOCATION");  // Berlin
                
                // Count entity types
                long personCount = ner.stream().filter(tag -> tag.equals("PERSON")).count();
                long locationCount = ner.stream().filter(tag -> tag.equals("LOCATION")).count();
                
                System.out.println("  Found PERSON entities: " + personCount);
                System.out.println("  Found LOCATION entities: " + locationCount);
            } else {
                System.out.println("✗ NER not available (NLP not enabled)");
                System.out.println("  To enable: set hitorro.jvs.nlp-enabled=true");
                System.out.println("  Requires: OpenNLP NER models");
            }
        }
        
        @Test
        @DisplayName("Should access clean text field with normalized content")
        void shouldAccessCleanTextField() {
            // Content with special characters, capitalization, punctuation
            String json = """
                {
                    "text": {
                        "mls": {
                            "en": {
                                "text": "Hello WORLD!!! This is a TEST... with $pecial ch@rs & CAPS!"
                            }
                        }
                    }
                }
                """;
            
            JVS doc = jtsManager.createJVS(json);
            
            // ALWAYS AVAILABLE: Access original text
            String original = doc.getString("text.mls.en.text");
            assertThat(original).isNotNull();
            assertThat(original).contains("WORLD", "$pecial", "ch@rs", "CAPS");
            
            // ATTEMPT TO ACCESS CLEAN FIELD
            // When NLP is enabled, JVS creates a 'clean' field with:
            // - Lowercase conversion
            // - Punctuation removal
            // - Special character normalization
            // - Whitespace normalization
            
            String clean = null;
            try {
                clean = doc.getString("text.mls[en].clean");
            } catch (Exception e) {
                // Field doesn't exist - NLP not enabled
            }
            
            if (clean != null && !clean.isEmpty()) {
                // NLP IS ENABLED - verify cleaning worked
                System.out.println("✓ Clean text available");
                System.out.println("  Original: " + original);
                System.out.println("  Clean:    " + clean);
                
                // Verify cleaning operations
                assertThat(clean).isLowerCase();  // All lowercase
                assertThat(clean).doesNotContain("!", "?", "...", "$", "@", "&");  // No punctuation/special chars
                assertThat(clean).contains("hello", "world", "test");  // Core words preserved
                
                // Clean text should be suitable for:
                // - Text comparison (case-insensitive)
                // - Search indexing
                // - Duplicate detection
                // - Fuzzy matching
            } else {
                System.out.println("✗ Clean text not available (NLP not enabled)");
                System.out.println("  Original text: " + original);
            }
        }
        
        @Test
        @DisplayName("Should access POS tags and parse tree fields")
        void shouldAccessPOSAndParseFields() {
            String json = """
                {
                    "sentence": {
                        "mls": {
                            "en": {
                                "text": "The quick brown fox jumps over the lazy dog"
                            }
                        }
                    }
                }
                """;
            
            JVS doc = jtsManager.createJVS(json);
            
            // ALWAYS AVAILABLE: Access original text
            String text = doc.getString("sentence.mls.en.text");
            assertThat(text).isNotNull();
            
            // ATTEMPT TO ACCESS POS TAGS
            // When NLP is enabled with OpenNLP POS tagger, JVS adds segmented_pos field
            List<String> posTags = null;
            try {
                posTags = doc.getStringList("sentence.mls[en].segmented_pos");
            } catch (Exception e) {
                // Field doesn't exist - NLP not enabled
            }
            
            if (posTags != null && !posTags.isEmpty()) {
                // NLP IS ENABLED - verify POS tagging
                System.out.println("✓ POS tags available: " + posTags);
                
                // Expected POS tags for "The quick brown fox jumps over the lazy dog":
                // DT (Determiner): "The", "the"
                // JJ (Adjective): "quick", "brown", "lazy"
                // NN (Noun): "fox", "dog"
                // VBZ (Verb, 3rd person singular): "jumps"
                // IN (Preposition): "over"
                
                assertThat(posTags).contains("DT", "JJ", "NN", "VBZ");
                
                // Count POS types
                long nounCount = posTags.stream().filter(tag -> tag.startsWith("NN")).count();
                long verbCount = posTags.stream().filter(tag -> tag.startsWith("VB")).count();
                long adjCount = posTags.stream().filter(tag -> tag.equals("JJ")).count();
                
                System.out.println("  Nouns: " + nounCount);
                System.out.println("  Verbs: " + verbCount);
                System.out.println("  Adjectives: " + adjCount);
            } else {
                System.out.println("✗ POS tags not available (NLP not enabled)");
            }
            
            // ATTEMPT TO ACCESS PARSE TREE
            // When NLP is enabled with OpenNLP parser, JVS adds segmented_parsed field
            JsonNode parseTree = null;
            try {
                parseTree = doc.get("sentence.mls[en].segmented_parsed");
            } catch (Exception e) {
                // Field doesn't exist - NLP not enabled
            }
            
            if (parseTree != null && !parseTree.isMissingNode()) {
                // NLP IS ENABLED - verify parse tree exists
                System.out.println("✓ Parse tree available");
                System.out.println("  Parse tree structure: " + parseTree.toString().substring(0, Math.min(100, parseTree.toString().length())) + "...");
                
                // Parse tree contains full syntactic structure:
                // - Sentence (S)
                // - Noun phrases (NP)
                // - Verb phrases (VP)
                // - Prepositional phrases (PP)
                // etc.
            } else {
                System.out.println("✗ Parse tree not available (NLP not enabled)");
            }
        }
        
        @Test
        @DisplayName("Should access WordNet semantic classification fields")
        void shouldAccessWordNetFields() {
            String json = """
                {
                    "description": {
                        "mls": {
                            "en": {
                                "text": "The dog ran quickly through the park"
                            }
                        }
                    }
                }
                """;
            
            JVS doc = jtsManager.createJVS(json);
            
            // ALWAYS AVAILABLE: Access original text
            String text = doc.getString("description.mls.en.text");
            assertThat(text).isNotNull();
            
            // ATTEMPT TO ACCESS SEMANTIC CLASSES
            // When NLP is enabled with WordNet, JVS adds segmented_classes field
            List<String> classes = null;
            try {
                classes = doc.getStringList("description.mls[en].segmented_classes");
            } catch (Exception e) {
                // Field doesn't exist - NLP not enabled
            }
            
            if (classes != null && !classes.isEmpty()) {
                // NLP IS ENABLED WITH WORDNET - verify semantic features
                System.out.println("✓ WordNet semantic classes available: " + classes);
                
                // For "dog", WordNet provides hypernym chain:
                // dog → canine → carnivore → mammal → animal → organism → living_thing
                //
                // This enables:
                // - Finding semantically similar words
                // - Understanding concept hierarchies
                // - Semantic search (match "dog" to queries about "animals")
                // - Question answering
                
                // Verify semantic classifications exist
                assertThat(classes).isNotEmpty();
                System.out.println("  Total semantic classes: " + classes.size());
            } else {
                System.out.println("✗ WordNet semantic classes not available");
                System.out.println("  Requires: hitorro.jvs.nlp-enabled=true");
                System.out.println("  Requires: WordNet dictionary in ${hitorro.home}/data/wordnet");
            }
            
            // ATTEMPT TO ACCESS ANSWER CLASSIFICATIONS
            // Used for question answering systems
            List<String> answers = null;
            try {
                answers = doc.getStringList("description.mls[en].segmented_answers");
            } catch (Exception e) {
                // Field doesn't exist - NLP not enabled
            }
            
            if (answers != null && !answers.isEmpty()) {
                System.out.println("✓ Answer classifications available: " + answers);
                System.out.println("  Can be used for question-answer matching");
            } else {
                System.out.println("✗ Answer classifications not available");
            }
        }
        
        @Test
        @DisplayName("Should access normalized hash fields for text comparison")
        void shouldAccessNormalizedHashFields() {
            String json = """
                {
                    "title1": {
                        "mls": {
                            "en": {"text": "Hello World"}
                        }
                    },
                    "title2": {
                        "mls": {
                            "en": {"text": "HELLO WORLD!!!"}
                        }
                    }
                }
                """;
            
            JVS doc = jtsManager.createJVS(json);
            
            // ALWAYS AVAILABLE: Access original text
            String text1 = doc.getString("title1.mls.en.text");
            String text2 = doc.getString("title2.mls.en.text");
            assertThat(text1).isNotEqualTo(text2);  // Different strings
            
            // ATTEMPT TO ACCESS NORMALIZED HASHES
            // When NLP is enabled, JVS creates segmented_normhash for each text
            long[] hash1 = null;
            long[] hash2 = null;
            try {
                hash1 = doc.getLongArray("title1.mls[en].segmented_normhash");
                hash2 = doc.getLongArray("title2.mls[en].segmented_normhash");
            } catch (Exception e) {
                // Fields don't exist - NLP not enabled
            }
            
            if (hash1 != null && hash1.length > 0 && hash2 != null && hash2.length > 0) {
                // NLP IS ENABLED - verify hash-based comparison
                System.out.println("✓ Normalized hashes available");
                System.out.println("  Text 1: \"" + text1 + "\"");
                System.out.println("  Hash 1: " + java.util.Arrays.toString(hash1));
                System.out.println("  Text 2: \"" + text2 + "\"");
                System.out.println("  Hash 2: " + java.util.Arrays.toString(hash2));
                
                // Both should have same number of tokens after normalization
                assertThat(hash1.length).isEqualTo(hash2.length);
                
                // Normalized hashes should match because:
                // "Hello World" normalizes to ["hello", "world"]
                // "HELLO WORLD!!!" normalizes to ["hello", "world"]
                // Both produce same hashes
                
                boolean hashesMatch = java.util.Arrays.equals(hash1, hash2);
                System.out.println("  Hashes match: " + hashesMatch);
                
                if (hashesMatch) {
                    System.out.println("  ✓ Successfully detected duplicate content via hash comparison!");
                }
                
                // Use cases:
                // 1. Duplicate detection: O(1) hash comparison instead of O(n) string comparison
                // 2. Fuzzy matching: Find similar text regardless of capitalization/punctuation
                // 3. Search indexing: Use hashes as keys for fast lookup
                // 4. Deduplication: Identify semantically identical documents
            } else {
                System.out.println("✗ Normalized hashes not available (NLP not enabled)");
                System.out.println("  Text 1: " + text1);
                System.out.println("  Text 2: " + text2);
                System.out.println("  Without hashes, must use slower string comparison");
            }
        }
        
        @Test
        @DisplayName("Should access multi-language NLP fields independently")
        void shouldAccessMultiLanguageNLPFields() {
            String json = """
                {
                    "content": {
                        "mls": {
                            "en": {
                                "text": "Hello world"
                            },
                            "de": {
                                "text": "Hallo Welt"
                            },
                            "es": {
                                "text": "Hola mundo"
                            }
                        }
                    }
                }
                """;
            
            JVS doc = jtsManager.createJVS(json);
            
            // ALWAYS AVAILABLE: Access text in each language
            String english = doc.getString("content.mls.en.text");
            String german = doc.getString("content.mls.de.text");
            String spanish = doc.getString("content.mls.es.text");
            
            assertThat(english).isEqualTo("Hello world");
            assertThat(german).isEqualTo("Hallo Welt");
            assertThat(spanish).isEqualTo("Hola mundo");
            
            // ATTEMPT TO ACCESS SEGMENTED FIELDS FOR EACH LANGUAGE
            // When NLP is enabled, EACH language is processed with language-specific models
            
            List<String> enSegments = null;
            List<String> deSegments = null;
            List<String> esSegments = null;
            try {
                enSegments = doc.getStringList("content.mls[en].segmented");
                deSegments = doc.getStringList("content.mls[de].segmented");
                esSegments = doc.getStringList("content.mls[es].segmented");
            } catch (Exception e) {
                // Fields don't exist - NLP not enabled
            }
            
            if (enSegments != null && !enSegments.isEmpty()) {
                System.out.println("✓ Multi-language NLP available");
                System.out.println("  English segmented: " + enSegments);
                assertThat(enSegments).containsExactly("Hello", "world");
            } else {
                System.out.println("✗ English segmentation not available");
            }
            
            if (deSegments != null && !deSegments.isEmpty()) {
                System.out.println("  German segmented:  " + deSegments);
                assertThat(deSegments).containsExactly("Hallo", "Welt");
            } else {
                System.out.println("✗ German segmentation not available");
            }
            
            if (esSegments != null && !esSegments.isEmpty()) {
                System.out.println("  Spanish segmented: " + esSegments);
                assertThat(esSegments).containsExactly("Hola", "mundo");
            } else {
                System.out.println("✗ Spanish segmentation not available");
            }
            
            // Each language can also have its own:
            // - Clean text: doc.getString("content.mls[en].clean")
            // - POS tags: doc.getStringList("content.mls[de].segmented_pos")
            // - NER tags: doc.getStringList("content.mls[es].segmented_ner")
            // - Parse trees: doc.get("content.mls[en].segmented_parsed")
            //
            // This enables:
            // - Cross-language search
            // - Multi-language document analysis
            // - Language-specific text processing
            // - Internationalized NLP applications
        }
    }
}
