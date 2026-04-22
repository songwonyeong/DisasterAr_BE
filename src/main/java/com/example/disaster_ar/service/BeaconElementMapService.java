package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.*;
import com.example.disaster_ar.dto.beacon.BeaconElementMapCreateRequest;
import com.example.disaster_ar.dto.beacon.BeaconElementMapResponse;
import com.example.disaster_ar.repository.BeaconElementMapRepositoryV4;
import com.example.disaster_ar.repository.BeaconRepositoryV4;
import com.example.disaster_ar.repository.ChannelElementTagRepositoryV4;
import com.example.disaster_ar.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BeaconElementMapService {

    private final BeaconElementMapRepositoryV4 beaconElementMapRepositoryV4;
    private final BeaconRepositoryV4 beaconRepositoryV4;
    private final ChannelElementTagRepositoryV4 channelElementTagRepositoryV4;
    private final SchoolRepository schoolRepository;

    @Transactional
    public BeaconElementMapResponse createMapping(BeaconElementMapCreateRequest req) {
        if (req.getSchoolId() == null || req.getSchoolId().isBlank()) {
            throw new IllegalArgumentException("schoolId가 비어 있습니다.");
        }
        if (req.getFloorIndex() == null) {
            throw new IllegalArgumentException("floorIndex가 비어 있습니다.");
        }
        if (req.getBeaconId() == null || req.getBeaconId().isBlank()) {
            throw new IllegalArgumentException("beaconId가 비어 있습니다.");
        }
        if (req.getElementId() == null || req.getElementId().isBlank()) {
            throw new IllegalArgumentException("elementId가 비어 있습니다.");
        }

        SchoolV4 school = schoolRepository.findById(req.getSchoolId())
                .orElseThrow(() -> new IllegalArgumentException("학교가 존재하지 않습니다."));

        BeaconV4 beacon = beaconRepositoryV4.findById(req.getBeaconId())
                .orElseThrow(() -> new IllegalArgumentException("비콘이 존재하지 않습니다."));

        if (beacon.getSchool() == null || !beacon.getSchool().getId().equals(school.getId())) {
            throw new IllegalArgumentException("비콘이 해당 학교 소속이 아닙니다.");
        }

        if (!beacon.getFloorIndex().equals(req.getFloorIndex())) {
            throw new IllegalArgumentException("비콘 floorIndex와 요청 floorIndex가 일치하지 않습니다.");
        }

        ChannelElementTagV4 element = channelElementTagRepositoryV4
                .findBySchool_IdAndFloorIndexAndElementId(
                        req.getSchoolId(),
                        req.getFloorIndex(),
                        req.getElementId()
                )
                .orElseThrow(() -> new IllegalArgumentException("해당 층에 element가 존재하지 않습니다."));

        // beacon_id unique라서 이미 있으면 갱신 정책으로 가도 되고, 막아도 됨
        BeaconElementMapV4 mapping = beaconElementMapRepositoryV4.findByBeacon_Id(beacon.getId())
                .orElse(
                        BeaconElementMapV4.builder()
                                .id(UUID.randomUUID().toString())
                                .createdAt(LocalDateTime.now())
                                .build()
                );

        mapping.setSchool(school);
        mapping.setFloorIndex(req.getFloorIndex());
        mapping.setBeacon(beacon);
        mapping.setElementId(element.getElementId());
        mapping.setUpdatedAt(LocalDateTime.now());

        BeaconElementMapV4 saved = beaconElementMapRepositoryV4.save(mapping);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BeaconElementMapResponse> getMappings(String schoolId, Integer floorIndex) {
        if (schoolId == null || schoolId.isBlank()) {
            throw new IllegalArgumentException("schoolId가 비어 있습니다.");
        }
        if (floorIndex == null) {
            throw new IllegalArgumentException("floorIndex가 비어 있습니다.");
        }

        return beaconElementMapRepositoryV4.findBySchool_IdAndFloorIndex(schoolId, floorIndex)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteMapping(String mappingId) {
        BeaconElementMapV4 mapping = beaconElementMapRepositoryV4.findById(mappingId)
                .orElseThrow(() -> new IllegalArgumentException("매핑이 존재하지 않습니다."));
        beaconElementMapRepositoryV4.delete(mapping);
    }

    private BeaconElementMapResponse toResponse(BeaconElementMapV4 mapping) {
        return BeaconElementMapResponse.builder()
                .id(mapping.getId())
                .schoolId(mapping.getSchool() != null ? mapping.getSchool().getId() : null)
                .floorIndex(mapping.getFloorIndex())
                .beaconId(mapping.getBeacon() != null ? mapping.getBeacon().getId() : null)
                .beaconName(mapping.getBeacon() != null ? mapping.getBeacon().getName() : null)
                .elementId(mapping.getElementId())
                .createdAt(mapping.getCreatedAt())
                .updatedAt(mapping.getUpdatedAt())
                .build();
    }
}