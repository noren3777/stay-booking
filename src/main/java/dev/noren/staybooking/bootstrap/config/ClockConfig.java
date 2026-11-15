package dev.noren.staybooking.bootstrap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneOffset;

@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.system(ZoneOffset.UTC);
    }
}
