package com.transit.SGComplaint.config;

import org.springframework.context.annotation.Bean;
import com.transit.SGComplaint.service.AbuseLimitService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.beans.factory.annotation.Value;

import javax.sql.DataSource;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RoleBasedAuthenticationSuccessHandler successHandler,
            AbuseLimitService limits,
            PersistentTokenRepository persistentTokenRepository,
            UserDetailsService userDetailsService,
            @Value("${app.security.remember-me-key}") String rememberMeKey) throws Exception {
        http
            .addFilterBefore(new AuthenticationRateLimitFilter(limits), UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET,
                    "/complaints",
                    "/complaints/view/**",
                    "/api/public/complaints",
                    "/notices/**"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/complaints/*/verify").permitAll()
                .requestMatchers(
                    "/",
                    "/login",
                    "/signup/**",
                    "/account/**",
                    "/api/members/check-id",
                    "/api/account-recovery/**",
                    "/api/signup/**",
                    "/api/csrf",
                    "/api/layout/me",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/main-banners/**",
                    "/actuator/health",
                    "/actuator/health/**",
                    "/company/**",
                    "/route/**",
                    "/route-guide-images/**",
                    "/recruit/**",
                    "/customer/**",
                    "/favicon.ico",
                    "/error"
                ).permitAll()
                .requestMatchers("/api/phone-verifications/**").authenticated()
                .requestMatchers("/admin/partners", "/admin/partners/**").hasRole("MASTER")
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "MASTER")
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "MASTER")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("empId")
                .passwordParameter("empPassword")
                .successHandler(successHandler)
                .failureUrl("/login?error")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .rememberMeParameter("remember-me")
                .tokenRepository(persistentTokenRepository)
                .userDetailsService(userDetailsService)
                .key(rememberMeKey)
                .tokenValiditySeconds(60 * 60 * 24 * 14)
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "remember-me")
            );

        return http.build();
    }

    @Bean
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl();
        repository.setDataSource(dataSource);
        return repository;
    }
}
