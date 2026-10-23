package com.transit.SGComplaint.repository;

import com.transit.SGComplaint.domain.NoticeImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Optional;
import java.util.List;

@Mapper
public interface NoticeImageRepository {
    Optional<NoticeImage> findById(@Param("noticeImageNo") Long noticeImageNo);
    List<NoticeImage> findByNoticeNo(@Param("noticeNo") Long noticeNo);
    int insertImage(NoticeImage image);
    int deleteById(@Param("noticeImageNo") Long noticeImageNo);
    int deleteByNoticeNo(@Param("noticeNo") Long noticeNo);
    default NoticeImage saveAndFlush(NoticeImage image) { insertImage(image); return image; }
}
