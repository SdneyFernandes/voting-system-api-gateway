package br.com.voting_system_api_gateway.config;

import org.springframework.beans.factory.annotation.Autowired; // <-- MUDANÇA: Importado
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import br.com.voting_system_api_gateway.filter.CookieAuthenticationFilter;

@Configuration
public class GatewayConfig {

    // <-- MUDANÇA: Injeta o filtro que agora é um @Component
    @Autowired
    private CookieAuthenticationFilter cookieAuthenticationFilter;

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // Rotas PÚBLICAS (sem filtro)
                .route("user-service-public", r -> r.path(
                        "/api/users/register",
                        "/api/users/login",
                        "/api/users/logout"
                    ).uri("https://voting-system-user-service.onrender.com"))

                // Rotas PRIVADAS (com filtro)
                .route("user-service-secured", r -> r.path("/api/users/**")
                        // <-- MUDANÇA: Usa o filtro injetado (sem parênteses)
                        .filters(f -> f.filter(cookieAuthenticationFilter)) 
                        .uri("https://voting-system-user-service.onrender.com"))

                .route("vote-service", r -> r.path("/api/votes/**", "/api/votes_session/**")
                        // <-- MUDANÇA: Usa o filtro injetado (sem parênteses)
                        .filters(f -> f.filter(cookieAuthenticationFilter))
                        .uri("https://voting-system-vote-service.onrender.com"))
                .build();
    }

    // <-- MUDANÇA: O @Bean do filtro foi removido daqui. Não é mais necessário.
}