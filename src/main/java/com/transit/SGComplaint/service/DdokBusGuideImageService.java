package com.transit.SGComplaint.service;

import com.transit.SGComplaint.DTO.DdokBusGuideImageItem;
import com.transit.SGComplaint.DTO.StoredAttachment;
import com.transit.SGComplaint.domain.DdokBusGuideImage;
import com.transit.SGComplaint.mapper.DdokBusGuideImageMapper;
import com.transit.SGComplaint.repository.DdokBusGuideImageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DdokBusGuideImageService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

    private final DdokBusGuideImageRepository imageRepository;
    private final DdokBusGuideImageMapper imageMapper;
    private final Path storageRoot;

    public DdokBusGuideImageService(
            DdokBusGuideImageRepository imageRepository,
            DdokBusGuideImageMapper imageMapper,
            @Value("${app.upload.ddokbus-guide-dir}")
            String storageDirectory) {
        this.imageRepository = imageRepository;
        this.imageMapper = imageMapper;
        this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public List<DdokBusGuideImageItem> getImages() {
        return imageMapper.findAllOrdered()
                .stream().map(this::toItem).toList();
    }

    public long countImages() {
        return imageMapper.countAll();
    }

    @Transactional
    public Long addImage(MultipartFile file) {
        validateFile(file);
        String originalName = safeOriginalName(file);
        String extension = extensionOf(originalName);
        String storedName = UUID.randomUUID() + "." + extension;
        Path destination = storageRoot.resolve(storedName).normalize();
        ensureInsideStorage(destination);

        try {
            Files.createDirectories(storageRoot);
            UploadValidation.copy(file, storageRoot, destination);
            DdokBusGuideImage image = DdokBusGuideImage.create(
                    originalName,
                    storedName,
                    storedName,
                    contentType(file, extension),
                    file.getSize());
            return imageRepository.saveAndFlush(image).getImageNo();
        } catch (IOException | RuntimeException exception) {
            deleteFileQuietly(destination);
            if (exception instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new IllegalStateException("똑버스 안내 이미지 저장 중 오류가 발생했습니다.", exception);
        }
    }

    @Transactional
    public void deleteImage(Long imageNo) {
        DdokBusGuideImage image = imageRepository.findById(imageNo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "삭제할 똑버스 안내 이미지를 찾을 수 없습니다."));
        Path storedFile = resolveStoredFile(image);
        imageRepository.delete(image);
        imageRepository.flush();
        deleteFileQuietly(storedFile);
    }

    public StoredAttachment getImage(Long imageNo) {
        DdokBusGuideImage image = imageRepository.findById(imageNo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "똑버스 안내 이미지를 찾을 수 없습니다."));
        Path storedFile = resolveStoredFile(image);
        if (!Files.isRegularFile(storedFile)) {
            throw new IllegalArgumentException("저장된 똑버스 안내 이미지를 찾을 수 없습니다.");
        }
        return new StoredAttachment(
                storedFile, image.getOriginalName(), image.getContentType());
    }

    private void validateFile(MultipartFile file) {
        UploadValidation.validate(file, MAX_FILE_SIZE, ALLOWED_EXTENSIONS);
    }

    private String safeOriginalName(MultipartFile file) {
        return UploadValidation.filename(file);
    }

    private String extensionOf(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) return "";
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String contentType(MultipartFile file, String extension) {
        return UploadValidation.contentType(extension);
    }

    private Path resolveStoredFile(DdokBusGuideImage image) {
        Path file = storageRoot.resolve(image.getFilePath()).normalize();
        ensureInsideStorage(file);
        return file;
    }

    private void ensureInsideStorage(Path path) {
        UploadValidation.ensureInside(storageRoot, path);
        if (!path.startsWith(storageRoot)) {
            throw new IllegalArgumentException("올바르지 않은 이미지 저장 경로입니다.");
        }
    }

    private void deleteFileQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // DB 처리를 유지하고 남은 파일은 운영 정리 대상으로 둡니다.
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private DdokBusGuideImageItem toItem(DdokBusGuideImage image) {
        return new DdokBusGuideImageItem(
                image.getImageNo(),
                image.getOriginalName(),
                formatFileSize(image.getFileSize()),
                image.getCreatedAt().format(DATE_TIME_FORMATTER),
                "/route-guide-images/" + image.getImageNo() + "/image");
    }
}
