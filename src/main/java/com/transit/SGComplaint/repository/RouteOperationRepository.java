package com.transit.SGComplaint.repository;

import com.transit.SGComplaint.domain.RouteOperation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Optional;

@Mapper
public interface RouteOperationRepository {
    Optional<RouteOperation> findById(@Param("routeNo") Long routeNo);
    int insertRoute(RouteOperation route);
    int updateRoute(RouteOperation route);
    default RouteOperation saveAndFlush(RouteOperation route) { insertRoute(route); return route; }
    default RouteOperation save(RouteOperation route) { updateRoute(route); return route; }
}
