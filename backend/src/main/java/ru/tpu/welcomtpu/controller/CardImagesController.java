package ru.tpu.welcomtpu.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.tpu.welcomtpu.dto.AssetDto;
import ru.tpu.welcomtpu.model.Asset;
import ru.tpu.welcomtpu.model.Card;
import ru.tpu.welcomtpu.model.CardAsset;
import ru.tpu.welcomtpu.repository.AssetRepository;
import ru.tpu.welcomtpu.repository.CardRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Изображения карточек: POST / DELETE /api/v1/admin/cards/{cardId}/images.
 * Файлы пишутся в ./uploads/{assetId}.{ext}, отдаются через /assets/**.
 */
@RestController
@RequestMapping("/api/v1/admin/cards/{cardId}/images")
@RequiredArgsConstructor
public class CardImagesController {

    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10 МБ
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");

    private final CardRepository cardRepository;
    private final AssetRepository assetRepository;

    /**
     * POST — загрузить картинку и привязать к карточке. 201 Created.
     */
    @PostMapping
    @Transactional
    public AssetDto upload(@PathVariable UUID cardId,
                           @RequestParam("file") MultipartFile file,
                           @RequestParam(name = "alt_text", required = false) String altText) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Карточка с id=%s не найдена".formatted(cardId)));

        if (file == null || file.isEmpty()) {
            throw badRequest("Файл 'file' обязателен");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Файл больше 10 МБ");
        }
        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw badRequest("Не изображение или неподдерживаемый формат '%s'. Разрешено: image/jpeg, image/png, image/webp"
                    .formatted(mimeType));
        }
        if (altText != null && altText.length() > 512) {
            throw badRequest("alt_text длиннее 512 символов");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не удалось прочитать файл");
        }
        int[] size = imageSize(mimeType, bytes);

        String assetId = sha256Hex(bytes);
        Asset asset = assetRepository.findById(assetId).orElseGet(() -> {
            String ext = EXTENSIONS.get(mimeType);
            Path target = Path.of("uploads", assetId + "." + ext);
            try {
                Files.createDirectories(target.getParent());
                Files.write(target, bytes);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Не удалось сохранить файл");
            }
            return assetRepository.saveAndFlush(Asset.builder()
                    .id(assetId)
                    .url("/assets/" + assetId + "." + ext)
                    .mimeType(mimeType)
                    .sizeBytes((long) bytes.length)
                    .width(size[0])
                    .height(size[1])
                    .altText(altText)
                    .build());
        });

        boolean attached = card.getCardAssets().stream()
                .anyMatch(ca -> ca.getAsset().getId().equals(assetId));
        if (attached) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Изображение уже привязано к этой карточке");
        }
        card.getCardAssets().add(CardAsset.builder().card(card).asset(asset).build());
        cardRepository.save(card);
        return AssetDto.from(asset);
    }

    /**
     * DELETE /{assetId} — отвязать картинку от карточки. 204.
     */
    @DeleteMapping("/{assetId}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID cardId, @PathVariable String assetId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Карточка с id=%s не найдена".formatted(cardId)));
        boolean removed = card.getCardAssets()
                .removeIf(ca -> ca.getAsset().getId().equals(assetId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Изображение %s не привязано к карточке %s".formatted(assetId, cardId));
        }
        cardRepository.save(card);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------------

    /** Размеры изображения: ImageIO для jpeg/png, ручной разбор заголовка для webp. */
    private static int[] imageSize(String mimeType, byte[] bytes) {
        if ("image/webp".equals(mimeType)) {
            int[] size = webpSize(bytes);
            if (size == null) {
                throw badRequest("Не удалось прочитать размеры webp-изображения");
            }
            return size;
        }
        try {
            BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            if (image == null) {
                throw badRequest("Файл не является валидным изображением");
            }
            return new int[]{image.getWidth(), image.getHeight()};
        } catch (IOException e) {
            throw badRequest("Файл не является валидным изображением");
        }
    }

    private static int[] webpSize(byte[] b) {
        if (b.length < 30 || b[0] != 'R' || b[1] != 'I' || b[2] != 'F' || b[3] != 'F') {
            return null;
        }
        String chunk = new String(b, 12, 4, StandardCharsets.US_ASCII);
        int p = 20; // 12 байт RIFF-заголовок + 8 байт заголовка чанка
        return switch (chunk) {
            case "VP8 " -> new int[]{
                    ((b[p + 6] & 0xFF) | (b[p + 7] & 0xFF) << 8) & 0x3FFF,
                    ((b[p + 8] & 0xFF) | (b[p + 9] & 0xFF) << 8) & 0x3FFF};
            case "VP8L" -> {
                int bits = (b[p + 1] & 0xFF) | (b[p + 2] & 0xFF) << 8
                        | (b[p + 3] & 0xFF) << 16 | (b[p + 4] & 0xFF) << 24;
                yield new int[]{(bits & 0x3FFF) + 1, ((bits >> 14) & 0x3FFF) + 1};
            }
            case "VP8X" -> new int[]{
                    1 + ((b[p + 4] & 0xFF) | (b[p + 5] & 0xFF) << 8 | (b[p + 6] & 0xFF) << 16),
                    1 + ((b[p + 7] & 0xFF) | (b[p + 8] & 0xFF) << 8 | (b[p + 9] & 0xFF) << 16)};
            default -> null;
        };
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 недоступен", e);
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
