package br.com.voting_system_api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import br.com.voting_system_api_gateway.filter.CookieAuthenticationFilter;

@Configuration
public class GatewayConfig {

    @Bean
    public CookieAuthenticationFilter cookieAuthenticationFilter() {
        return new CookieAuthenticationFilter();
    }

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder, CookieAuthenticationFilter cookieAuthenticationFilter) {
        return builder.routes()
                .route("user-service", r -> r.path("/api/users/**")
                        .filters(f -> f.filter((exchange, chain) -> {
                            String path = exchange.getRequest().getURI().getPath();
                            // Não aplica o filtro para rotas públicas de autenticação
                            if (path.startsWith("/api/users/register") || path.startsWith("/api/users/login")) {
                                return chain.filter(exchange);
                            }
                            // Para todas as outras, aplica o filtro normalmente
                            return cookieAuthenticationFilter.filter(exchange, chain);
                        }))
                        .uri("https://voting-system-user-service.onrender.com"))
                .route("vote-service", r -> r.path("/api/votes/**", "/api/votes_session/**")
                        .filters(f -> f.filter(cookieAuthenticationFilter))
                        .uri("https://voting-system-vote-service.onrender.com"))
                .build();
    }
}
