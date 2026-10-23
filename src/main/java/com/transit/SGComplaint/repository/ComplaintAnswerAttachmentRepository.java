package com.transit.SGComplaint.repository;

import com.transit.SGComplaint.domain.ComplaintAnswerAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Mapper
public interface ComplaintAnswerAttachmentRepository {
    List<ComplaintAnswerAttachment> findByAnswerNoInOrderByAnswerAttachmentNoAsc(@Param("answerNumbers") List<Long> answerNumbers);
    Optional<ComplaintAnswerAttachment> findById(@Param("attachmentNo") Long attachmentNo);
    long countByAnswerNo(@Param("answerNo") Long answerNo);
    List<ComplaintAnswerAttachment> findExpiredCompleted(@Param("cutoff") LocalDateTime cutoff,
                                                          @Param("limit") int limit);
    int deleteById(@Param("attachmentNo") Long attachmentNo);
    int insertAttachment(ComplaintAnswerAttachment attachment);
    default ComplaintAnswerAttachment save(ComplaintAnswerAttachment attachment) { insertAttachment(attachment); return attachment; }
    default void flush() { }
}
