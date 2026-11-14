package ru.tpu.welcomtpu.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Клиент единой авторизации ТПУ (oauth.tpu.ru):
 * authorize -> access_token -> check-token / user.
 *
 * @see <a href="https://devdocs.tpu.ru/books/edinaia-avtorizaciia-tpu">документация</a>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TpuOAuthService {

    private final RestClient restClient = RestClient.create();
    private final SecureRandom random = new SecureRandom();

    /** state -> время создания (защита от CSRF). */
    private final Map<String, Instant> pendingStates = new ConcurrentHashMap<>();

    @Value("${oauth.tpu.authorize-url:https://oauth.tpu.ru/authorize}")
    private String authorizeUrl;
    @Value("${oauth.tpu.access-token-url:https://oauth.tpu.ru/access_token}")
    private String accessTokenUrl;
    @Value("${oauth.tpu.check-token-url:https://oauth.tpu.ru/check-token}")
    private String checkTokenUrl;
    @Value("${oauth.tpu.client-id}")
    private String clientId;
    @Value("${oauth.tpu.client-secret}")
    private String clientSecret;
    @Value("${oauth.tpu.redirect-uri}")
    private String redirectUri;
    @Value("${auth.admin-user-ids:}")
    private Set<Long> adminUserIds; // "123,456"
    @Value("${auth.admin-usernames:}")
    private Set<String> adminUsernames; // "mas142,ivanov"

    /** GET /auth/login — URL для перехода на страницу авторизации. */
    public String buildAuthorizationUrl() {
        String state = newState();
        return authorizeUrl + "?client_id=" + urlEncode(clientId)
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&response_type=code"
                + "&state=" + state;
    }

    /** Обмен code на access_token (GET /access_token по схеме ТПУ). */
    @SuppressWarnings("unchecked")
    public String exchangeCodeForToken(String code) {
        // параметры уже url-encoded, поэтому URI собираем строкой и не даём клиенту перекодировать
        String uri = accessTokenUrl + "?client_id=" + urlEncode(clientId)
                + "&client_secret=" + urlEncode(clientSecret)
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&code=" + urlEncode(code)
                + "&grant_type=authorization_code";
        Map<String, Object> response;
        try {
            response = restClient.get()
                    .uri(java.net.URI.create(uri))
                    .retrieve()
                    .body(Map.class);
        } catch (org.springframework.web.client.RestClientResponseException e) {
            log.warn("oauth.tpu.ru/access_token отклонил запрос: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "oauth.tpu.ru отклонил authorization code");
        } catch (org.springframework.web.client.RestClientException e) {
            log.error("oauth.tpu.ru недоступен", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Сервис авторизации ТПУ недоступен");
        }
        Object token = response != null ? response.get("access_token") : null;
        if (token == null) {
            log.warn("oauth.tpu.ru/access_token не вернул access_token: {}", response);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Не удалось обменять authorization code на access_token");
        }
        return token.toString();
    }

    /** GET /check-token — user_id и username по access_token. */
    public TpuUser checkToken(String accessToken) {
        CheckTokenResponse body = restClient.get()
                .uri(checkTokenUrl)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(CheckTokenResponse.class);
        if (body == null || body.body() == null || body.body().user_id() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Токен ТПУ невалиден");
        }
        return new TpuUser(body.body().user_id(), body.body().username());
    }

    /** Пользователь — администратор приложения? (по user_id или логину) */
    public boolean isAdmin(long userId, String username) {
        return adminUserIds.contains(userId)
                || (username != null && adminUsernames.contains(username));
    }

    /** Валиден ли state, выданный нами в /auth/login (одноразовый). */
    public boolean consumeState(String state) {
        if (state == null) {
            return false;
        }
        Instant created = pendingStates.remove(state);
        return created != null && created.plusSeconds(600).isAfter(Instant.now());
    }

    private String newState() {
        String state = HexFormat.of().formatHex(random.generateSeed(16));
        pendingStates.put(state, Instant.now());
        // подчистка просроченных
        pendingStates.entrySet().removeIf(e -> e.getValue().plusSeconds(600).isBefore(Instant.now()));
        return state;
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }

    public record TpuUser(long userId, String username) {
    }

    /** Ответ oauth.tpu.ru/check-token. */
    record CheckTokenResponse(String message, Body body) {
        record Body(Long user_id, String username, String client_id, String type) {
        }
    }
}
