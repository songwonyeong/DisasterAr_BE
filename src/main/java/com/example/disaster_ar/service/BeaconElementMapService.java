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

    private static final int DEFAULT_THRESHOLD_RSSI = -85;

    private final BeaconElementMapRepositoryV4 beaconElementMapRepositoryV4;
    private final BeaconRepositoryV4 beaconRepositoryV4;
    private final ChannelElementTagRepositoryV4 channelElementTagRepositoryV4;
    private final SchoolRepository schoolRepository;

    @Transactional
    public BeaconElementMapResponse createMapping(BeaconElementMapCreateRequest req) {
        String schoolId = trimToNull(req.getSchoolId());
        String beaconId = trimToNull(req.getBeaconId());

        if (schoolId == null) {
            throw new IllegalArgumentException("schoolId가 비어 있습니다.");
        }
        if (req.getFloorIndex() == null) {
            throw new IllegalArgumentException("floorIndex가 비어 있습니다.");
        }
        if (beaconId == null) {
            throw new IllegalArgumentException("beaconId가 비어 있습니다.");
        }

        /*
         * 1차 핵심:
         * zoneElementId가 있으면 그걸 사용하고,
         * 없으면 기존 elementId를 zoneElementId로 사용한다.
         */
        String zoneElementId = trimToNull(req.getZoneElementId());
        if (zoneElementId == null) {
            zoneElementId = trimToNull(req.getElementId());
        }

        if (zoneElementId == null) {
            throw new IllegalArgumentException("zoneElementId 또는 elementId가 필요합니다.");
        }

        String beaconElementId = trimToNull(req.getBeaconElementId());

        SchoolV4 school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("학교가 존재하지 않습니다."));

        BeaconV4 beacon = beaconRepositoryV4.findById(beaconId)
                .orElseThrow(() -> new IllegalArgumentException("비콘이 존재하지 않습니다."));

        if (beacon.getSchool() == null || !beacon.getSchool().getId().equals(school.getId())) {
            throw new IllegalArgumentException("비콘이 해당 학교 소속이 아닙니다.");
        }

        if (!beacon.getFloorIndex().equals(req.getFloorIndex())) {
            throw new IllegalArgumentException("비콘 floorIndex와 요청 floorIndex가 일치하지 않습니다.");
        }

        /*
         * zoneElementId 검증.
         * SAFE_ZONE / DISASTER_ZONE / RESTRICTED_ZONE 같은 구역 element가
         * channel_element_tags에 존재하는지 확인한다.
         */
        ChannelElementTagV4 zoneElement = channelElementTagRepositoryV4
                .findBySchool_IdAndFloorIndexAndElementId(
                        schoolId,
                        req.getFloorIndex(),
                        zoneElementId
                )
                .orElseThrow(() -> new IllegalArgumentException("zoneElementId에 해당하는 element가 존재하지 않습니다."));

        /*
         * beaconElementId는 선택값.
         * 값이 있으면 channel_element_tags에 존재하는지만 검증한다.
         */
        if (beaconElementId != null) {
            channelElementTagRepositoryV4
                    .findBySchool_IdAndFloorIndexAndElementId(
                            schoolId,
                            req.getFloorIndex(),
                            beaconElementId
                    )
                    .orElseThrow(() -> new IllegalArgumentException("beaconElementId에 해당하는 element가 존재하지 않습니다."));
        }

        /*
         * 기존 정책 유지:
         * beacon_id unique이므로 같은 비콘 매핑이 이미 있으면 새로 만들지 않고 갱신한다.
         */
        BeaconElementMapV4 mapping = beaconElementMapRepositoryV4.findByBeacon_Id(beacon.getId())
                .orElseGet(() -> BeaconElementMapV4.builder()
                        .id(UUID.randomUUID().toString())
                        .createdAt(LocalDateTime.now())
                        .build()
                );

        LocalDateTime now = LocalDateTime.now();

        if (mapping.getCreatedAt() == null) {
            mapping.setCreatedAt(now);
        }

        mapping.setSchool(school);
        mapping.setFloorIndex(req.getFloorIndex());
        mapping.setBeacon(beacon);

        /*
         * 중요:
         * 기존 element_id는 호환용으로 남긴다.
         * 1차에서는 element_id = zone_element_id 로 맞춘다.
         */
        mapping.setElementId(zoneElementId);
        mapping.setZoneElementId(zoneElementId);

        /*
         * 비콘 마커 element ID.
         * 없으면 null로 둔다.
         */
        mapping.setBeaconElementId(beaconElementId);

        /*
         * placementName, zoneType은 요청값이 있으면 요청값 우선.
         * 없으면 channel_element_tags의 name, elementType을 사용한다.
         */
        mapping.setPlacementName(
                hasText(req.getPlacementName())
                        ? req.getPlacementName().trim()
                        : zoneElement.getName()
        );

        mapping.setZoneType(
                hasText(req.getZoneType())
                        ? req.getZoneType().trim()
                        : zoneElement.getElementType()
        );

        /*
         * thresholdRssi는 요청값이 있으면 요청값 사용.
         * 없으면 기존값 유지.
         * 기존값도 없으면 -85 사용.
         */
        Integer thresholdRssi = req.getThresholdRssi();
        if (thresholdRssi == null) {
            thresholdRssi = mapping.getThresholdRssi();
        }
        if (thresholdRssi == null) {
            thresholdRssi = DEFAULT_THRESHOLD_RSSI;
        }

        validateThresholdRssi(thresholdRssi);
        mapping.setThresholdRssi(thresholdRssi);

        /*
         * active도 요청값이 있으면 요청값 사용.
         * 없으면 기존값 유지.
         * 기존값도 없으면 true 사용.
         */
        Boolean active = req.getIsActive();
        if (active == null) {
            active = mapping.getActive();
        }
        if (active == null) {
            active = true;
        }

        mapping.setActive(active);
        mapping.setUpdatedAt(now);

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

        /*
         * 관리용 목록 조회는 inactive도 보여줘야 하므로
         * 여기서는 active 필터를 걸지 않는다.
         */
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
        String effectiveZoneElementId = mapping.getEffectiveZoneElementId();

        return BeaconElementMapResponse.builder()
                .id(mapping.getId())
                .schoolId(mapping.getSchool() != null ? mapping.getSchool().getId() : null)
                .floorIndex(mapping.getFloorIndex())
                .beaconId(mapping.getBeacon() != null ? mapping.getBeacon().getId() : null)
                .beaconName(mapping.getBeacon() != null ? mapping.getBeacon().getName() : null)

                .elementId(effectiveZoneElementId)
                .beaconElementId(mapping.getBeaconElementId())
                .zoneElementId(effectiveZoneElementId)
                .placementName(mapping.getPlacementName())
                .zoneType(mapping.getZoneType())
                .thresholdRssi(mapping.getEffectiveThresholdRssi())
                .isActive(mapping.isEffectivelyActive())

                .createdAt(mapping.getCreatedAt())
                .updatedAt(mapping.getUpdatedAt())
                .build();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static void validateThresholdRssi(Integer thresholdRssi) {
        if (thresholdRssi == null) {
            return;
        }

        /*
         * RSSI는 보통 음수다.
         * 너무 이상한 값이 들어오면 막는다.
         */
        if (thresholdRssi > 0 || thresholdRssi < -120) {
            throw new IllegalArgumentException("thresholdRssi는 -120 이상 0 이하 값이어야 합니다.");
        }
    }
}