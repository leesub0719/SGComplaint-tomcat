package com.transit.SGComplaint.service;

import com.transit.SGComplaint.DTO.MainBannerItem;
import com.transit.SGComplaint.DTO.StoredAttachment;
import com.transit.SGComplaint.domain.Employee;
import com.transit.SGComplaint.domain.MainBanner;
import com.transit.SGComplaint.mapper.MainBannerMapper;
import com.transit.SGComplaint.mapper.EmployeeMapper;
import com.transit.SGComplaint.repository.MainBannerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class MainBannerService {

    private static final int MAX_BANNER_COUNT = 5;
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

    private final MainBannerRepository bannerRepository;
    private final MainBannerMapper bannerMapper;
    private final EmployeeMapper employeeRepository;
    private final AdminDeletionAuditService deletionAuditService;
    private final Path storageRoot;

    public MainBannerService(
            MainBannerRepository bannerRepository,
            MainBannerMapper bannerMapper,
            EmployeeMapper employeeRepository,
            AdminDeletionAuditService deletionAuditService,
            @Value("${app.upload.main-banner-dir}")
            String storageDirectory) {
        this.bannerRepository = bannerRepository;
        this.bannerMapper = bannerMapper;
        this.employeeRepository = employeeRepository;
        this.deletionAuditService = deletionAuditService;
        this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public List<MainBannerItem> getBanners() {
        return bannerMapper.findAllOrdered()
                .stream()
                .map(this::toItem)
                .toList();
    }

    public long countBanners() {
        return bannerMapper.countAll();
    }

    public int remainingCount() {
        return Math.max(0, MAX_BANNER_COUNT - (int) bannerMapper.countAll());
    }

    @Transactional
    public int addBanners(String loginId, List<MultipartFile> files) {
        requireAdministrator(loginId);
        List<MultipartFile> uploadFiles = files == null
                ? List.of()
                : files.stream().filter(UploadValidation::isSelected).toList();
        if (uploadFiles.isEmpty()) {
            throw new IllegalArgumentException("등록할 메인 배너 이미지를 선택해 주세요.");
        }
        long currentCount = bannerMapper.countAll();
        if (currentCount + uploadFiles.size() > MAX_BANNER_COUNT) {
            throw new IllegalArgumentException(
                    "메인 배너는 최대 5장까지 등록할 수 있습니다. 현재 "
                            + currentCount + "장이 등록되어 있습니다."
            );
        }
        uploadFiles.forEach(this::validateFile);

        int nextOrder = bannerMapper.findMaximumDisplayOrder() + 1;
        List<Path> savedPaths = new ArrayList<>();
        try {
            Files.createDirectories(storageRoot);
            for (MultipartFile file : uploadFiles) {
                String originalName = safeOriginalName(file);
                String extension = extensionOf(originalName);
                String storedName = UUID.randomUUID() + "." + extension;
                Path destination = storageRoot.resolve(storedName).normalize();
                ensureInsideStorage(destination);
                UploadValidation.copy(file, storageRoot, destination);
                savedPaths.add(destination);

                MainBanner banner = MainBanner.create(
                        originalName,
                        storedName,
                        storedName,
                        contentType(file, extension),
                        file.getSize(),
                        nextOrder++,
                        loginId
                );
                bannerRepository.save(banner);
            }
            bannerRepository.flush();
            return uploadFiles.size();
        } catch (IOException | RuntimeException exception) {
            savedPaths.forEach(this::deleteFileQuietly);
            if (exception instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new IllegalStateException("메인 배너 저장 중 오류가 발생했습니다.", exception);
        }
    }

    @Transactional
    public void deleteBanner(String loginId, Long bannerNo) {
        Employee administrator = requireAdministrator(loginId);
        MainBanner banner = bannerRepository.findById(bannerNo)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 배너를 찾을 수 없습니다."));
        Path file = resolveStoredFile(banner);
        deletionAuditService.record(administrator, "MAIN_BANNER", bannerNo,
                banner.getOriginalName(), Map.of(
                        "originalName", banner.getOriginalName(),
                        "filePath", banner.getFilePath(),
                        "fileSize", banner.getFileSize(),
                        "displayOrder", banner.getDisplayOrder(),
                        "createdBy", banner.getCreatedBy()));
        bannerRepository.delete(banner);
        bannerRepository.flush();
        deleteFileQuietly(file);
    }

    public StoredAttachment getBannerImage(Long bannerNo) {
        MainBanner banner = bannerRepository.findById(bannerNo)
                .orElseThrow(() -> new IllegalArgumentException("배너 이미지를 찾을 수 없습니다."));
        Path file = resolveStoredFile(banner);
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("저장된 배너 이미지를 찾을 수 없습니다.");
        }
        return new StoredAttachment(file, banner.getOriginalName(), banner.getContentType());
    }

    private MainBannerItem toItem(MainBanner banner) {
        return new MainBannerItem(
                banner.getBannerNo(),
                banner.getOriginalName(),
                formatFileSize(banner.getFileSize()),
                banner.getCreatedBy(),
                banner.getCreatedAt().format(DATE_TIME_FORMATTER),
                "/main-banners/" + banner.getBannerNo() + "/image"
        );
    }

    private Employee requireAdministrator(String loginId) {
        return employeeRepository.findByEmpIdAndEmpStatus(loginId, "Y")
                .filter(Employee::hasAdminRole)
                .orElseThrow(() -> new IllegalStateException("관리자 권한을 확인할 수 없습니다."));
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

    private Path resolveStoredFile(MainBanner banner) {
        Path file = storageRoot.resolve(banner.getFilePath()).normalize();
        ensureInsideStorage(file);
        return file;
    }

    private void ensureInsideStorage(Path path) {
        UploadValidation.ensureInside(storageRoot, path);
        if (!path.startsWith(storageRoot)) {
            throw new IllegalArgumentException("올바르지 않은 배너 저장 경로입니다.");
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
        if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
