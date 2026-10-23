package com.transit.SGComplaint.repository;

import com.transit.SGComplaint.domain.ComplaintAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Mapper
public interface ComplaintAttachmentRepository {
    List<ComplaintAttachment> findByComplaintNoInOrderByAttachmentNoAsc(@Param("complaintNumbers") List<Long> complaintNumbers);
    Optional<ComplaintAttachment> findById(@Param("attachmentNo") Long attachmentNo);
    List<ComplaintAttachment> findExpiredCompleted(@Param("cutoff") LocalDateTime cutoff,
                                                    @Param("limit") int limit);
    int deleteById(@Param("attachmentNo") Long attachmentNo);
    int insertAttachment(ComplaintAttachment attachment);
    default ComplaintAttachment save(ComplaintAttachment attachment) { insertAttachment(attachment); return attachment; }
    default void flush() { }
}
