package com.transit.SGComplaint.mapper;

import com.transit.SGComplaint.domain.ComplaintSmsNotification;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ComplaintSmsNotificationMapper {

    int insertIfAbsent(ComplaintSmsNotification notification);

    Optional<ComplaintSmsNotification> findById(
            @Param("notificationNo") Long notificationNo);

    List<Long> findRetryCandidates(@Param("limit") int limit);

    int claim(@Param("notificationNo") Long notificationNo);

    int markSent(@Param("notificationNo") Long notificationNo);

    int markFailed(
            @Param("notificationNo") Long notificationNo,
            @Param("lastError") String lastError);

    int recoverStaleSending(@Param("cutoff") LocalDateTime cutoff);
}
