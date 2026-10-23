package com.transit.SGComplaint.repository;

import com.transit.SGComplaint.domain.Complaint;
import com.transit.SGComplaint.domain.ComplaintStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface ComplaintRepository {
    List<Complaint> selectMemberPage(@Param("empNo") Long empNo,
                                     @Param("startDateTime") LocalDateTime startDateTime,
                                     @Param("endDateTime") LocalDateTime endDateTime,
                                     @Param("limit") int limit,
                                     @Param("offset") long offset);
    long countMemberPage(@Param("empNo") Long empNo,
                         @Param("startDateTime") LocalDateTime startDateTime,
                         @Param("endDateTime") LocalDateTime endDateTime);
    List<Complaint> findAllByOrderByCreatedAtDesc();
    List<Complaint> findByStatusOrderByCreatedAtDesc(@Param("status") ComplaintStatus status);
    List<Complaint> findTop5ByOrderByCreatedAtDesc();
    Optional<Complaint> findById(@Param("complaintNo") Long complaintNo);
    long count();
    long countByStatus(@Param("status") ComplaintStatus status);
    int insertComplaint(Complaint complaint);
    int updateComplaint(Complaint complaint);
    int updateStatusWithVersion(@Param("complaintNo") Long complaintNo,
                                @Param("status") ComplaintStatus status,
                                @Param("expectedVersion") Long expectedVersion);
    int updateMemberContent(@Param("complaintNo") Long complaintNo,
                            @Param("empNo") Long empNo,
                            @Param("category") String category,
                            @Param("title") String title,
                            @Param("content") String content);
    int deleteMemberComplaint(@Param("complaintNo") Long complaintNo,
                              @Param("empNo") Long empNo);
    List<Complaint> selectAdminPage(@Param("status") ComplaintStatus status, @Param("limit") int limit, @Param("offset") long offset);
    long countAdminPage(@Param("status") ComplaintStatus status);
    List<Complaint> selectPublicPage(@Param("category") String category, @Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") long offset);
    long countPublicPage(@Param("category") String category, @Param("keyword") String keyword);
    int anonymizeExpiredCompleted(@Param("cutoff") LocalDateTime cutoff,
                                  @Param("limit") int limit);

    default Page<Complaint> findAllByOrderByCreatedAtDesc(Pageable pageable) {
        return new PageImpl<>(selectAdminPage(null, pageable.getPageSize(), pageable.getOffset()), pageable, countAdminPage(null));
    }
    default Page<Complaint> findByStatusOrderByCreatedAtDesc(ComplaintStatus status, Pageable pageable) {
        return new PageImpl<>(selectAdminPage(status, pageable.getPageSize(), pageable.getOffset()), pageable, countAdminPage(status));
    }
    default Page<Complaint> searchPublicComplaints(String category, String keyword, Pageable pageable) {
        return new PageImpl<>(selectPublicPage(category, keyword, pageable.getPageSize(), pageable.getOffset()), pageable, countPublicPage(category, keyword));
    }
    default Page<Complaint> findMemberComplaints(Long empNo, LocalDateTime startDateTime,
                                                 LocalDateTime endDateTime, Pageable pageable) {
        return new PageImpl<>(selectMemberPage(empNo, startDateTime, endDateTime,
                pageable.getPageSize(), pageable.getOffset()), pageable,
                countMemberPage(empNo, startDateTime, endDateTime));
    }
    default Complaint saveAndFlush(Complaint complaint) { insertComplaint(complaint); return complaint; }
    default Complaint save(Complaint complaint) { updateComplaint(complaint); return complaint; }
}
