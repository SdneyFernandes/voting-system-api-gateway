package br.com.voting_system_api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public class CookieAuthenticationFilter implements GatewayFilter {

    // Rotas públicas
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/users/register",
            "/api/users/login",
            "/api/users/logout",
            "/actuator/health"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        System.out.println("🔍 [GATEWAY DEBUG] Processing: " + method + " " + path);

        // ✅ Permite preflight CORS
        if (HttpMethod.OPTIONS.matches(method)) {
            System.out.println("✅ [GATEWAY DEBUG] Preflight liberado");
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return exchange.getResponse().setComplete();
        }

        // Se for rota pública, pula autenticação
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            System.out.println("✅ [GATEWAY DEBUG] Public route, skipping auth check");
            return chain.filter(exchange);
        }

        Optional<String> userId = getCookieValue(request, "userId");
        Optional<String> role = getCookieValue(request, "role");

        if (userId.isPresent() && role.isPresent()) {
            System.out.println("🎯 [GATEWAY DEBUG] Cookies found -> userId=" + userId.get() + ", role=" + role.get());

            // Encaminha cookies como headers
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId.get())
                    .header("X-User-Role", role.get())
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        // Falta de cookies em rota privada → bloqueia
        System.out.println("⛔ [GATEWAY DEBUG] Missing auth cookies, blocking request");
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    private Optional<String> getCookieValue(ServerHttpRequest request, String cookieName) {
        HttpCookie cookie = request.getCookies().getFirst(cookieName);
        if (cookie != null) {
            return Optional.of(cookie.getValue());
        }
        return Optional.empty();
    }
}
