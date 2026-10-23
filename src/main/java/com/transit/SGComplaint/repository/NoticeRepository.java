package com.transit.SGComplaint.repository;

import com.transit.SGComplaint.domain.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Mapper
public interface NoticeRepository {
    Optional<Notice> findById(@Param("noticeNo") Long noticeNo);
    List<Notice> findTop3ByOrderByPinnedDescCreatedAtDesc();
    List<Notice> findByPopupOrderByCreatedAtDesc(@Param("popup") String popup);
    List<Notice> selectPage(@Param("keyword") String keyword, @Param("adminOrder") boolean adminOrder, @Param("limit") int limit, @Param("offset") long offset);
    long countPage(@Param("keyword") String keyword);
    int insertNotice(Notice notice);
    int updateNotice(Notice notice);
    int deleteById(@Param("noticeNo") Long noticeNo);

    default Page<Notice> searchNotices(String keyword, Pageable pageable) {
        return new PageImpl<>(selectPage(keyword, false, pageable.getPageSize(), pageable.getOffset()), pageable, countPage(keyword));
    }
    default Page<Notice> findAllByOrderByCreatedAtDesc(Pageable pageable) {
        return new PageImpl<>(selectPage("", true, pageable.getPageSize(), pageable.getOffset()), pageable, countPage(""));
    }
    default Notice saveAndFlush(Notice notice) {
        if (notice.getNoticeNo() == null) insertNotice(notice); else updateNotice(notice);
        return notice;
    }
}
