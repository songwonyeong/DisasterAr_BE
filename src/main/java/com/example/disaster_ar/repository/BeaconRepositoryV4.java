package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.BeaconV4;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BeaconRepositoryV4 extends JpaRepository<BeaconV4, String> {

    List<BeaconV4> findBySchool_IdOrderByFloorIndexAscBeaconNoAsc(String schoolId);

    List<BeaconV4> findBySchool_IdAndFloorIndexOrderByBeaconNoAsc(
            String schoolId,
            Integer floorIndex
    );

    /**
     * 1차 반영 기준:
     * uuid + major + minor만으로 찾지 말고,
     * 반드시 학교 기준까지 포함해서 찾는다.
     */
    Optional<BeaconV4> findBySchool_IdAndUuidAndMajorAndMinor(
            String schoolId,
            String uuid,
            Integer major,
            Integer minor
    );

    boolean existsBySchool_IdAndUuidAndMajorAndMinor(
            String schoolId,
            String uuid,
            Integer major,
            Integer minor
    );

    boolean existsBySchool_IdAndFloorIndexAndBeaconNo(
            String schoolId,
            Integer floorIndex,
            Integer beaconNo
    );

    Optional<BeaconV4> findBySchool_IdAndFloorIndexAndBeaconNo(
            String schoolId,
            Integer floorIndex,
            Integer beaconNo
    );
}