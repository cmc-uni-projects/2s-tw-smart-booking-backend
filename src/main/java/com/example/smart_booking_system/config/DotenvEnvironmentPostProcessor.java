package com.example.smart_booking_system.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // .env is optional: Docker Compose mounts one, Kubernetes passes plain environment variables
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        Map<String, Object> map = new HashMap<>();
        dotenv.entries().forEach(entry -> map.put(entry.getKey(), entry.getValue()));

        // addFirst để ưu tiên .env hơn các nguồn khác
        environment.getPropertySources().addFirst(new MapPropertySource("dotenv", map));
    }
}
