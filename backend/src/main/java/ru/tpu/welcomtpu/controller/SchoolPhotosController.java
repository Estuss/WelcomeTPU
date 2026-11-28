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
import ru.tpu.welcomtpu.model.School;
import ru.tpu.welcomtpu.model.SchoolAsset;
import ru.tpu.welcomtpu.repository.AssetRepository;
import ru.tpu.welcomtpu.repository.SchoolAssetRepository;
import ru.tpu.welcomtpu.repository.SchoolRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;

/**
 * Фотографии школ: POST / DELETE /api/v1/admin/schools/{schoolId}/photos.
 * Файлы пишутся в ./uploads/{assetId}.{ext}, отдаются через /assets/**.
 */
@RestController
@RequestMapping("/api/v1/admin/schools/{schoolId}/photos")
@RequiredArgsConstructor
public class SchoolPhotosController {

    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10 МБ
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");

    private final SchoolRepository schoolRepository;
    private final SchoolAssetRepository schoolAssetRepository;
    private final AssetRepository assetRepository;

    /**
     * POST — загрузить фотографию и привязать к школе. 201 Created.
     */
    @PostMapping
    @Transactional
    public ResponseEntity<AssetDto> upload(@PathVariable Long schoolId,
                                           @RequestParam("file") MultipartFile file,
                                           @RequestParam(name = "alt_text", required = false) String altText) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> notFound(schoolId));

        if (file == null || file.isEmpty()) {
            throw badRequest("Файл 'file' обязателен");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Файл больше 10 МБ");
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
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось сохранить файл");
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

        boolean attached = school.getPhotos().stream()
                .anyMatch(sa -> sa.getAsset().getId().equals(assetId));
        if (attached) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Фотография уже привязана к этой школе");
        }
        int position = school.getPhotos().stream()
                .mapToInt(SchoolAsset::getPosition)
                .max()
                .orElse(-1) + 1;
        school.getPhotos().add(SchoolAsset.builder().school(school).asset(asset).position(position).build());
        schoolRepository.save(school);
        return ResponseEntity.status(HttpStatus.CREATED).body(AssetDto.from(asset));
    }

    /**
     * DELETE /{assetId} — отвязать фотографию от школы. 204.
     */
    @DeleteMapping("/{assetId}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long schoolId, @PathVariable String assetId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> notFound(schoolId));
        boolean removed = school.getPhotos().removeIf(sa -> sa.getAsset().getId().equals(assetId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Фотография %s не привязана к школе %d".formatted(assetId, schoolId));
        }
        schoolRepository.save(school);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------------

    private static ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Школа с id=%d не найдена".formatted(id));
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

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
        if (b[8] != 'W' || b[9] != 'E' || b[10] != 'B' || b[11] != 'P') {
            return null;
        }
        String chunk = new String(b, 12, 4, StandardCharsets.US_ASCII);
        if ("VP8 ".equals(chunk)) {
            int w = ((b[26] & 0xFF) | (b[27] & 0xFF) << 8) & 0x3FFF;
            int h = ((b[28] & 0xFF) | (b[29] & 0xFF) << 8) & 0x3FFF;
            return new int[]{w, h};
        }
        if ("VP8L".equals(chunk)) {
            int bits = (b[21] & 0xFF) | (b[22] & 0xFF) << 8 | (b[23] & 0xFF) << 16 | (b[24] & 0xFF) << 24;
            int w = (bits & 0x3FFF) + 1;
            int h = ((bits >> 14) & 0x3FFF) + 1;
            return new int[]{w, h};
        }
        if ("VP8X".equals(chunk)) {
            int w = 1 + (b[24] & 0xFF) | (b[25] & 0xFF) << 8 | (b[26] & 0xFF) << 16;
            int h = 1 + (b[27] & 0xFF) | (b[28] & 0xFF) << 8 | (b[29] & 0xFF) << 16;
            return new int[]{w, h};
        }
        return null;
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 недоступен", e);
        }
    }
}
