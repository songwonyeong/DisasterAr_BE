package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.BeaconElementMapV4;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BeaconElementMapRepositoryV4 extends JpaRepository<BeaconElementMapV4, String> {

    Optional<BeaconElementMapV4> findByBeacon_Id(String beaconId);

    /**
     * 위치 판정/모니터링에서 사용할 active 매핑 조회.
     * Entity 필드명이 active라서 메서드명도 ActiveTrue로 간다.
     */
    Optional<BeaconElementMapV4> findByBeacon_IdAndActiveTrue(String beaconId);

    List<BeaconElementMapV4> findBySchool_IdAndFloorIndex(String schoolId, Integer floorIndex);

    /**
     * monitoring-map에서는 active 매핑만 내려준다.
     */
    List<BeaconElementMapV4> findBySchool_IdAndFloorIndexAndActiveTrue(
            String schoolId,
            Integer floorIndex
    );

    /**
     * 기존 호환용 element_id 기준 조회.
     */
    List<BeaconElementMapV4> findBySchool_IdAndFloorIndexAndElementId(
            String schoolId,
            Integer floorIndex,
            String elementId
    );

    /**
     * 신규 zone_element_id 기준 조회.
     */
    List<BeaconElementMapV4> findBySchool_IdAndFloorIndexAndZoneElementId(
            String schoolId,
            Integer floorIndex,
            String zoneElementId
    );
}