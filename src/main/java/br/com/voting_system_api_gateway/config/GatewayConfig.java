package br.com.voting_system_api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import br.com.voting_system_api_gateway.filter.CookieAuthenticationFilter; // Importante!


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
                        .filters(f -> f.filter(cookieAuthenticationFilter)) // Adicione o filtro
                        .uri("lb://voting-system-user-service"))
                .route("vote-service", r -> r.path("/api/votes/**", "/api/votes_session/**")
                        .filters(f -> f.filter(cookieAuthenticationFilter)) // Adicione o filtro
                        .uri("lb://voting-system-vote-service"))
                .build();
    }
}