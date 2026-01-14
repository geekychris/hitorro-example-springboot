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
package com.hitorro.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * Example Spring Boot application demonstrating Hitorro integration.
 * 
 * This application showcases:
 * - Automatic Hitorro service initialization
 * - Command execution via REST endpoints
 * - Property integration
 * - Multiple CLI access modes
 * - DMS (Document Management System) integration
 */
@SpringBootApplication
@EntityScan(basePackages = {
    "com.hitorro.base.objects",      // Hitorro base entities (NamedLongEntry, etc.)
    "com.hitorro.basedms",           // Hitorro DMS entities
    "com.hitorro.example"            // Application entities (if any)
})
public class HitorroExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(HitorroExampleApplication.class, args);
    }
}
