package com.smartsell.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartsell.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * คำขอที่ไม่มี token หรือ token ใช้ไม่ได้ → 401 พร้อม JSON รูปแบบเดียวกับ error อื่นทั้งระบบ
 *
 * ค่าเริ่มต้นของ Spring Security จะตอบเป็นหน้า HTML หรือ body ว่าง ซึ่ง client
 * ที่คาดหวัง JSON จะ parse ไม่ได้
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                new ApiError(HttpServletResponse.SC_UNAUTHORIZED, "กรุณาเข้าสู่ระบบก่อนใช้งาน"));
    }
}
