package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.ChannelElementTagV4;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChannelElementTagRepositoryV4 extends JpaRepository<ChannelElementTagV4, String> {
    List<ChannelElementTagV4> findBySchool_IdAndFloorIndex(String schoolId, Integer floorIndex);
    Optional<ChannelElementTagV4> findBySchool_IdAndFloorIndexAndElementId(String schoolId, Integer floorIndex, String elementId);
}