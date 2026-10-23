package com.transit.SGComplaint.service;

import com.transit.SGComplaint.domain.DdokBusGuideImage;
import com.transit.SGComplaint.mapper.DdokBusGuideImageMapper;
import com.transit.SGComplaint.repository.DdokBusGuideImageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UploadValidationTests {
    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "gif", "webp",
            "pdf", "doc", "docx", "hwp", "hwpx");
    private static final long LIMIT = 10L * 1024 * 1024;
    @TempDir Path root;

    private MockMultipartFile file(String name, byte[] bytes) {
        // Supplied MIME is deliberately forged. It must never decide validation/storage MIME.
        return new MockMultipartFile("files", name, "text/html", bytes);
    }

    private byte[] png() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
        return output.toByteArray();
    }

    private void validate(String name, byte[] bytes) {
        UploadValidation.validate(file(name, bytes), LIMIT, ALLOWED);
    }

    private byte[] archive(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }

    @Test void validPngIgnoresForgedMimeAndUsesServerType() throws Exception {
        validate("사진.PNG", png());
        assertEquals("image/png", UploadValidation.contentType("png"));
    }

    @Test void imageExtensionMustMatchActualImage() throws Exception {
        byte[] bytes = png();
        assertThrows(IllegalArgumentException.class, () -> validate("photo.jpg", bytes));
        assertThrows(IllegalArgumentException.class, () -> validate("photo.png", "<script>alert(1)</script>".getBytes()));
        assertThrows(IllegalArgumentException.class, () -> validate("photo.svg", bytes));
    }

    @Test void rejectsEmptyOversizedAndReportedSizeMismatch() {
        assertThrows(IllegalArgumentException.class, () -> validate("empty.png", new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> UploadValidation.validate(
                file("large.pdf", new byte[11]), 10, ALLOWED));
        var mismatch = new MockMultipartFile("files", "fake.pdf", null, new byte[5]) {
            @Override public long getSize() { return 1; }
        };
        assertThrows(IllegalArgumentException.class, () -> UploadValidation.validate(mismatch, LIMIT, ALLOWED));
    }

    @Test void emptySelectionAndEmptyNamedFileAreDifferent() {
        assertFalse(UploadValidation.isSelected(file("", new byte[0])));
        assertTrue(UploadValidation.isSelected(file("empty.png", new byte[0])));
    }

    @Test void rejectsTraversalControlCharactersAndDisguisedNames() throws Exception {
        byte[] bytes = png();
        for (String name : List.of("../x.png", "a/b.png", "a\\b.png", "a\r\n.png",
                "a\u202e.png", "x.png ", "x.png.exe", "x:png", ".png")) {
            assertThrows(IllegalArgumentException.class, () -> validate(name, bytes), name);
        }
        assertEquals("photo.png", UploadValidation.filename(file("C:\\fakepath\\photo.png", bytes)));
    }

    @Test void truncatedImageIsRejected() throws Exception {
        byte[] bytes = png();
        assertThrows(IllegalArgumentException.class, () -> validate("broken.png", Arrays.copyOf(bytes, 35)));
    }

    @Test void excessiveImageDimensionsAreRejectedBeforeDecode() throws Exception {
        byte[] bytes = png();
        // PNG IHDR width = 10001 with corrected CRC.
        bytes[16] = 0; bytes[17] = 0; bytes[18] = 39; bytes[19] = 17;
        CRC32 crc = new CRC32();
        crc.update(bytes, 12, 17);
        long value = crc.getValue();
        for (int i = 0; i < 4; i++) bytes[29 + i] = (byte) (value >>> (24 - 8 * i));
        assertThrows(IllegalArgumentException.class, () -> validate("large.png", bytes));
    }

    @Test void checksPdfSignatureAndEndMarker() {
        validate("document.pdf", "%PDF-1.7\n%%EOF".getBytes(StandardCharsets.US_ASCII));
        assertThrows(IllegalArgumentException.class, () -> validate("document.pdf", "not a pdf".getBytes()));
        assertThrows(IllegalArgumentException.class, () -> validate("document.pdf", "%PDF-1.7\n".getBytes()));
    }

    @Test void checksOfficeAndHwpArchiveIdentity() throws Exception {
        byte[] xml = "<document/>".getBytes(StandardCharsets.UTF_8);
        validate("document.docx", archive(Map.of("[Content_Types].xml", xml, "word/document.xml", xml)));
        validate("document.hwpx", archive(Map.of("mimetype", "application/hwp+zip".getBytes(),
                "Contents/content.hpf", xml, "Contents/section0.xml", xml)));
        byte[] ordinaryZip = archive(Map.of("hello.txt", xml));
        assertThrows(IllegalArgumentException.class, () -> validate("document.docx", ordinaryZip));
        assertThrows(IllegalArgumentException.class, () -> validate("document.hwp", ordinaryZip));
    }

    @Test void rejectsArchiveTraversalMacrosAndExpansionBomb() throws Exception {
        byte[] xml = "<document/>".getBytes();
        byte[] traversal = archive(Map.of("../escape", xml));
        assertThrows(IllegalArgumentException.class, () -> validate("document.docx", traversal));
        byte[] macro = archive(Map.of("[Content_Types].xml", xml, "word/document.xml", xml,
                "word/vbaProject.bin", xml));
        assertThrows(IllegalArgumentException.class, () -> validate("document.docx", macro));
        byte[] bomb = archive(Map.of("[Content_Types].xml", xml,
                "word/document.xml", new byte[21 * 1024 * 1024]));
        assertThrows(IllegalArgumentException.class, () -> validate("document.docx", bomb));
    }

    @Test void rejectsTruncatedWebpAndMissingImageChunk() {
        byte[] riff = "RIFF\u0004\u0000\u0000\u0000WEBP".getBytes(StandardCharsets.ISO_8859_1);
        assertThrows(IllegalArgumentException.class, () -> validate("image.webp", riff));
    }

    @Test void preventsPathEscapeAndOverwrite() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> UploadValidation.ensureInside(root, root.resolve("../outside")));
        Path target = root.resolve("existing.png");
        Files.writeString(target, "keep");
        assertThrows(FileAlreadyExistsException.class, () -> UploadValidation.copy(file("x.png", png()), root, target));
        assertEquals("keep", Files.readString(target));
    }

    @Test void failedCopyRemovesPartialFile() throws Exception {
        var broken = new MockMultipartFile("files", "x.png", null, new byte[5]) {
            @Override public long getSize() { return 1; }
        };
        Path target = root.resolve("partial.png");
        assertThrows(IllegalArgumentException.class, () -> UploadValidation.copy(broken, root, target));
        assertFalse(Files.exists(target));
    }

    @Test void transactionRollbackDeletesNewUpload() throws Exception {
        Path target = root.resolve("rollback.png");
        TransactionSynchronizationManager.initSynchronization();
        try {
            UploadValidation.copy(file("x.png", png()), root, target);
            assertTrue(Files.exists(target));
            for (var callback : TransactionSynchronizationManager.getSynchronizations()) {
                callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }
            assertFalse(Files.exists(target));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test void transactionCommitKeepsNewUpload() throws Exception {
        Path target = root.resolve("commit.png");
        TransactionSynchronizationManager.initSynchronization();
        try {
            UploadValidation.copy(file("x.png", png()), root, target);
            for (var callback : TransactionSynchronizationManager.getSynchronizations()) {
                callback.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            }
            assertTrue(Files.exists(target));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test void ddokbusServiceRejectsFakeImageBeforeDbWrite() {
        var repository = mock(DdokBusGuideImageRepository.class);
        var service = new DdokBusGuideImageService(repository, mock(DdokBusGuideImageMapper.class), root.toString());
        assertThrows(IllegalArgumentException.class, () -> service.addImage(file("fake.png", "html".getBytes())));
        verifyNoInteractions(repository);
    }

    @Test void ddokbusServiceStoresUuidAndTrustedMime() throws Exception {
        var repository = mock(DdokBusGuideImageRepository.class);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new DdokBusGuideImageService(repository, mock(DdokBusGuideImageMapper.class), root.toString());
        service.addImage(file("사진.png", png()));
        var captor = org.mockito.ArgumentCaptor.forClass(DdokBusGuideImage.class);
        verify(repository).saveAndFlush(captor.capture());
        assertEquals("image/png", captor.getValue().getContentType());
        assertTrue(captor.getValue().getStoredName().matches("[a-f0-9-]{36}\\.png"));
        assertTrue(Files.exists(root.resolve(captor.getValue().getStoredName())));
    }

    @Test void ddokbusDbFailureRemovesSavedFile() throws Exception {
        var repository = mock(DdokBusGuideImageRepository.class);
        when(repository.saveAndFlush(any())).thenThrow(new IllegalStateException("DB failed"));
        var service = new DdokBusGuideImageService(repository, mock(DdokBusGuideImageMapper.class), root.toString());
        assertThrows(IllegalStateException.class, () -> service.addImage(file("사진.png", png())));
        try (var paths = Files.list(root)) { assertEquals(0, paths.count()); }
    }
}
