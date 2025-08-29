import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable) // CSRF não faz sentido em API Gateway
            .cors(cors -> {}) // 🔑 Habilita o CORS antes do resto
            .authorizeExchange(exchange -> exchange
                .pathMatchers("/api/auth/**", "/actuator/health", "/actuator/info" ).permitAll() // login/registro liberados
                .anyExchange().authenticated() // o resto exige auth
            );

        return http.build();
    }
}
