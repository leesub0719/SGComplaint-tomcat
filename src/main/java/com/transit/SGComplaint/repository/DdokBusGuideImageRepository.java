package com.transit.SGComplaint.repository;

import com.transit.SGComplaint.domain.DdokBusGuideImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Optional;

@Mapper
public interface DdokBusGuideImageRepository {
    Optional<DdokBusGuideImage> findById(@Param("imageNo") Long imageNo);
    int insertImage(DdokBusGuideImage image);
    int deleteById(@Param("imageNo") Long imageNo);
    default DdokBusGuideImage saveAndFlush(DdokBusGuideImage image) { insertImage(image); return image; }
    default void delete(DdokBusGuideImage image) { deleteById(image.getImageNo()); }
    default void flush() { }
}
