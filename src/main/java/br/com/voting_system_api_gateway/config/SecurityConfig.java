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
            .csrf(ServerHttpSecurity.CsrfSpec::disable) // desativa CSRF
            .cors(cors -> {}) // habilita CORS
            .authorizeExchange(exchanges -> exchanges
                // libera login e endpoints de health/info
                .pathMatchers("/api/auth/**", "/actuator/health", "/actuator/info").permitAll()
                // qualquer outro endpoint exige autenticação
                .anyExchange().authenticated()
            )
            .httpBasic(); // habilita HTTP Basic para teste

        return http.build();
    }
}

