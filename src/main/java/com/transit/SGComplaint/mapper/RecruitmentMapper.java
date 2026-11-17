package com.transit.SGComplaint.mapper;

import com.transit.SGComplaint.domain.JobApplication;
import com.transit.SGComplaint.domain.JobApplicationFile;
import com.transit.SGComplaint.domain.JobPosting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface RecruitmentMapper {
    long countPostings(@Param("keyword") String keyword);
    List<JobPosting> findPostingPage(@Param("keyword") String keyword,
                                     @Param("limit") int limit,
                                     @Param("offset") long offset);
    Optional<JobPosting> findPosting(@Param("postingNo") Long postingNo);
    int insertPosting(JobPosting posting);
    int updatePosting(JobPosting posting);
    int updatePostingStatus(@Param("postingNo") Long postingNo,
                            @Param("status") String status);
    int deletePosting(@Param("postingNo") Long postingNo);
    int insertPostingDocument(@Param("postingNo") Long postingNo,
                              @Param("documentType") String documentType,
                              @Param("displayOrder") int displayOrder);
    int deletePostingDocuments(@Param("postingNo") Long postingNo);
    List<String> findPostingDocumentTypes(@Param("postingNo") Long postingNo);
    int insertApplication(JobApplication application);
    long countApplications(@Param("postingNo") Long postingNo);
    List<JobApplication> findApplications(@Param("postingNo") Long postingNo);
    Optional<JobApplication> findApplication(@Param("applicationNo") Long applicationNo);
    int insertApplicationFile(JobApplicationFile file);
    List<JobApplicationFile> findApplicationFiles(@Param("applicationNo") Long applicationNo);
}
