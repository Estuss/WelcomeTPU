package ru.tpu.welcomtpu.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Эндпоинты аутентификации: /api/v1/auth/login, /callback, /me, /logout.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    public static final String COOKIE_NAME = "access_token";

    private final TpuOAuthService oauth;
    private final JwtService jwt;

    @Value("${auth.cookie.secure:false}")
    private boolean cookieSecure;
    @Value("${auth.success-redirect:/}")
    private String successRedirect;

    /**
     * GET /auth/login — URL страницы авторизации oauth.tpu.ru.
     */
    @GetMapping("/login")
    public Map<String, String> login() {
        return Map.of("authorization_url", oauth.buildAuthorizationUrl());
    }

    /**
     * GET /auth/callback?code&state — обмен code, выдача JWT-cookie, редирект.
     * 403 при невалидном state или не-админе.
     */
    @GetMapping("/callback")
    public void callback(@RequestParam(required = false) String code,
                         @RequestParam(required = false) String state,
                         HttpServletResponse response) throws java.io.IOException {
        if (!oauth.consumeState(state)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Невалидный state");
        }
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Отсутствует параметр code");
        }

        String accessToken = oauth.exchangeCodeForToken(code);
        TpuOAuthService.TpuUser user = oauth.checkToken(accessToken);
        if (!oauth.isAdmin(user.userId(), user.username())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Пользователь %d не входит в список администраторов приложения".formatted(user.userId()));
        }

        String token = jwt.issue(new JwtService.AuthUser(user.userId(), user.username(), true));
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(3600)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        response.sendRedirect(successRedirect);
    }

    /**
     * GET /auth/me — текущий админ (401 без/с просроченной cookie).
     */
    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        JwtService.AuthUser user = currentUser(request)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Нет действительной сессии"));
        return Map.of(
                "user_id", user.userId(),
                "username", user.username(),
                "is_admin", user.admin());
    }

    /**
     * POST /auth/logout — сброс cookie. 204.
     */
    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    /** Текущий пользователь из cookie (используется и фильтром админ-эндпоинтов). */
    public java.util.Optional<JwtService.AuthUser> currentUser(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return java.util.Optional.empty();
        }
        for (Cookie c : cookies) {
            if (COOKIE_NAME.equals(c.getName())) {
                return jwt.verify(c.getValue());
            }
        }
        return java.util.Optional.empty();
    }
}
