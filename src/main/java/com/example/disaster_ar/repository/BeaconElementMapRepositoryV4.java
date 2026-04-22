package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.BeaconElementMapV4;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BeaconElementMapRepositoryV4 extends JpaRepository<BeaconElementMapV4, String> {

    Optional<BeaconElementMapV4> findByBeacon_Id(String beaconId);

    List<BeaconElementMapV4> findBySchool_IdAndFloorIndex(String schoolId, Integer floorIndex);

    List<BeaconElementMapV4> findBySchool_IdAndFloorIndexAndElementId(
            String schoolId,
            Integer floorIndex,
            String elementId
    );
}