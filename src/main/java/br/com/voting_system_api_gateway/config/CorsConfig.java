package br.com.voting_system_api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    // Injeta a lista de origens da variável de ambiente que você já criou!
    @Value("${ALLOWED_ORIGINS_LIST}")
    private List<String> allowedOrigins;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // Usa a lista injetada
        corsConfig.setAllowedOrigins(allowedOrigins);
        
        corsConfig.setMaxAge(3600L);
        corsConfig.addAllowedMethod("*"); // Permite todos os métodos (GET, POST, etc)
        corsConfig.addAllowedHeader("*"); // Permite todos os cabeçalhos
        
        // ESSENCIAL para autenticação baseada em cookies
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}