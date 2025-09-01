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

    // ObjectMapper para nos ajudar a ler o corpo da resposta JSON
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        String userServiceUri = "https://voting-system-user-service.onrender.com";
        String voteServiceUri = "https://voting-system-vote-service.onrender.com";

        return builder.routes()
                // Rota pública de registro (não mexe com cookies)
                .route("user-service-register", r -> r.path("/api/users/register")
                        .uri(userServiceUri))

                // Rota de LOGOUT: O Gateway intercepta para limpar os cookies do browser
                .route("user-service-logout", r -> r.path("/api/users/logout").and().method(HttpMethod.POST)
                        .filters(f -> f.filter((exchange, chain) -> {
                            System.out.println("✅ [GATEWAY LOGOUT] Limpando cookies do browser.");
                            // Cria cookies com tempo de vida 0 para instruir o browser a removê-los
                            ResponseCookie userIdCookie = ResponseCookie.from("userId", "").httpOnly(false).secure(true).path("/").sameSite("None").maxAge(0).build();
                            ResponseCookie roleCookie = ResponseCookie.from("role", "").httpOnly(false).secure(true).path("/").sameSite("None").maxAge(0).build();

                            exchange.getResponse().getHeaders().add(HttpHeaders.SET_COOKIE, userIdCookie.toString());
                            exchange.getResponse().getHeaders().add(HttpHeaders.SET_COOKIE, roleCookie.toString());

                            // Permite que a requisição continue para o user-service (opcional, mas bom para logs)
                            return chain.filter(exchange);
                        }))
                        .uri(userServiceUri))

                // Rota ESPECIAL DE LOGIN: O Gateway cria os cookies
                .route("user-service-login", r -> r.path("/api/users/login").and().method(HttpMethod.POST)
                        .filters(f -> f.modifyResponseBody(String.class, String.class, (exchange, body) -> {
                            try {
                                // Verifica se o login no user-service foi bem-sucedido (status 2xx)
                                if (exchange.getResponse().getStatusCode() != null &&
                                    exchange.getResponse().getStatusCode().is2xxSuccessful() &&
                                    body != null) {
                                    
                                    // Lê os dados do usuário do corpo da resposta (JSON)
                                    Map<String, Object> bodyMap = objectMapper.readValue(body, Map.class);
                                    String userId = String.valueOf(bodyMap.get("userId"));
                                    String role = (String) bodyMap.get("role");

                                    if (userId != null && !userId.equals("null") && role != null) {
                                        System.out.println("✅ [GATEWAY LOGIN] Sucesso! Criando cookies para userId=" + userId);
                                        // O Gateway cria os cookies para seu próprio domínio
                                        ResponseCookie userIdCookie = ResponseCookie.from("userId", userId)
                                                .httpOnly(false).secure(true).path("/").sameSite("None").maxAge(3600).build();
                                        
                                        ResponseCookie roleCookie = ResponseCookie.from("role", role)
                                                .httpOnly(false).secure(true).path("/").sameSite("None").maxAge(3600).build();

                                        // Adiciona os cookies na resposta final para o browser
                                        exchange.getResponse().getHeaders().add(HttpHeaders.SET_COOKIE, userIdCookie.toString());
                                        exchange.getResponse().getHeaders().add(HttpHeaders.SET_COOKIE, roleCookie.toString());
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("❌ [GATEWAY LOGIN] Erro ao processar resposta do login: " + e.getMessage());
                            }
                            // Retorna o corpo da resposta original para o frontend
                            return Mono.just(body);
                        }))
                        .uri(userServiceUri))

                // Rotas PRIVADAS (protegidas pelo filtro de autenticação)
                .route("user-service-secured", r -> r.path("/api/users/**")
                        .filters(f -> f.filter(cookieAuthenticationFilter))
                        .uri(userServiceUri))

                .route("vote-service-secured", r -> r.path("/api/votes/**", "/api/votes_session/**")
                        .filters(f -> f.filter(cookieAuthenticationFilter))
                        .uri(voteServiceUri))
                .build();
    }
}