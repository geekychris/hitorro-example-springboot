package com.hitorro.example;

import com.hitorro.index.config.LuceneFieldType;
import com.hitorro.index.config.LuceneFieldTypes;
import com.hitorro.index.indexer.LuceneExecutionBuilderMapper;
import com.hitorro.jsontypesystem.Field;
import com.hitorro.jsontypesystem.Group;
import com.hitorro.jsontypesystem.JsonTypeSystem;
import com.hitorro.jsontypesystem.Type;
import com.hitorro.jsontypesystem.executors.ExecutionBuilder;
import com.hitorro.util.core.events.cache.HashCache;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collection;

/**
 * Diagnostic test to understand type system integration with Lucene indexing.
 * 
 * Run this test with breakpoints to inspect:
 * 1. Whether fields have groups configured
 * 2. Whether field types are loaded
 * 3. Whether ExecutionBuilder creates actions
 * 
 * This is NOT a passing/failing test - it's for manual debugging.
 */
@SpringBootTest
public class TypeSystemDiagnosticTest {

    @Test
    public void diagnoseTypeSystemIntegration() {
        System.out.println("\n========================================");
        System.out.println("TYPE SYSTEM DIAGNOSTIC TEST");
        System.out.println("========================================\n");
        
        // TEST 1: Check if type exists
        System.out.println("TEST 1: Type System");
        System.out.println("-------------------");
        Type productType = JsonTypeSystem.getMe().getType("demo_product");
        System.out.println("demo_product type exists: " + (productType != null));
        
        if (productType == null) {
            System.out.println("❌ FAILED: Type not found!");
            return;
        }
        
        System.out.println("Type name: " + productType.getName());
        System.out.println("✅ Type exists\n");
        
        // TEST 2: Check fields
        System.out.println("TEST 2: Field Configuration");
        System.out.println("---------------------------");
        
        Field brandField = productType.getField("brand");
        System.out.println("brand field exists: " + (brandField != null));
        
        if (brandField != null) {
            System.out.println("  - Field type: " + brandField.getType());
            
            // **KEY TEST**: Check if field has "index" group
            // PUT BREAKPOINT HERE to inspect groups
            Collection<Group> indexGroups = brandField.getGroup("index");
            System.out.println("  - 'index' groups: " + (indexGroups != null ? indexGroups.size() : "null"));
            
            if (indexGroups != null && !indexGroups.isEmpty()) {
                System.out.println("  ✅ Field HAS index groups:");
                for (Group g : indexGroups) {
                    System.out.println("     - Group name: " + g.getName());
                    System.out.println("       Method: " + g.getMethod());
                }
            } else {
                System.out.println("  ❌ Field has NO index groups!");
                System.out.println("     This is likely why fields aren't being indexed.");
            }
        }
        
        Field skuField = productType.getField("sku");
        System.out.println("\nsku field exists: " + (skuField != null));
        if (skuField != null) {
            Collection<Group> skuIndexGroups = skuField.getGroup("index");
            System.out.println("  - 'index' groups: " + (skuIndexGroups != null ? skuIndexGroups.size() : "null"));
        }
        
        Field priceField = productType.getField("price");
        System.out.println("\nprice field exists: " + (priceField != null));
        if (priceField != null) {
            Collection<Group> priceIndexGroups = priceField.getGroup("index");
            System.out.println("  - 'index' groups: " + (priceIndexGroups != null ? priceIndexGroups.size() : "null"));
        }
        
        System.out.println();
        
        // TEST 3: Check field types configuration
        System.out.println("TEST 3: Lucene Field Types Configuration");
        System.out.println("----------------------------------------");
        
        LuceneFieldTypes lfts = LuceneFieldTypes.getInstance();
        
        LuceneFieldType identifierType = lfts.get("identifier");
        System.out.println("'identifier' type loaded: " + (identifierType != null));
        if (identifierType != null) {
            System.out.println("  - indexed: " + identifierType.isIndexed());
            System.out.println("  - stored: " + identifierType.isStored());
            System.out.println("  - tokenized: " + identifierType.isTokenized());
            System.out.println("  - indexType: " + identifierType.getIndexType());
        }
        
        LuceneFieldType textType = lfts.get("text");
        System.out.println("\n'text' type loaded: " + (textType != null));
        
        LuceneFieldType longType = lfts.get("long");
        System.out.println("'long' type loaded: " + (longType != null));
        
        if (identifierType != null && textType != null && longType != null) {
            System.out.println("✅ All field types loaded\n");
        } else {
            System.out.println("❌ Some field types missing!\n");
        }
        
        // TEST 4: Check ExecutionBuilder
        System.out.println("TEST 4: ExecutionBuilder Creation");
        System.out.println("---------------------------------");
        
        try {
            HashCache<Type, ExecutionBuilder> cache = 
                Type.getExecBuilderCache("lucene", LuceneExecutionBuilderMapper.me);
            
            // PUT BREAKPOINT HERE to inspect builder
            ExecutionBuilder builder = cache.get(productType);
            
            System.out.println("ExecutionBuilder created: " + (builder != null));
            
            if (builder != null) {
                System.out.println("  - Builder class: " + builder.getClass().getName());
                
                // The builder should have actions for each field with index groups
                // To inspect: put breakpoint in LuceneIndexerAction constructor
                // and see if it's called when this builder is used
                
                System.out.println("✅ ExecutionBuilder exists");
                System.out.println("\n💡 TIP: Put breakpoint in LuceneIndexerAction constructor");
                System.out.println("   and index a document to see which fields get actions created.");
            } else {
                System.out.println("❌ ExecutionBuilder is null!");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Error creating ExecutionBuilder:");
            e.printStackTrace();
        }
        
        System.out.println("\n========================================");
        System.out.println("DIAGNOSTIC COMPLETE");
        System.out.println("========================================\n");
        
        System.out.println("SUMMARY:");
        System.out.println("--------");
        System.out.println("If you see 'Field has NO index groups', that's the root cause!");
        System.out.println("Fields need to have groups configured in their type definition.");
        System.out.println("\nNext steps:");
        System.out.println("1. Check the demo_product type JSON file for group definitions");
        System.out.println("2. Or put breakpoint in LuceneIndexerAction constructor");
        System.out.println("3. Index a demo_product document and see if constructor is called");
    }
}
