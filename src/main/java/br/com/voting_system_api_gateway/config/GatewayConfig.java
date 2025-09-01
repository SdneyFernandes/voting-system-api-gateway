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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.GZIPInputStream;

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
                // ... (as outras rotas de register, logout, etc. continuam iguais)
                .route("user-service-register", r -> r.path("/api/users/register").uri(userServiceUri))

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

                // ROTA ESPECIAL DE LOGIN COM LÓGICA DE DESCOMPRESSÃO
                .route("user-service-login", r -> r.path("/api/users/login").and().method(HttpMethod.POST)
                        // <-- MUDANÇA: Agora trabalhamos com bytes (byte[]) em vez de String
                        .filters(f -> f.modifyResponseBody(byte[].class, String.class, (exchange, bodyBytes) -> {
                            String responseBody = "";
                            try {
                                // Se a resposta for bem-sucedida e tiver conteúdo...
                                if (exchange.getResponse().getStatusCode() != null &&
                                    exchange.getResponse().getStatusCode().is2xxSuccessful() &&
                                    bodyBytes != null) {

                                    // Verifica se a resposta está comprimida com Gzip
                                    String contentEncoding = exchange.getResponse().getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);
                                    if ("gzip".equalsIgnoreCase(contentEncoding)) {
                                        System.out.println("✅ [GATEWAY LOGIN] Resposta Gzip detectada. Descomprimindo...");
                                        responseBody = decompressGzip(bodyBytes);
                                    } else {
                                        // Se não estiver comprimida, apenas converte os bytes para String
                                        responseBody = new String(bodyBytes, StandardCharsets.UTF_8);
                                    }

                                    // Agora que temos o JSON limpo, processamos para criar os cookies
                                    Map<String, Object> bodyMap = objectMapper.readValue(responseBody, Map.class);
                                    String userId = String.valueOf(bodyMap.get("userId"));
                                    String role = (String) bodyMap.get("role");

                                    if (userId != null && !userId.equals("null") && role != null) {
                                        System.out.println("✅ [GATEWAY LOGIN] Sucesso! Criando cookies para userId=" + userId);
                                        ResponseCookie userIdCookie = ResponseCookie.from("userId", userId).httpOnly(false).secure(true).path("/").sameSite("None").maxAge(3600).build();
                                        ResponseCookie roleCookie = ResponseCookie.from("role", role).httpOnly(false).secure(true).path("/").sameSite("None").maxAge(3600).build();
                                        exchange.getResponse().getHeaders().add(HttpHeaders.SET_COOKIE, userIdCookie.toString());
                                        exchange.getResponse().getHeaders().add(HttpHeaders.SET_COOKIE, roleCookie.toString());
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("❌ [GATEWAY LOGIN] Erro ao processar resposta do login: " + e.getMessage());
                            }
                            // Retorna o corpo (agora como String) para o frontend
                            return Mono.just(responseBody);
                        }))
                        .uri(userServiceUri))

                // Rotas PRIVADAS (protegidas pelo filtro de autenticação)
                .route("user-service-secured", r -> r.path("/api/users/**").filters(f -> f.filter(cookieAuthenticationFilter)).uri(userServiceUri))
                .route("vote-service-secured", r -> r.path("/api/votes/**", "/api/votes_session/**").filters(f -> f.filter(cookieAuthenticationFilter)).uri(voteServiceUri))
                .build();
    }

    // Método auxiliar para descompactar Gzip
    private String decompressGzip(byte[] compressed) throws Exception {
        if (compressed == null || compressed.length == 0) {
            return "";
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
             GZIPInputStream gis = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toString(StandardCharsets.UTF_8.name());
        }
    }
}