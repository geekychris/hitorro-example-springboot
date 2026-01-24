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

import com.hitorro.unittime.UnitTimeCommand;
import com.hitorro.util.commandandcontrol.CommandRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Register UnitTime command with the CommandRegistry.
 * 
 * NOTE: UnitTimeCommand extends Command (class-level command), so it needs
 * explicit registration. The @CommandDef annotation on the class is metadata
 * for the command, but doesn't automatically register it like @CommandDef methods do.
 */
@Configuration
public class UnitTimeCommandRegistration {
    
    private static final Logger logger = LoggerFactory.getLogger(UnitTimeCommandRegistration.class);
    
    /**
     * Register UnitTime command after services are initialized.
     * This ensures TestServerService is ready before the command is registered.
     */
    @Bean
    @Order(100)  // Run after service initialization
    public CommandLineRunner registerUnitTimeCommand() {
        return args -> {
            CommandRegistry registry = CommandRegistry.getRegistry();
            
            // Register UnitTimeCommand
            UnitTimeCommand cmd = new UnitTimeCommand();
            registry.add(cmd);
            
            logger.info("✓ Registered UnitTimeCommand: test.rununittime");
        };
    }
}
