package com.newapp.Erpnext.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
<<<<<<< Updated upstream:src/main/java/com/newapp/Erpnext/config/RestTemplateConfig.java
} 
=======

    
}
>>>>>>> Stashed changes:src/main/java/com/newapp/Erpnext/config/AppConfig.java
