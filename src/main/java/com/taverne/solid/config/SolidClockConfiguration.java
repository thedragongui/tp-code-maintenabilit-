package com.taverne.solid.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class SolidClockConfiguration {

    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
