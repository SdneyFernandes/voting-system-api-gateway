package br.com.voting_system_api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.Optional;

public class CookieAuthenticationFilter implements GatewayFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();
        
        // 🔍 LOG DE DEBUG - Mostra todas as requisições
        System.out.println("🔍 [GATEWAY DEBUG] Processing: " + method + " " + path);
        System.out.println("🍪 [GATEWAY DEBUG] Cookies: " + request.getCookies());
        System.out.println("📋 [GATEWAY DEBUG] Headers: " + request.getHeaders().keySet());
        
        Optional<String> userId = getCookieValue(request, "userId");
        Optional<String> role = getCookieValue(request, "role");

        // 🔍 LOG DOS COOKIES ENCONTRADOS
        System.out.println("✅ [GATEWAY DEBUG] userId cookie: " + userId.orElse("NOT_FOUND"));
        System.out.println("✅ [GATEWAY DEBUG] role cookie: " + role.orElse("NOT_FOUND"));

        if (userId.isPresent() && role.isPresent()) {
            System.out.println("🎯 [GATEWAY DEBUG] Adding headers X-User-Id and X-User-Role");
            
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId.get())
                    .header("X-User-Role", role.get())
                    .build();

            ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
            return chain.filter(mutatedExchange);
        }

        System.out.println("⚠️ [GATEWAY DEBUG] No auth cookies found, proceeding without authentication");
        return chain.filter(exchange);
    }

    private Optional<String> getCookieValue(ServerHttpRequest request, String cookieName) {
        HttpCookie cookie = request.getCookies().getFirst(cookieName);
        if (cookie != null) {
            return Optional.of(cookie.getValue());
        }
        return Optional.empty();
    }
}