package com.newapp.Erpnext.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> authorize
<<<<<<< Updated upstream
                .requestMatchers("/", "/login", "/api/auth/login", "/css/**", "/js/**", "/images/**", "/dashboard").permitAll()
                .requestMatchers("/dashboard", "/api/**", 
                               "/suppliers", "/suppliers/**", 
                               "/quotations", "/quotations/**", 
                               "/purchase/**",
                               "/invoices", "/invoices/**",
                               "/payments", "/payments/**", "/payments/submit",
                               "/employees", "/employees/**",
                               "/salaries", "/salaries/**",
                               "/import", "/import/**").permitAll()
=======
                .requestMatchers("/", "/login", "/api/auth/login", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/dashboard", "/api/**", "/suppliers/**", "/quotations/**", 
                    "/purchase/**", "/invoices/**", "/payments/**", "/employees/**", 
                    "/salaries/**","/import/**","/import","/salaries/pdf/**","/salaries","/error").permitAll()
>>>>>>> Stashed changes
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login")
                .permitAll()
            )
            .sessionManagement(session -> session
                .invalidSessionUrl("/login")
                .maximumSessions(1)
                .expiredUrl("/login?expired")
            );
                
        return http.build();
    }
}