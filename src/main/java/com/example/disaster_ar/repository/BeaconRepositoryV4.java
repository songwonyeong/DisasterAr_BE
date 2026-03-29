package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.BeaconV4;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BeaconRepositoryV4 extends JpaRepository<BeaconV4, String> {
    List<BeaconV4> findBySchool_IdOrderByFloorIndexAscBeaconNoAsc(String schoolId);
}