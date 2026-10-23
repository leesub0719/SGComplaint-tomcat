package com.transit.SGComplaint.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AbuseLimitMapper {
    int deleteExpired();
    int ensureBucket(@Param("key") String key);
    int take(@Param("key") String key, @Param("seconds") int seconds, @Param("limit") int limit);
}
