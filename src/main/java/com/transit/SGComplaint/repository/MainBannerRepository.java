package com.transit.SGComplaint.repository;

import com.transit.SGComplaint.domain.MainBanner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Optional;

@Mapper
public interface MainBannerRepository {
    Optional<MainBanner> findById(@Param("bannerNo") Long bannerNo);
    int insertBanner(MainBanner banner);
    int deleteById(@Param("bannerNo") Long bannerNo);
    default MainBanner save(MainBanner banner) { insertBanner(banner); return banner; }
    default void delete(MainBanner banner) { deleteById(banner.getBannerNo()); }
    default void flush() { }
}
