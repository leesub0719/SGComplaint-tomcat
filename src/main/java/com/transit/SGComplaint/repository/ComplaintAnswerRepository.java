package com.transit.SGComplaint.repository;

import com.transit.SGComplaint.domain.ComplaintAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Mapper
public interface ComplaintAnswerRepository {
    Optional<ComplaintAnswer> findByComplaintNo(@Param("complaintNo") Long complaintNo);
    List<ComplaintAnswer> findByComplaintNoIn(@Param("complaintNumbers") Collection<Long> complaintNumbers);
    Optional<ComplaintAnswer> findById(@Param("answerNo") Long answerNo);
    int insertAnswer(ComplaintAnswer answer);
    int updateAnswer(ComplaintAnswer answer);
    default ComplaintAnswer saveAndFlush(ComplaintAnswer answer) {
        if (answer.getAnswerNo() == null) insertAnswer(answer); else updateAnswer(answer);
        return answer;
    }
}
