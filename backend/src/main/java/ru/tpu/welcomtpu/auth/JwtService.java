package ru.tpu.welcomtpu.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Минимальный JWT (HS256) без внешних зависимостей.
 * Payload: sub (user_id), username, is_admin, iat, exp.
 */
@Service
public class JwtService {

    private final byte[] secret;
    private final long ttlSeconds;

    public JwtService(@Value("${auth.jwt.secret}") String secret,
                      @Value("${auth.jwt.ttl-seconds:3600}") long ttlSeconds) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "auth.jwt.secret не задан или короче 32 символов (см. .secret/backend-oauth.properties)");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = ttlSeconds;
    }

    public record AuthUser(long userId, String username, boolean admin) {
    }

    public String issue(AuthUser user) {
        long now = Instant.now().getEpochSecond();
        String header = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = b64("""
                {"sub":"%d","username":"%s","is_admin":%b,"iat":%d,"exp":%d}"""
                .formatted(user.userId(), esc(user.username()), user.admin(), now, now + ttlSeconds));
        String signingInput = header + "." + payload;
        return signingInput + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(signingInput));
    }

    /** Возвращает пользователя, если подпись валидна и токен не истёк. */
    public Optional<AuthUser> verify(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }
        byte[] expected = hmac(parts[0] + "." + parts[1]);
        if (!MessageDigest.isEqual(expected, Base64.getUrlDecoder().decode(parts[2]))) {
            return Optional.empty();
        }
        String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        long exp = longField(json, "exp");
        if (exp <= Instant.now().getEpochSecond()) {
            return Optional.empty();
        }
        return Optional.of(new AuthUser(
                Long.parseLong(stringField(json, "sub")),
                stringField(json, "username"),
                Boolean.parseBoolean(stringField(json, "is_admin"))));
    }

    // ------------------------------------------------------------------

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA256 недоступен", e);
        }
    }

    private static String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Наивный разбор поля строки/числа из плоского JSON (payload у нас всегда плоский). */
    private static String stringField(String json, String field) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"" + field + "\"\\s*:\\s*\"?([^,\"}]+)\"?").matcher(json);
        return m.find() ? m.group(1).trim() : null;
    }

    private static long longField(String json, String field) {
        return Long.parseLong(stringField(json, field));
    }
}
