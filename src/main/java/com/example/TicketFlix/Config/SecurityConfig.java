package com.example.TicketFlix.Config;

import com.example.TicketFlix.Filters.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                // Public endpoints
                .antMatchers("/auth/**").permitAll()
                .antMatchers("/swagger-ui/**", "/swagger-resources/**", "/v3/api-docs/**", "/v2/api-docs/**", "/webjars/**").permitAll()
                .antMatchers("/user/add").permitAll() // Registration is public
                // Admin only endpoints
                .antMatchers("/user/delete/**", "/user/update/**").hasRole("ADMIN")
                .antMatchers("/movies/add", "/movies/update/**", "/movies/delete/**").hasAnyRole("ADMIN", "THEATER_OWNER")
                .antMatchers("/theater/add", "/theater/update/**", "/theater/delete/**").hasAnyRole("ADMIN", "THEATER_OWNER")
                .antMatchers("/screens/add", "/screens/update/**", "/screens/delete/**").hasAnyRole("ADMIN", "THEATER_OWNER")
                .antMatchers("/shows/add", "/shows/update/**", "/shows/delete/**").hasAnyRole("ADMIN", "THEATER_OWNER")
                // Authenticated endpoints
                .antMatchers("/tickets/**", "/payments/**").authenticated()
                .antMatchers("/account/**").authenticated()
                .antMatchers("/user/get/**", "/user/get-all").authenticated()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            .and()
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

