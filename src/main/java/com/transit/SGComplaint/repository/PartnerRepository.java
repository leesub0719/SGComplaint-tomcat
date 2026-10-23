package com.transit.SGComplaint.repository;

import com.transit.SGComplaint.domain.Partner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Optional;

@Mapper
public interface PartnerRepository {
    Optional<Partner> findById(@Param("partnerNo") Long partnerNo);
    int insertPartner(Partner partner);
    int updatePartner(Partner partner);
    default Partner saveAndFlush(Partner partner) { insertPartner(partner); return partner; }
    default Partner save(Partner partner) { updatePartner(partner); return partner; }
}
