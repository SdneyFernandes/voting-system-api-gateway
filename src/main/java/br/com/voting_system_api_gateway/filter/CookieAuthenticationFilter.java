package br.com.voting_system_api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.stereotype.Component; //Remova essa linha
import org.springframework.cloud.gateway.filter.GatewayFilter;

import java.util.Optional;

//Remova a anotação @Component
//@Component
public class CookieAuthenticationFilter implements GatewayFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        Optional<String> userId = getCookieValue(request, "userId");
        Optional<String> role = getCookieValue(request, "role");

        if (userId.isPresent() && role.isPresent()) {
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId.get())
                    .header("X-User-Role", role.get())
                    .build();

            ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
            return chain.filter(mutatedExchange);
        }

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
