package br.com.voting_system_api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component; // <-- MUDANÇA: Importado
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class CookieAuthenticationFilter implements GatewayFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        
        Optional<String> userId = getCookieValue(request, "userId");
        Optional<String> role = getCookieValue(request, "role");

        if (userId.isPresent() && role.isPresent()) {
            System.out.println("✅ [GATEWAY FILTER] Cookies válidos encontrados. Adicionando headers para " + request.getURI().getPath());

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId.get())
                    .header("X-User-Role", role.get())
                    .build();
            
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        
        System.out.println("⛔ [GATEWAY FILTER] Cookies de autenticação ausentes. Bloqueando requisição 403 para " + request.getURI().getPath());
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    private Optional<String> getCookieValue(ServerHttpRequest request, String cookieName) {
        HttpCookie cookie = request.getCookies().getFirst(cookieName);
                return Optional.ofNullable(cookie).map(HttpCookie::getValue);
    }
}