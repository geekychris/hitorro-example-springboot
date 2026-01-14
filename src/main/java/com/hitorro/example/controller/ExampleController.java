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
package com.hitorro.example.controller;

import com.hitorro.base.service.BasicService;
import com.hitorro.spring.autoconfigure.service.HitorroServiceFactory;
import com.hitorro.util.startupframework.ServiceContext;
import com.hitorro.util.startupframework.ServiceWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Example REST controller demonstrating how to use Hitorro services
 * in a Spring Boot application.
 */
@RestController
@RequestMapping("/api/example")
public class ExampleController {

    private final HitorroServiceFactory serviceFactory;
    private final ServiceContext serviceContext;

    @Autowired
    public ExampleController(HitorroServiceFactory serviceFactory, 
                            ServiceContext serviceContext) {
        this.serviceFactory = serviceFactory;
        this.serviceContext = serviceContext;
    }

    /**
     * Get application status including Hitorro services.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        
        status.put("application", "Hitorro Spring Boot Example");
        status.put("hitorroIntegration", "active");
        status.put("serviceContextState", serviceContext.getServerState());
        
        // Get all initialized services
        List<ServiceWrapper> services = serviceContext.getServices();
        List<String> serviceNames = services.stream()
                .map(ServiceWrapper::getShortName)
                .collect(Collectors.toList());
        
        status.put("initializedServices", serviceNames);
        status.put("serviceCount", services.size());
        
        return ResponseEntity.ok(status);
    }

    /**
     * Example of accessing a specific Hitorro service.
     */
    @GetMapping("/basic-service")
    public ResponseEntity<Map<String, Object>> getBasicServiceInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // Check if BasicService is available
        boolean available = serviceFactory.isServiceAvailable(BasicService.class);
        info.put("basicServiceAvailable", available);
        
        if (available) {
            BasicService basicService = serviceFactory.getService(BasicService.class);
            info.put("serviceClass", basicService.getClass().getName());
            info.put("message", "BasicService is initialized and ready");
        } else {
            info.put("message", "BasicService is not available");
        }
        
        return ResponseEntity.ok(info);
    }

    /**
     * List all initialized Hitorro services with details.
     */
    @GetMapping("/services")
    public ResponseEntity<List<Map<String, String>>> listServices() {
        List<ServiceWrapper> services = serviceFactory.getAllServices();
        
        List<Map<String, String>> serviceList = services.stream()
                .map(sw -> {
                    Map<String, String> serviceInfo = new HashMap<>();
                    serviceInfo.put("shortName", sw.getShortName());
                    serviceInfo.put("description", sw.getDescription());
                    serviceInfo.put("className", sw.getClazz().getName());
                    serviceInfo.put("initialized", String.valueOf(sw.isInitialized()));
                    return serviceInfo;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(serviceList);
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("hitorroServices", serviceContext.isInitialized() ? "initialized" : "not initialized");
        return ResponseEntity.ok(health);
    }
}
