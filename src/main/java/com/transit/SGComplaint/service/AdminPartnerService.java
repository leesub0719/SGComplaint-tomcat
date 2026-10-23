package com.transit.SGComplaint.service;

import com.transit.SGComplaint.DTO.AdminPartnerItem;
import com.transit.SGComplaint.DTO.PartnerRequest;
import com.transit.SGComplaint.domain.Partner;
import com.transit.SGComplaint.mapper.PartnerMapper;
import com.transit.SGComplaint.repository.PartnerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class AdminPartnerService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final int PAGE_SIZE = 10;
    private static final Set<String> ALLOWED_SITE_SCHEMES = Set.of("http", "https");

    private final PartnerRepository partnerRepository;
    private final PartnerMapper partnerMapper;

    public AdminPartnerService(PartnerRepository partnerRepository,
                               PartnerMapper partnerMapper) {
        this.partnerRepository = partnerRepository;
        this.partnerMapper = partnerMapper;
    }

    public long countPartners() {
        return partnerMapper.countActive(null);
    }

    public Page<AdminPartnerItem> searchPartners(String keyword, int pageNumber) {
        String normalizedKeyword = StringUtils.hasText(keyword)
                ? keyword.trim().toLowerCase(Locale.ROOT)
                : null;
        PageRequest pageable = PageRequest.of(Math.max(pageNumber, 0), PAGE_SIZE);
        long total = partnerMapper.countActive(normalizedKeyword);
        List<AdminPartnerItem> content = partnerMapper.findActivePage(
                        normalizedKeyword, PAGE_SIZE, pageable.getOffset())
                .stream().map(this::toItem).toList();
        return new PageImpl<>(content, pageable, total);
    }

    @Transactional
    public Long createPartner(PartnerRequest request) {
        Partner partner = Partner.create(
                request.getName(),
                request.getPhone(),
                normalizeSite(request.getSite()),
                normalizeNotes(request.getNotes())
        );
        return partnerRepository.saveAndFlush(partner).getPartnerNo();
    }

    @Transactional
    public String updatePartner(Long partnerNo, PartnerRequest request) {
        Partner partner = getRequiredPartner(partnerNo);
        partner.changeInformation(
                request.getName(),
                request.getPhone(),
                normalizeSite(request.getSite()),
                normalizeNotes(request.getNotes())
        );
        partnerRepository.save(partner);
        return partner.getName() + " 협력업체 정보를 수정했습니다.";
    }

    @Transactional
    public String deletePartner(Long partnerNo) {
        Partner partner = getRequiredPartner(partnerNo);
        String name = partner.getName();
        partner.deactivate();
        partnerRepository.save(partner);
        return name + " 협력업체를 목록에서 제외했습니다.";
    }

    private Partner getRequiredPartner(Long partnerNo) {
        return partnerRepository.findById(partnerNo)
                .filter(partner -> "Y".equals(partner.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "협력업체 정보를 찾을 수 없습니다."
                ));
    }

    private String normalizeSite(String site) {
        if (!StringUtils.hasText(site)) return "";
        String normalized = site.trim();
        if (!normalized.matches("(?i)^https?://.*")) {
            normalized = "https://" + normalized;
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("사이트 주소에는 공백을 사용할 수 없습니다.");
        }
        try {
            URI uri = new URI(normalized);
            String scheme = uri.getScheme() == null
                    ? ""
                    : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!ALLOWED_SITE_SCHEMES.contains(scheme)
                    || !StringUtils.hasText(uri.getRawAuthority())) {
                throw new IllegalArgumentException(
                        "사이트 주소는 올바른 http 또는 https URL로 입력해 주세요."
                );
            }
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(
                    "사이트 주소 형식을 확인해 주세요.", exception
            );
        }
        return normalized;
    }

    private String normalizeNotes(String notes) {
        return StringUtils.hasText(notes) ? notes.trim() : "";
    }

    private AdminPartnerItem toItem(Partner partner) {
        return new AdminPartnerItem(
                partner.getPartnerNo(),
                partner.getName(),
                partner.getPhone(),
                partner.getSite(),
                partner.getNotes(),
                partner.getCreatedAt().format(DATE_FORMATTER)
        );
    }
}
