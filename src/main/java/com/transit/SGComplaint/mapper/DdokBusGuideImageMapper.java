package com.transit.SGComplaint.mapper;

import com.transit.SGComplaint.domain.DdokBusGuideImage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DdokBusGuideImageMapper {
    List<DdokBusGuideImage> findAllOrdered();
    long countAll();
}
