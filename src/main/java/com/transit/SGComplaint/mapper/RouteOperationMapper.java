package com.transit.SGComplaint.mapper;

import com.transit.SGComplaint.domain.RouteOperation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RouteOperationMapper {
    List<RouteOperation> findActiveByType(@Param("routeType") String routeType);
    long countActiveByType(@Param("routeType") String routeType);
}
