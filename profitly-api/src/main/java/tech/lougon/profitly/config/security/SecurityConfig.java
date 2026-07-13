package tech.lougon.profitly.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SecurityFilter securityFilter;

    public SecurityConfig(SecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/google").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/analysis/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tickers/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/news/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/rankings/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ibovespa/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/fii/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/treasury/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/funds/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/crypto/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stocks/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:5199", // secondary local dev server (tooling previews)
                "https://profitly.lougon.tech"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
