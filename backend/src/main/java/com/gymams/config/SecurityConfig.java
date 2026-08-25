package com.gymams.config;

import com.gymams.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Stateless JWT security. /api/auth/** is open (registration/login have
 * to work before a token exists); everything else needs a valid token.
 * Equipment mutation endpoints additionally require the ADMIN role —
 * enforced here, not just by hiding buttons on the frontend, per the
 * Admin equipment-management requirement.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // More specific match must come first: change-password needs a valid
                // JWT even though the rest of /api/auth/** (register/login) is public.
                .requestMatchers(HttpMethod.PUT, "/api/auth/change-password").authenticated()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/equipment/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/equipment/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/equipment/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/equipment/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/equipment/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/usage/**").hasAnyRole("ADMIN", "GYM_MANAGER")
                .requestMatchers(HttpMethod.POST, "/api/usage/**").hasRole("GYM_MANAGER")
                .requestMatchers(HttpMethod.PUT, "/api/usage/**").hasRole("GYM_MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/usage/**").hasRole("ADMIN")

                // ---------- Module 4 — Repair Request Management ----------
                // Gym Manager: report a problem, view own submissions.
                .requestMatchers(HttpMethod.POST, "/api/repair-requests").hasRole("GYM_MANAGER")
                .requestMatchers(HttpMethod.GET, "/api/repair-requests/my").hasRole("GYM_MANAGER")
                // Admin: view all, approve/reject, assign technician.
                .requestMatchers(HttpMethod.GET, "/api/repair-requests").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/repair-requests/*/approve").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/repair-requests/*/reject").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/repair-requests/*/assign").hasRole("ADMIN")
                // Technician: view assigned work, progress its status.
                .requestMatchers(HttpMethod.GET, "/api/repair-requests/assigned").hasRole("TECHNICIAN")
                .requestMatchers(HttpMethod.PUT, "/api/repair-requests/*/start").hasRole("TECHNICIAN")
                .requestMatchers(HttpMethod.PUT, "/api/repair-requests/*/complete").hasRole("TECHNICIAN")

                // ---------- Module 5 — Maintenance Management ----------
                // Admin only: schedule, view all, reschedule, cancel. No Gym Manager access.
                .requestMatchers(HttpMethod.POST, "/api/maintenance").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/maintenance").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/maintenance/*/reschedule").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/maintenance/*/cancel").hasRole("ADMIN")
                // Technician: view assigned tasks, progress their status.
                .requestMatchers(HttpMethod.GET, "/api/maintenance/assigned").hasRole("TECHNICIAN")
                .requestMatchers(HttpMethod.PUT, "/api/maintenance/*/start").hasRole("TECHNICIAN")
                .requestMatchers(HttpMethod.PUT, "/api/maintenance/*/complete").hasRole("TECHNICIAN")

                // ---------- Technician directory (Module 4/5 assignment dropdown) ----------
                .requestMatchers(HttpMethod.GET, "/api/users/technicians").hasRole("ADMIN")

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
