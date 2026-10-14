package br.com.cinepass.reserva.infrastructure.http;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {
    @Bean
    @Primary
    RestClient.Builder restClientBuilder(ObservationRegistry observationRegistry) {
        return RestClient.builder().observationRegistry(observationRegistry);
    }

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder(ObservationRegistry observationRegistry) {
        return RestClient.builder().observationRegistry(observationRegistry);
    }
}