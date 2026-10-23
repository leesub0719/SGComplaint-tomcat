package com.transit.SGComplaint.mapper;

import com.transit.SGComplaint.domain.MainBanner;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MainBannerMapper {
    List<MainBanner> findAllOrdered();
    long countAll();
    Integer findMaximumDisplayOrder();
}
