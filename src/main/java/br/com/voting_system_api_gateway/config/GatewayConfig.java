package br.com.voting_system_api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import br.com.voting_system_api_gateway.filter.CookieAuthenticationFilter;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // Rotas PÚBLICAS
                .route("user-service-public", r -> r.path(
                        "/api/users/register", 
                        "/api/users/login",
                        "/api/users/logout"
                    )
                    .uri("https://voting-system-user-service.onrender.com"))
                
                // Rotas PRIVADAS (com filtro de autenticação)
                .route("user-service-secured", r -> r.path("/api/users/**")
                        .filters(f -> f.filter(cookieAuthenticationFilter()))
                        .uri("https://voting-system-user-service.onrender.com"))
                
                .route("vote-service", r -> r.path("/api/votes/**", "/api/votes_session/**")
                        .filters(f -> f.filter(cookieAuthenticationFilter()))
                        .uri("https://voting-system-vote-service.onrender.com"))
                .build();
    }

    @Bean
    public CookieAuthenticationFilter cookieAuthenticationFilter() {
        return new CookieAuthenticationFilter();
    }
}