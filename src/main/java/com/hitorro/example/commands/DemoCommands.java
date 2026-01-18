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
package com.hitorro.example.commands;

import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.commandandcontrol.ano.DebugArgAno;
import com.hitorro.util.commandandcontrol.responsemappings.MapMapping;
import com.hitorro.util.core.GenericKeyValue;
import com.hitorro.util.json.keys.IntegerProperty;
import com.hitorro.util.json.keys.StringProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Demonstration commands showcasing proper Hitorro @CommandDef functionality.
 * 
 * These commands are automatically discovered by the CommandDefScanner during
 * Spring Boot startup and registered with the Hitorro CommandRegistry.
 * 
 * Key points:
 * - @Component makes this a Spring bean so it's scanned
 * - @CommandDef annotation marks methods as commands
 * - @DebugArgAno annotation on parameters for proper Hitorro integration
 * - propType specifies the property type (StringProperty, IntegerProperty, etc.)
 * - keyName is the JSON key for the parameter
 * - resultMapper controls how results are formatted (optional)
 */
@Component
public class DemoCommands {
    
    /**
     * Simple echo command that returns the input message.
     * Shows basic String parameter usage.
     */
    @CommandDef(command = "demo.echo", 
                description = "Echo back the input message", 
                isInternal = false)
    public String echo(
            @DebugArgAno(propType = StringProperty.class, 
                        keyName = "message",
                        description = "message to echo back",
                        defaultValue = "Hello World") 
            String message) {
        return message;
    }
    
    /**
     * Add two numbers together.
     * Shows integer parameter usage and Map result type.
     */
    @CommandDef(command = "demo.add", 
                description = "Add two numbers together", 
                isInternal = false,
                resultMapper = MapMapping.class)
    public Map<String, Object> add(
            @DebugArgAno(propType = IntegerProperty.class,
                        keyName = "a",
                        description = "first number",
                        defaultValue = "0")
            int a,
            @DebugArgAno(propType = IntegerProperty.class,
                        keyName = "b",
                        description = "second number",
                        defaultValue = "0")
            int b) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("a", a);
        result.put("b", b);
        result.put("sum", a + b);
        result.put("operation", "addition");
        return result;
    }
    
    /**
     * Generate a greeting message.
     * Shows optional parameters (mustExist = false).
     */
    @CommandDef(command = "demo.greet", 
                description = "Generate a greeting message", 
                isInternal = false)
    public String greet(
            @DebugArgAno(propType = StringProperty.class,
                        keyName = "name",
                        description = "name to greet",
                        defaultValue = "stranger",
                        mustExist = false)
            String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Hello, stranger!";
        }
        return "Hello, " + name + "! Welcome to Hitorro.";
    }
    
    /**
     * Get current timestamp with optional format.
     * Shows how to return structured data as a Map.
     */
    @CommandDef(command = "demo.timestamp", 
                description = "Get current timestamp with optional format", 
                isInternal = false,
                resultMapper = MapMapping.class)
    public Map<String, Object> timestamp(
            @DebugArgAno(propType = StringProperty.class,
                        keyName = "format",
                        description = "date format pattern (e.g., 'yyyy-MM-dd HH:mm:ss')",
                        defaultValue = "",
                        mustExist = false)
            String format) {
        LocalDateTime now = LocalDateTime.now();
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", now.toString());
        result.put("epochMillis", System.currentTimeMillis());
        
        if (format != null && !format.trim().isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                result.put("formatted", now.format(formatter));
                result.put("format", format);
            } catch (Exception e) {
                result.put("formatError", e.getMessage());
            }
        }
        
        return result;
    }
    
    /**
     * Calculate statistics for a list of numbers.
     * Shows how to parse and process more complex input.
     */
    @CommandDef(command = "demo.stats", 
                description = "Calculate statistics for comma-separated numbers", 
                isInternal = false,
                resultMapper = MapMapping.class)
    public Map<String, Object> stats(
            @DebugArgAno(propType = StringProperty.class,
                        keyName = "numbers",
                        description = "comma-separated numbers (e.g., '1,2,3,4,5')",
                        defaultValue = "1,2,3,4,5")
            String numbersStr) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        try {
            String[] parts = numbersStr.split(",");
            List<Double> numbers = new ArrayList<>();
            
            for (String part : parts) {
                numbers.add(Double.parseDouble(part.trim()));
            }
            
            if (numbers.isEmpty()) {
                result.put("error", "No valid numbers provided");
                return result;
            }
            
            double sum = numbers.stream().mapToDouble(Double::doubleValue).sum();
            double mean = sum / numbers.size();
            double min = numbers.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            double max = numbers.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            
            result.put("count", numbers.size());
            result.put("sum", sum);
            result.put("mean", mean);
            result.put("min", min);
            result.put("max", max);
            result.put("numbers", numbers);
            
        } catch (NumberFormatException e) {
            result.put("error", "Invalid number format: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Reverse a string.
     * Simple string manipulation example.
     */
    @CommandDef(command = "demo.reverse", 
                description = "Reverse a string", 
                isInternal = false,
                resultMapper = MapMapping.class)
    public Map<String, Object> reverse(
            @DebugArgAno(propType = StringProperty.class,
                        keyName = "text",
                        description = "text to reverse",
                        defaultValue = "Hitorro")
            String text) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("original", text);
        result.put("reversed", new StringBuilder(text).reverse().toString());
        result.put("length", text.length());
        return result;
    }
    
    /**
     * Get system information.
     * Shows a command with no parameters.
     */
    @CommandDef(command = "demo.sysinfo", 
                description = "Get system information", 
                isInternal = false,
                resultMapper = MapMapping.class)
    public Map<String, Object> sysinfo() {
        Map<String, Object> result = new LinkedHashMap<>();
        Runtime runtime = Runtime.getRuntime();
        
        result.put("javaVersion", System.getProperty("java.version"));
        result.put("javaVendor", System.getProperty("java.vendor"));
        result.put("osName", System.getProperty("os.name"));
        result.put("osVersion", System.getProperty("os.version"));
        result.put("osArch", System.getProperty("os.arch"));
        result.put("availableProcessors", runtime.availableProcessors());
        result.put("maxMemoryMB", runtime.maxMemory() / (1024 * 1024));
        result.put("totalMemoryMB", runtime.totalMemory() / (1024 * 1024));
        result.put("freeMemoryMB", runtime.freeMemory() / (1024 * 1024));
        
        return result;
    }
}
