package com.smartsell.config;

import com.smartsell.security.JwtAuthFilter;
import com.smartsell.security.RestAccessDeniedHandler;
import com.smartsell.security.RestAuthenticationEntryPoint;
import com.smartsell.security.Roles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          RestAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ปิด CSRF ได้เพราะเป็น REST API แบบ stateless ที่ยืนยันตัวตนด้วย Authorization header
            // ไม่ใช่ cookie เบราว์เซอร์จึงไม่แนบ credential ให้อัตโนมัติจากเว็บอื่น
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // --- เปิดสาธารณะ: ทางเข้าระบบ ---
                // hidden route ฝั่งหน้าเว็บกันคนไม่ได้จริง ด่านจริงคือ role check ของ /api/admin/** ข้างล่าง
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login",
                                 "/api/auth/reset-password").permitAll()

                // --- ต้องมี token ที่ใช้ได้ ---
                .requestMatchers("/api/auth/me").authenticated()

                // --- ต้องมี token และ role ถึงระดับผู้ดูแลร้าน ---
                .requestMatchers("/api/admin/**").hasAnyRole(Roles.STORE_MANAGER, Roles.ADMIN)

                // --- ส่วนหน้าร้านสำหรับลูกค้า ใช้ได้โดยไม่ต้องล็อกอิน ---
                .requestMatchers("/api/products/**", "/api/chat/**",
                                 "/api/personal-color/**", "/api/lucky-color/**",
                                 "/api/interactions/**").permitAll()
                .anyRequest().permitAll()
            )
            // ตอบ 401/403 เป็น JSON แทนหน้า HTML เริ่มต้นของ Spring Security
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
