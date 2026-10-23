package com.transit.SGComplaint.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface WithdrawalHistoryMapper {
    int insert(@Param("empNo") Long empNo,
               @Param("originalEmpId") String originalEmpId,
               @Param("replacementEmpId") String replacementEmpId,
               @Param("changeReason") String changeReason,
               @Param("changedBy") String changedBy,
               @Param("purgeAfter") LocalDateTime purgeAfter);

    int deleteExpired(@Param("cutoff") LocalDateTime cutoff,
                      @Param("limit") int limit);
}
