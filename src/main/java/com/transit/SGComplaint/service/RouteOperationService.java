package com.transit.SGComplaint.service;

import com.transit.SGComplaint.DTO.RouteOperationItem;
import com.transit.SGComplaint.DTO.RouteOperationRequest;
import com.transit.SGComplaint.domain.RouteOperation;
import com.transit.SGComplaint.domain.Employee;
import com.transit.SGComplaint.mapper.EmployeeMapper;
import com.transit.SGComplaint.mapper.RouteOperationMapper;
import com.transit.SGComplaint.repository.RouteOperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class RouteOperationService {

    public static final String VILLAGE = "VILLAGE";
    public static final String DDOK = "DDOK";

    private static final Set<String> ROUTE_TYPES = Set.of(VILLAGE, DDOK);
    private static final Set<String> ALLOWED_URL_SCHEMES = Set.of("http", "https");

    private final RouteOperationRepository routeOperationRepository;
    private final RouteOperationMapper routeOperationMapper;
    private final EmployeeMapper employeeMapper;
    private final AdminDeletionAuditService deletionAuditService;

    public RouteOperationService(RouteOperationRepository routeOperationRepository,
                                 RouteOperationMapper routeOperationMapper,
                                 EmployeeMapper employeeMapper,
                                 AdminDeletionAuditService deletionAuditService) {
        this.routeOperationRepository = routeOperationRepository;
        this.routeOperationMapper = routeOperationMapper;
        this.employeeMapper = employeeMapper;
        this.deletionAuditService = deletionAuditService;
    }

    public List<RouteOperationItem> getPublicRoutes(String routeType) {
        String normalizedType = normalizeRouteType(routeType);
        return routeOperationMapper.findActiveByType(normalizedType)
                .stream().map(this::toItem).toList();
    }

    public List<RouteOperationItem> getAdminRoutes() {
        return routeOperationMapper.findActiveByType(VILLAGE)
                .stream().map(this::toItem).toList();
    }

    public long countRoutes() {
        return routeOperationMapper.countActiveByType(VILLAGE);
    }

    @Transactional
    public Long createRoute(RouteOperationRequest request) {
        validateVillageRequest(request);
        RouteOperation route = RouteOperation.create(
                normalizeRouteType(request.getRouteType()),
                request.getBusName(),
                request.getTerminalInfo(),
                request.getDispatchInterval(),
                request.getInquiryPhone(),
                normalizeRouteUrl(request.getRouteUrl()));
        return routeOperationRepository.saveAndFlush(route).getRouteNo();
    }

    @Transactional
    public String updateRoute(Long routeNo, RouteOperationRequest request) {
        validateVillageRequest(request);
        RouteOperation route = getRequiredRoute(routeNo);
        route.changeInformation(
                normalizeRouteType(request.getRouteType()),
                request.getBusName(),
                request.getTerminalInfo(),
                request.getDispatchInterval(),
                request.getInquiryPhone(),
                normalizeRouteUrl(request.getRouteUrl()));
        routeOperationRepository.save(route);
        return route.getBusName() + " 운행안내를 수정했습니다.";
    }

    @Transactional
    public String deleteRoute(String administratorId, Long routeNo) {
        Employee administrator = employeeMapper.findByEmpIdAndEmpStatus(administratorId, "Y")
                .filter(Employee::hasAdminRole)
                .orElseThrow(() -> new IllegalStateException("관리자 권한을 확인할 수 없습니다."));
        RouteOperation route = getRequiredRoute(routeNo);
        String busName = route.getBusName();
        deletionAuditService.record(administrator, "ROUTE", routeNo, busName, Map.of(
                "routeType", route.getRouteType(),
                "busName", route.getBusName(),
                "terminalInfo", route.getTerminalInfo(),
                "dispatchInterval", route.getDispatchInterval(),
                "inquiryPhone", route.getInquiryPhone(),
                "routeUrl", route.getRouteUrl()));
        route.deactivate();
        routeOperationRepository.save(route);
        return busName + " 운행안내를 삭제했습니다.";
    }

    private RouteOperation getRequiredRoute(Long routeNo) {
        return routeOperationRepository.findById(routeNo)
                .filter(route -> "Y".equals(route.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "운행안내 정보를 찾을 수 없습니다."));
    }

    private void validateVillageRequest(RouteOperationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("운행안내 정보를 입력해 주세요.");
        }
        String routeType = normalizeRouteType(request.getRouteType());
        if (!VILLAGE.equals(routeType)) {
            throw new IllegalArgumentException(
                    "똑버스는 노선 정보 대신 안내 이미지를 등록해 주세요.");
        }
        requireText(request.getBusName(), "버스 이름", 100);
        requireText(request.getTerminalInfo(), "기점-종점", 200);
        requireText(request.getDispatchInterval(), "배차간격", 200);
        requireText(request.getInquiryPhone(), "문의전화", 100);
        requireText(request.getRouteUrl(), "노선안내 링크", 500);
    }

    private void requireText(String value, String label, int maxLength) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(label + "을(를) 입력해 주세요.");
        }
        if (value.trim().length() > maxLength) {
            throw new IllegalArgumentException(
                    label + "은(는) " + maxLength + "자 이내로 입력해 주세요.");
        }
    }

    private String normalizeRouteType(String routeType) {
        String normalized = StringUtils.hasText(routeType)
                ? routeType.trim().toUpperCase(Locale.ROOT) : "";
        if (!ROUTE_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("올바른 운행 유형을 선택해 주세요.");
        }
        return normalized;
    }

    private String normalizeRouteUrl(String routeUrl) {
        if (!StringUtils.hasText(routeUrl)) {
            throw new IllegalArgumentException("노선안내 링크를 입력해 주세요.");
        }
        String normalized = routeUrl.trim();
        if (!normalized.matches("(?i)^https?://.*")) {
            normalized = "https://" + normalized;
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("노선안내 링크에는 공백을 사용할 수 없습니다.");
        }
        try {
            URI uri = new URI(normalized);
            String scheme = uri.getScheme() == null
                    ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!ALLOWED_URL_SCHEMES.contains(scheme)
                    || !StringUtils.hasText(uri.getRawAuthority())) {
                throw new IllegalArgumentException(
                        "노선안내 링크는 올바른 http 또는 https 주소로 입력해 주세요.");
            }
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(
                    "노선안내 링크 형식을 확인해 주세요.", exception);
        }
        return normalized;
    }

    private RouteOperationItem toItem(RouteOperation route) {
        return new RouteOperationItem(
                route.getRouteNo(),
                route.getRouteType(),
                DDOK.equals(route.getRouteType()) ? "똑버스" : "마을버스",
                route.getBusName(),
                route.getTerminalInfo(),
                route.getDispatchInterval(),
                route.getInquiryPhone(),
                route.getRouteUrl());
    }
}
