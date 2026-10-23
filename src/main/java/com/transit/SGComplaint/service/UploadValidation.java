package com.transit.SGComplaint.service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Shared server-side checks. Format validation is not an antivirus scan. */
public final class UploadValidation {
    private static final long MAX_PIXELS = 20_000_000L;
    private static final long MAX_EXPANDED_BYTES = 50L * 1024 * 1024;
    private static final Set<String> IMAGES = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private UploadValidation() { }

    // An unselected HTML file field is ignored; a selected zero-byte file is rejected.
    public static boolean isSelected(MultipartFile file) {
        return file != null && (!file.isEmpty()
                || (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()));
    }

    public static String filename(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null) {
            throw invalid("파일 이름이 없습니다.");
        }
        String name = Normalizer.normalize(file.getOriginalFilename(), Normalizer.Form.NFC);
        if (name.startsWith("C:\\fakepath\\")) name = name.substring(12);
        if (name.isBlank() || !name.equals(name.trim()) || name.length() > 255
                || name.contains("/") || name.contains("\\") || name.contains(":")
                || name.codePoints().anyMatch(c -> Character.isISOControl(c)
                    || Character.getType(c) == Character.FORMAT)) {
            throw invalid("올바르지 않은 파일 이름입니다.");
        }
        return name;
    }

    public static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 1 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public static String contentType(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "hwp" -> "application/x-hwp";
            case "hwpx" -> "application/hwp+zip";
            default -> throw invalid("허용하지 않는 파일 형식입니다.");
        };
    }

    public static String imageContentType(String filename) {
        String extension = extension(filename);
        if (!IMAGES.contains(extension)) throw invalid("허용하지 않는 이미지 형식입니다.");
        return contentType(extension);
    }

    public static void validate(MultipartFile file, long maxBytes, Set<String> allowed) {
        String extension = extension(filename(file));
        if (!allowed.contains(extension)) throw invalid("첨부할 수 없는 파일 형식입니다.");
        if (file.isEmpty() || file.getSize() <= 0 || file.getSize() > maxBytes) {
            throw invalid("빈 파일이거나 파일 크기가 허용 범위를 초과했습니다.");
        }
        try (InputStream input = file.getInputStream()) {
            byte[] bytes = input.readNBytes(Math.toIntExact(maxBytes + 1));
            if (bytes.length == 0 || bytes.length > maxBytes || bytes.length != file.getSize()) {
                throw invalid("파일 크기가 올바르지 않습니다.");
            }
            if (IMAGES.contains(extension)) {
                if ("webp".equals(extension)) validateWebp(bytes);
                else validateImage(bytes, extension);
            } else {
                switch (extension) {
                    case "pdf" -> {
                        String head = new String(bytes, 0, Math.min(8, bytes.length), StandardCharsets.US_ASCII);
                        String tail = new String(bytes, Math.max(0, bytes.length - 1024),
                                Math.min(1024, bytes.length), StandardCharsets.US_ASCII);
                        if (!head.matches("%PDF-[12]\\.[0-9]") || !tail.contains("%%EOF")) {
                            throw invalid("PDF 파일 형식이 올바르지 않습니다.");
                        }
                    }
                    case "doc", "hwp" -> validateCompoundDocument(bytes, extension);
                    case "docx", "hwpx" -> validateArchive(bytes, extension);
                    default -> throw invalid("허용하지 않는 파일 형식입니다.");
                }
            }
        } catch (IOException | ArithmeticException exception) {
            throw invalid("파일을 읽거나 형식을 확인할 수 없습니다.");
        }
    }

    private static void checkDimensions(long width, long height) {
        if (width < 1 || height < 1 || width > 10000 || height > 10000
                || width * height > MAX_PIXELS) {
            throw invalid("이미지는 가로·세로 10,000px 이하, 총 2,000만 화소 이하만 가능합니다.");
        }
    }

    private static void validateImage(byte[] bytes, String extension) throws IOException {
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid("실제 이미지 파일이 아닙니다.");
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                String expected = "jpg".equals(extension) ? "jpeg" : extension;
                if (!format.equals(expected)) throw invalid("확장자와 실제 이미지 형식이 다릅니다.");
                reader.setInput(input);
                reader.addIIOReadWarningListener((source, warning) -> {
                    throw invalid("손상되었거나 불완전한 이미지입니다.");
                });
                int frames = reader.getNumImages(true);
                if (frames < 1 || frames > 100) throw invalid("이미지 프레임은 최대 100개입니다.");
                long totalPixels = 0;
                for (int index = 0; index < frames; index++) {
                    int width = reader.getWidth(index);
                    int height = reader.getHeight(index);
                    checkDimensions(width, height);
                    totalPixels += (long) width * height;
                    if (totalPixels > MAX_PIXELS) throw invalid("이미지 전체 프레임의 용량이 너무 큽니다.");
                    var image = reader.read(index);
                    if (image == null) throw invalid("손상된 이미지입니다.");
                    image.flush();
                }
            } finally {
                reader.dispose();
            }
        }
    }

    // JDK ImageIO has no WebP decoder. Validate RIFF/chunk boundaries and image dimensions.
    private static void validateWebp(byte[] bytes) {
        if (bytes.length < 20 || !ascii(bytes, 0, 4).equals("RIFF")
                || !ascii(bytes, 8, 4).equals("WEBP") || u32(bytes, 4) + 8 != bytes.length) {
            throw invalid("WEBP 파일 형식이 올바르지 않습니다.");
        }
        boolean imageFound = false;
        for (int offset = 12; offset < bytes.length;) {
            if (offset + 8 > bytes.length) throw invalid("손상된 WEBP 파일입니다.");
            String chunk = ascii(bytes, offset, 4);
            long size = u32(bytes, offset + 4);
            int data = offset + 8;
            long end = data + size + (size & 1);
            if (end > bytes.length) throw invalid("손상된 WEBP 파일입니다.");
            if (chunk.equals("ANIM") || chunk.equals("ANMF")) {
                throw invalid("움직이는 WEBP는 지원하지 않습니다. JPG 또는 PNG로 변환해 주세요.");
            }
            if (chunk.equals("VP8X")) {
                if (size != 10) throw invalid("손상된 WEBP 헤더입니다.");
                checkDimensions(1 + u24(bytes, data + 4), 1 + u24(bytes, data + 7));
            } else if (chunk.equals("VP8 ")) {
                if (size < 10 || (bytes[data] & 1) != 0 || (bytes[data + 3] & 255) != 0x9d
                        || (bytes[data + 4] & 255) != 1 || (bytes[data + 5] & 255) != 0x2a) {
                    throw invalid("손상된 WEBP 이미지입니다.");
                }
                checkDimensions(u16(bytes, data + 6) & 0x3fff, u16(bytes, data + 8) & 0x3fff);
                if (imageFound) throw invalid("중복 WEBP 이미지입니다.");
                imageFound = true;
            } else if (chunk.equals("VP8L")) {
                if (size < 5 || (bytes[data] & 255) != 0x2f) throw invalid("손상된 WEBP 이미지입니다.");
                long bits = u32(bytes, data + 1);
                if ((bits >>> 29) != 0) throw invalid("지원하지 않는 WEBP 버전입니다.");
                checkDimensions((bits & 0x3fff) + 1, ((bits >>> 14) & 0x3fff) + 1);
                if (imageFound) throw invalid("중복 WEBP 이미지입니다.");
                imageFound = true;
            }
            offset = (int) end;
        }
        if (!imageFound) throw invalid("WEBP 이미지 데이터가 없습니다.");
    }

    private static void validateCompoundDocument(byte[] bytes, String extension) {
        byte[] magic = {(byte) 0xd0, (byte) 0xcf, 0x11, (byte) 0xe0,
                (byte) 0xa1, (byte) 0xb1, 0x1a, (byte) 0xe1};
        if (bytes.length < 512 || !Arrays.equals(Arrays.copyOf(bytes, 8), magic)
                || u16(bytes, 28) != 0xfffe || (u16(bytes, 30) != 9 && u16(bytes, 30) != 12)) {
            throw invalid("DOC/HWP 문서 헤더가 올바르지 않습니다.");
        }
        // Basic container identification only, not execution or full document parsing.
        String container = new String(bytes, StandardCharsets.UTF_16LE);
        if ("doc".equals(extension) ? !container.contains("WordDocument")
                : !(container.contains("FileHeader") && container.contains("BodyText"))) {
            throw invalid("확장자와 문서 컨테이너 형식이 다릅니다.");
        }
    }

    private static void validateArchive(byte[] bytes, String extension) throws IOException {
        if (bytes.length < 4 || u32(bytes, 0) != 0x04034b50L) throw invalid("압축 문서 형식이 아닙니다.");
        int end = -1;
        for (int offset = bytes.length - 22; offset >= Math.max(0, bytes.length - 65557); offset--) {
            if (u32(bytes, offset) == 0x06054b50L
                    && offset + 22 + u16(bytes, offset + 20) == bytes.length) {
                end = offset;
                break;
            }
        }
        if (end < 0 || u16(bytes, end + 4) != 0 || u16(bytes, end + 6) != 0
                || u16(bytes, end + 8) != u16(bytes, end + 10)
                || u32(bytes, end + 12) + u32(bytes, end + 16) != end) {
            throw invalid("압축 문서의 끝부분이 손상되었습니다.");
        }
        int expectedEntries = u16(bytes, end + 10);
        Set<String> names = new HashSet<>();
        long expanded = 0;
        String mimetype = "";
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (names.size() >= 1024 || !names.add(name) || name.startsWith("/")
                        || name.contains("\\") || name.contains(":")
                        || Arrays.asList(name.split("/")).contains("..")
                        || name.codePoints().anyMatch(Character::isISOControl)) {
                    throw invalid("압축 문서 내부 경로가 올바르지 않습니다.");
                }
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.contains("vbaproject") || lower.endsWith(".exe") || lower.endsWith(".js")
                        || lower.endsWith(".vbs") || lower.endsWith(".bat") || lower.endsWith(".cmd")) {
                    throw invalid("실행 파일 또는 매크로가 포함된 압축 문서는 허용하지 않습니다.");
                }
                ByteArrayOutputStream type = name.equals("mimetype") ? new ByteArrayOutputStream() : null;
                long entryBytes = 0;
                int length;
                while ((length = zip.read(buffer)) != -1) {
                    expanded += length;
                    entryBytes += length;
                    if (expanded > MAX_EXPANDED_BYTES || entryBytes > 20L * 1024 * 1024) {
                        throw invalid("압축을 푼 문서 크기가 허용 범위를 초과했습니다.");
                    }
                    if (type != null) {
                        if (entryBytes > 128) throw invalid("문서 mimetype 정보가 올바르지 않습니다.");
                        type.write(buffer, 0, length);
                    }
                }
                if (type != null) mimetype = type.toString(StandardCharsets.US_ASCII).trim();
                zip.closeEntry();
            }
        }
        boolean valid = "docx".equals(extension)
                ? names.contains("[Content_Types].xml") && names.contains("word/document.xml")
                : names.contains("Contents/content.hpf") && names.contains("Contents/section0.xml")
                    && mimetype.equals("application/hwp+zip");
        if (!valid || names.size() != expectedEntries) {
            throw invalid("확장자와 압축 문서 내부 형식이 다르거나 문서가 손상되었습니다.");
        }
    }

    public static void ensureInside(Path root, Path path) {
        root = root.toAbsolutePath().normalize();
        path = path.toAbsolutePath().normalize();
        if (!path.startsWith(root)) throw invalid("허용하지 않는 파일 저장 경로입니다.");
        for (Path part = path; part != null && part.startsWith(root); part = part.getParent()) {
            if (Files.isSymbolicLink(part)) throw invalid("심볼릭 링크 경로는 허용하지 않습니다.");
        }
    }

    public static void copy(MultipartFile file, Path root, Path destination) throws IOException {
        ensureInside(root, destination);
        boolean created = false;
        try (InputStream input = file.getInputStream()) {
            try (OutputStream output = Files.newOutputStream(destination,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                created = true;
                long total = 0;
                byte[] buffer = new byte[8192];
                int length;
                while ((length = input.read(buffer)) != -1) {
                    total += length;
                    if (total > file.getSize() || total > 10L * 1024 * 1024) {
                        throw invalid("파일 저장 중 크기가 변경되었습니다.");
                    }
                    output.write(buffer, 0, length);
                }
                if (total != file.getSize()) throw invalid("파일이 완전히 저장되지 않았습니다.");
            }
        } catch (IOException | RuntimeException exception) {
            if (created) Files.deleteIfExists(destination);
            throw exception;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        try { Files.deleteIfExists(destination); } catch (IOException ignored) { }
                    }
                }
            });
        }
    }

    private static String ascii(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, StandardCharsets.US_ASCII);
    }
    private static int u16(byte[] bytes, int offset) {
        return (bytes[offset] & 255) | ((bytes[offset + 1] & 255) << 8);
    }
    private static long u24(byte[] bytes, int offset) {
        return u16(bytes, offset) | ((long) (bytes[offset + 2] & 255) << 16);
    }
    private static long u32(byte[] bytes, int offset) {
        return u24(bytes, offset) | ((long) (bytes[offset + 3] & 255) << 24);
    }
    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }
}
