
package com.smartattendance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        // Maximum time to establish a connection: 5 seconds
        factory.setConnectTimeout(5000);

        // Maximum time to wait for a response: 30 seconds
        factory.setReadTimeout(30000);

        return new RestTemplate(factory);
    }
}
