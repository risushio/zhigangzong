package com.zhigangzong.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.mapper.AuthMapper;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean UserDetailsService userDetailsService(AuthMapper mapper) {
        return username -> {
            var account = mapper.find(username);
            if (account == null) throw new UsernameNotFoundException("账号或密码错误");
            return User.withUsername(username).password((String) account.get("passwordHash"))
                    .roles((String) account.get("role"))
                    .disabled(!Boolean.TRUE.equals(account.get("enabled"))).build();
        };
    }

    @Bean SecurityFilterChain security(HttpSecurity http, ObjectMapper json) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/csrf", "/api/auth/login", "/api/health", "/api/health/live").permitAll()
                .requestMatchers("/api/auth/me").authenticated()
                .requestMatchers("/api/portal/**").hasAnyRole("SCHOOL_ADMIN", "STUDENT", "RECRUITER", "TEACHER", "ENTERPRISE_MENTOR")
                .requestMatchers("/api/**").hasRole("SCHOOL_ADMIN")
                .anyRequest().permitAll());
        http.formLogin(form -> form.loginProcessingUrl("/api/auth/login")
                .successHandler((req, res, auth) -> {
                    res.setContentType("application/json;charset=UTF-8");
                    json.writeValue(res.getWriter(), ApiResponse.ok(java.util.Map.of("username", auth.getName())));
                })
                .failureHandler((req, res, ex) -> {
                    res.setStatus(401); res.setContentType("application/json;charset=UTF-8");
                    json.writeValue(res.getWriter(), ApiResponse.error("UNAUTHORIZED", "账号或密码错误"));
                }));
        http.logout(logout -> logout.logoutUrl("/api/auth/logout").deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req, res, auth) -> {
                    res.setContentType("application/json;charset=UTF-8");
                    json.writeValue(res.getWriter(), ApiResponse.ok(null));
                }));
        http.exceptionHandling(errors -> errors
                .authenticationEntryPoint((req,res,ex) -> {
                    res.setStatus(401); res.setContentType("application/json;charset=UTF-8");
                    json.writeValue(res.getWriter(), ApiResponse.error("UNAUTHORIZED", "请先登录"));
                })
                .accessDeniedHandler((req,res,ex) -> {
                    res.setStatus(403); res.setContentType("application/json;charset=UTF-8");
                    json.writeValue(res.getWriter(), ApiResponse.error("FORBIDDEN", "无操作权限或安全令牌已过期，请刷新页面重试"));
                }));
        return http.build();
    }
}
