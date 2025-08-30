package br.com.voting_system_api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity // Importante: Use a anotação para WebFlux, não a de Web MVC
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            // 1. Desativa a proteção CSRF, que é a causa provável do erro 403
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            
            // 2. Permite todas as requisições, pois a validação será feita 
            //    pelo nosso CookieAuthenticationFilter e pelos microserviços.
            .authorizeExchange(exchange -> exchange
                .anyExchange().permitAll()
            );

        return http.build();
    }
}