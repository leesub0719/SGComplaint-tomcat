package com.transit.SGComplaint.service;

import com.transit.SGComplaint.DTO.JobApplicationRequest;
import com.transit.SGComplaint.DTO.JobPostingRequest;
import com.transit.SGComplaint.domain.JobApplication;
import com.transit.SGComplaint.domain.JobPosting;
import com.transit.SGComplaint.mapper.RecruitmentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecruitmentServiceTests {
    private final Path storage = Path.of("build", "test-storage");

    @Test void closedPostingRejectsApplicationBeforeDatabaseWrite() {
        RecruitmentMapper mapper = mock(RecruitmentMapper.class);
        JobPosting posting = posting("N", LocalDate.now().plusDays(10));
        when(mapper.findPosting(1L)).thenReturn(Optional.of(posting));
        RecruitmentService service = new RecruitmentService(mapper, storage.toString());

        assertThrows(IllegalArgumentException.class, () -> service.apply(1L, requestWithResume()));
        verify(mapper, never()).insertApplication(any());
    }

    @Test void resumeIsRequired() {
        RecruitmentMapper mapper = mock(RecruitmentMapper.class);
        when(mapper.findPosting(1L)).thenReturn(Optional.of(posting("Y", LocalDate.now().plusDays(10))));
        when(mapper.findPostingDocumentTypes(1L)).thenReturn(java.util.List.of("RESUME"));
        RecruitmentService service = new RecruitmentService(mapper, storage.toString());
        JobApplicationRequest request = requestWithResume();
        request.setResume(null);

        ApplicationFileValidationException exception = assertThrows(
                ApplicationFileValidationException.class,
                () -> service.apply(1L, request));
        assertEquals("resume", exception.getFieldName());
        verify(mapper, never()).insertApplication(any());
    }

    @Test void invalidDocumentIdentifiesOnlyTheFileFieldThatMustBeReselected() {
        RecruitmentMapper mapper = mock(RecruitmentMapper.class);
        when(mapper.findPosting(1L)).thenReturn(Optional.of(posting("Y", LocalDate.now().plusDays(10))));
        when(mapper.findPostingDocumentTypes(1L))
                .thenReturn(java.util.List.of("RESUME", "DRIVER_LICENSE"));
        RecruitmentService service = new RecruitmentService(mapper, storage.toString());
        JobApplicationRequest request = requestWithResume();
        request.setDriverLicense(new MockMultipartFile(
                "driverLicense", "license.pdf", "application/pdf",
                "not-a-real-pdf".getBytes(StandardCharsets.US_ASCII)));

        ApplicationFileValidationException exception = assertThrows(
                ApplicationFileValidationException.class,
                () -> service.apply(1L, request));

        assertEquals("driverLicense", exception.getFieldName());
        verify(mapper, never()).insertApplication(any());
    }

    @Test void unconfiguredDocumentIsRejectedEvenIfClientSubmitsIt() {
        RecruitmentMapper mapper = mock(RecruitmentMapper.class);
        when(mapper.findPosting(1L)).thenReturn(Optional.of(posting("Y", LocalDate.now().plusDays(10))));
        when(mapper.findPostingDocumentTypes(1L)).thenReturn(java.util.List.of());
        RecruitmentService service = new RecruitmentService(mapper, storage.toString());

        ApplicationFileValidationException exception = assertThrows(
                ApplicationFileValidationException.class,
                () -> service.apply(1L, requestWithResume()));

        assertEquals("resume", exception.getFieldName());
        verify(mapper, never()).insertApplication(any());
    }

    @Test void archiveFilenameUsesApplicantNameAndApplicationDate() {
        RecruitmentMapper mapper = mock(RecruitmentMapper.class);
        JobApplication application = new JobApplication();
        application.setApplicationNo(12L);
        application.setApplicantName("홍 길동");
        application.setAppliedAt(LocalDateTime.of(2026, 11, 10, 15, 30));
        when(mapper.findApplication(12L)).thenReturn(Optional.of(application));
        RecruitmentService service = new RecruitmentService(mapper, storage.toString());

        assertEquals("홍_길동_20261110.zip", service.archiveFilename(12L));
    }

    @Test void updatePostingChangesEditableFields() {
        RecruitmentMapper mapper = mock(RecruitmentMapper.class);
        JobPosting posting = posting("Y", LocalDate.now().plusDays(10));
        posting.setTitle("기존 공고");
        posting.setContent("기존 내용");
        when(mapper.findPosting(1L)).thenReturn(Optional.of(posting));
        when(mapper.updatePosting(any(JobPosting.class))).thenReturn(1);
        RecruitmentService service = new RecruitmentService(mapper, storage.toString());

        JobPostingRequest request = new JobPostingRequest();
        request.setTitle("수정된 공고");
        request.setContent("수정된 내용");
        request.setDeadline(LocalDate.now().plusDays(20));
        request.setActive(false);

        service.updatePosting(1L, request);

        assertEquals("수정된 공고", posting.getTitle());
        assertEquals("수정된 내용", posting.getContent());
        assertEquals(request.getDeadline(), posting.getDeadline());
        assertEquals("N", posting.getAlwaysOpen());
        assertEquals("N", posting.getStatus());
        verify(mapper).updatePosting(posting);
        verify(mapper).insertPostingDocument(1L, "RESIDENT_REGISTRATION", 7);
        verify(mapper).insertPostingDocument(1L, "MEDICAL_CERTIFICATE", 8);
    }

    @Test void alwaysOpenPostingHasNoDeadlineAndRemainsOpen() {
        RecruitmentMapper mapper = mock(RecruitmentMapper.class);
        JobPosting posting = posting("Y", null);
        posting.setAlwaysOpen("Y");
        posting.setTitle("상시 모집 공고");
        posting.setContent("상시 모집 내용");
        when(mapper.findPosting(1L)).thenReturn(Optional.of(posting));
        when(mapper.findPostingDocumentTypes(1L)).thenReturn(java.util.List.of());
        RecruitmentService service = new RecruitmentService(mapper, storage.toString());

        var detail = service.getPostingDetail(1L);

        assertEquals("상시 모집", detail.deadline());
        assertTrue(detail.open());
    }

    @Test void updatingToAlwaysOpenClearsExistingDeadline() {
        RecruitmentMapper mapper = mock(RecruitmentMapper.class);
        JobPosting posting = posting("Y", LocalDate.now().plusDays(10));
        when(mapper.findPosting(1L)).thenReturn(Optional.of(posting));
        when(mapper.updatePosting(any(JobPosting.class))).thenReturn(1);
        RecruitmentService service = new RecruitmentService(mapper, storage.toString());

        JobPostingRequest request = new JobPostingRequest();
        request.setTitle("상시 모집 공고");
        request.setContent("상시 모집 내용");
        request.setAlwaysOpen(true);

        service.updatePosting(1L, request);

        assertNull(posting.getDeadline());
        assertEquals("Y", posting.getAlwaysOpen());
        verify(mapper).updatePosting(posting);
    }

    @Test void postingWithApplicationsCannotBeDeleted() {
        RecruitmentMapper mapper = mock(RecruitmentMapper.class);
        when(mapper.findPosting(1L)).thenReturn(Optional.of(posting("N", LocalDate.now().plusDays(10))));
        when(mapper.countApplications(1L)).thenReturn(1L);
        RecruitmentService service = new RecruitmentService(mapper, storage.toString());

        assertThrows(IllegalStateException.class, () -> service.deletePosting(1L));

        verify(mapper, never()).deletePosting(1L);
    }

    @Test void postingWithoutApplicationsCanBeDeleted() {
        RecruitmentMapper mapper = mock(RecruitmentMapper.class);
        when(mapper.findPosting(1L)).thenReturn(Optional.of(posting("N", LocalDate.now().plusDays(10))));
        when(mapper.countApplications(1L)).thenReturn(0L);
        when(mapper.deletePosting(1L)).thenReturn(1);
        RecruitmentService service = new RecruitmentService(mapper, storage.toString());

        service.deletePosting(1L);

        verify(mapper).deletePosting(1L);
    }

    private JobPosting posting(String status, LocalDate deadline) {
        JobPosting posting = new JobPosting();
        posting.setPostingNo(1L);
        posting.setStatus(status);
        posting.setDeadline(deadline);
        posting.setAlwaysOpen("N");
        return posting;
    }

    private JobApplicationRequest requestWithResume() {
        JobApplicationRequest request = new JobApplicationRequest();
        request.setApplicantName("지원자");
        request.setPhone("010-0000-0000");
        request.setApplicantType("NEW");
        request.setConsented(true);
        request.setResume(new MockMultipartFile("resume", "resume.pdf", "application/pdf",
                "%PDF-1.7\n%%EOF".getBytes(StandardCharsets.US_ASCII)));
        return request;
    }
}
