package ru.tpu.welcomtpu.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Защита /api/v1/admin/** : только с валидной JWT-cookie access_token.
 * 401 — нет/просрочен токен; 403 — токен валиден, но не админ.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class AdminAuthFilter extends OncePerRequestFilter {

    private final AuthController authController;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean isAdminPath = path.startsWith("/api/v1/admin/");
        boolean isMe = path.equals("/api/v1/auth/me");

        if (isAdminPath || isMe) {
            var user = authController.currentUser(request);
            if (user.isEmpty()) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Нет действительной сессии");
                return;
            }
            if (isAdminPath && !user.get().admin()) {
                response.sendError(HttpStatus.FORBIDDEN.value(), "Доступ только для администраторов");
                return;
            }
            request.setAttribute("auth.user", user.get());
        }
        chain.doFilter(request, response);
    }
}
