package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.ChannelMapV4;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChannelMapRepositoryV4 extends JpaRepository<ChannelMapV4, String> {

    List<ChannelMapV4> findBySchool_IdOrderByFloorIndexAsc(String schoolId);

    Optional<ChannelMapV4> findByIdAndSchool_Id(String mapId, String schoolId);
}