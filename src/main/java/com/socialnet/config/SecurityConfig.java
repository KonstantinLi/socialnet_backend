package com.socialnet.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.socialnet.security.JwtRequestFilter;
import com.socialnet.security.UserDetailsServiceImpl;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtRequestFilter jwtRequestFilter;
    private final ClientProperties clientProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf().disable()
                .cors()
                .and()
                .authorizeHttpRequests(registry ->
                        registry.requestMatchers("/api/v1/admin-console/**")
                                .hasRole("ADMIN")
                                .requestMatchers("/api/v1/auth/login").permitAll()
                                .requestMatchers("/api/v1/auth/captcha").permitAll()
                                .requestMatchers("/api/v1/account/register").permitAll()
                                .requestMatchers("/api/v1/account/password/recovery").permitAll()
                                .requestMatchers("/api/v1/account/password/reset").permitAll()
                                .requestMatchers("/api/v1/account/email").permitAll()
                                .requestMatchers("/api/v1/ws/**").permitAll()
                                .requestMatchers("/api/v1/statistics/**").permitAll()
                                .requestMatchers("/api/v1/**").authenticated()
                                .anyRequest().permitAll())
                .sessionManagement(manager ->
                        manager.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handler ->
                        handler.authenticationEntryPoint((request, response, ex) ->
                                response.sendError(
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        ex.getMessage()
                                )))
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(clientProperties.getLocal(), clientProperties.getRemote()));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {

        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();

        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);

        return daoAuthenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {

        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }
}
