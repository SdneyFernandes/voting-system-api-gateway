package br.com.voting_system_api_gateway.config;

import br.com.voting_system_api_gateway.filter.CookieAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseCookie;
import reactor.core.publisher.Mono;

import java.util.Map;

@Configuration
public class GatewayConfig {

    @Autowired
    private CookieAuthenticationFilter cookieAuthenticationFilter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        String userServiceUri = "https://voting-system-user-service.onrender.com";
        String voteServiceUri = "https://voting-system-vote-service.onrender.com";

        return builder.routes()
                .route("user-service-register", r -> r.path("/api/users/register")
                        .uri(userServiceUri))

                .route("user-service-logout", r -> r.path("/api/users/logout").and().method(HttpMethod.POST)
                        .filters(f -> f.filter((exchange, chain) -> {
                            System.out.println("✅ [GATEWAY LOGOUT] Limpando cookies do browser.");
                            ResponseCookie userIdCookie = ResponseCookie.from("userId", "").httpOnly(false).secure(true).path("/").sameSite("None").maxAge(0).build();
                            ResponseCookie roleCookie = ResponseCookie.from("role", "").httpOnly(false).secure(true).path("/").sameSite("None").maxAge(0).build();

                            exchange.getResponse().getHeaders().add(HttpHeaders.SET_COOKIE, userIdCookie.toString());
                            exchange.getResponse().getHeaders().add(HttpHeaders.SET_COOKIE, roleCookie.toString());
                            
                            return chain.filter(exchange);
                        }))
                        .uri(userServiceUri))

                .route("user-service-login", r -> r.path("/api/users/login").and().method(HttpMethod.POST)
                        .filters(f -> f.modifyResponseBody(String.class, String.class, (exchange, body) -> {
                            
                            // ===================================================================
                            // INÍCIO DO CÓDIGO DE DEPURAÇÃO
                            // ===================================================================
                            System.out.println("================= RAW BODY RECEIVED FROM USER-SERVICE =================");
                            System.out.println(body);
                            System.out.println("=====================================================================");
                            // ===================================================================
                            // FIM DO CÓDIGO DE DEPURAÇÃO
                            // ===================================================================

                            try {
                                if (exchange.getResponse().getStatusCode() != null &&
                                    exchange.getResponse().getStatusCode().is2xxSuccessful() &&
                                    body != null) {
                                    
                                    Map<String, Object> bodyMap = objectMapper.readValue(body, Map.class);
                                    String userId = String.valueOf(bodyMap.get("userId"));
                                    String role = (String) bodyMap.get("role");

                                    if (userId != null && !userId.equals("null") && role != null) {
                                        System.out.println("✅ [GATEWAY LOGIN] Sucesso! Criando cookies para userId=" + userId);
                                        ResponseCookie userIdCookie = ResponseCookie.from("userId", userId)
                                                .httpOnly(false).secure(true).path("/").sameSite("None").maxAge(3600).build();
                                        
                                        ResponseCookie roleCookie = ResponseCookie.from("role", role)
                                                .httpOnly(false).secure(true).path("/").sameSite("None").maxAge(3600).build();

                                        exchange.getResponse().getHeaders().add(HttpHeaders.SET_COOKIE, userIdCookie.toString());
                                        exchange.getResponse().getHeaders().add(HttpHeaders.SET_COOKIE, roleCookie.toString());
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("❌ [GATEWAY LOGIN] Erro ao processar resposta do login: " + e.getMessage());
                            }
                            return Mono.just(body);
                        }))
                        .uri(userServiceUri))

                .route("user-service-secured", r -> r.path("/api/users/**")
                        .filters(f -> f.filter(cookieAuthenticationFilter))
                        .uri(userServiceUri))

                .route("vote-service-secured", r -> r.path("/api/votes/**", "/api/votes_session/**")
                        .filters(f -> f.filter(cookieAuthenticationFilter))
                        .uri(voteServiceUri))
                .build();
    }
}