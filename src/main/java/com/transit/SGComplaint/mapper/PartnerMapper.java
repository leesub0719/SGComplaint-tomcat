package com.transit.SGComplaint.mapper;

import com.transit.SGComplaint.domain.Partner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PartnerMapper {
    long countActive(@Param("keyword") String keyword);
    List<Partner> findActivePage(
            @Param("keyword") String keyword,
            @Param("limit") int limit,
            @Param("offset") long offset);
}
