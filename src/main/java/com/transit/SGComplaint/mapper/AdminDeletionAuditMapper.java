package com.transit.SGComplaint.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminDeletionAuditMapper {
    int insert(@Param("adminEmpNo") Long adminEmpNo,
               @Param("adminEmpId") String adminEmpId,
               @Param("targetType") String targetType,
               @Param("targetNo") Long targetNo,
               @Param("targetSummary") String targetSummary,
               @Param("snapshotJson") String snapshotJson);
}
