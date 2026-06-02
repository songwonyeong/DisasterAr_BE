package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.BeaconV4;
import com.example.disaster_ar.domain.v4.SchoolV4;
import com.example.disaster_ar.dto.beacon.BeaconRequest;
import com.example.disaster_ar.dto.beacon.BeaconResponse;
import com.example.disaster_ar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import com.example.disaster_ar.exception.ApiException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BeaconService {

    private final BeaconRepositoryV4 beaconRepositoryV4;
    private final SchoolRepository schoolRepository;
    private final BeaconElementMapRepositoryV4 beaconElementMapRepositoryV4;
    private final StudentRepositoryV4 studentRepositoryV4;
    private final StudentBeaconEventRepositoryV4 studentBeaconEventRepositoryV4;
    private final ScenarioAssignmentRepositoryV4 scenarioAssignmentRepositoryV4;
    private final ScenarioActionEventRepositoryV4 scenarioActionEventRepositoryV4;

    @Transactional
    public BeaconResponse createBeacon(BeaconRequest req) {
        validateCreateBeaconRequest(req);
        SchoolV4 school = schoolRepository.findById(req.getSchoolId())
                .orElseThrow(() -> ApiException.badRequest(
                        "INVALID_BEACON_REQUEST",
                        "schoolId에 해당하는 학교가 존재하지 않습니다."
                ));

        String uuid = req.getUuid().trim();

        if (beaconRepositoryV4.existsBySchool_IdAndUuidAndMajorAndMinor(
                school.getId(),
                uuid,
                req.getMajor(),
                req.getMinor()
        )) {
            throw ApiException.conflict(
                    "DUPLICATE_BEACON",
                    "이미 등록된 비콘입니다."
            );
        }

        if (req.getBeaconNo() != null
                && beaconRepositoryV4.existsBySchool_IdAndFloorIndexAndBeaconNo(
                        school.getId(),
                        req.getFloorIndex(),
                        req.getBeaconNo()
        )) {
            throw ApiException.conflict(
                    "DUPLICATE_BEACON",
                    "같은 학교/층에 동일한 beaconNo가 이미 등록되어 있습니다."
            );
        }

        BeaconV4 beacon = new BeaconV4();
        beacon.setId(UUID.randomUUID().toString());
        beacon.setSchool(school);
        beacon.setFloorIndex(req.getFloorIndex());

        beacon.setUuid(uuid);

        beacon.setMajor(req.getMajor());
        beacon.setMinor(req.getMinor());
        beacon.setBeaconNo(req.getBeaconNo());
        beacon.setMac(trimToNull(req.getMac()));
        beacon.setX(req.getX());
        beacon.setY(req.getY());
        beacon.setRealXM(req.getRealXM());
        beacon.setRealYM(req.getRealYM());
        beacon.setRealZM(req.getRealZM());
        beacon.setName(trimToNull(req.getName()));
        beacon.setTxPower(req.getTxPower());
        beacon.setCreatedAt(LocalDateTime.now());
        beacon.setUpdatedAt(LocalDateTime.now());

        BeaconV4 saved = beaconRepositoryV4.save(beacon);
        return toDto(saved);
    }

    public List<BeaconResponse> getBeacons(String schoolId) {
        schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("학교가 존재하지 않습니다."));

        return beaconRepositoryV4.findBySchool_IdOrderByFloorIndexAscBeaconNoAsc(schoolId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private void validateCreateBeaconRequest(BeaconRequest req) {
        if (req == null) {
            throw ApiException.badRequest(
                    "INVALID_BEACON_REQUEST",
                    "비콘 등록 요청값을 확인해 주세요."
            );
        }

        if (req.getSchoolId() == null || req.getSchoolId().isBlank()) {
            throw ApiException.badRequest(
                    "INVALID_BEACON_REQUEST",
                    "schoolId는 필수입니다."
            );
        }

        if (req.getFloorIndex() == null || req.getFloorIndex() < 0) {
            throw ApiException.badRequest(
                    "INVALID_BEACON_REQUEST",
                    "floorIndex는 0 이상이어야 합니다."
            );
        }

        if (req.getUuid() == null || req.getUuid().isBlank()) {
            throw ApiException.badRequest(
                    "INVALID_BEACON_REQUEST",
                    "uuid는 필수입니다."
            );
        }

        if (req.getMajor() == null || req.getMinor() == null) {
            throw ApiException.badRequest(
                    "INVALID_BEACON_REQUEST",
                    "major/minor는 필수입니다."
            );
        }

        if (req.getX() == null || req.getY() == null) {
            throw ApiException.badRequest(
                    "INVALID_BEACON_REQUEST",
                    "비콘의 x/y 좌표는 필수입니다."
            );
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    public BeaconResponse updateBeacon(String beaconId, BeaconRequest req) {
        BeaconV4 beacon = beaconRepositoryV4.findById(beaconId)
                .orElseThrow(() -> new IllegalArgumentException("비콘이 존재하지 않습니다."));

        SchoolV4 school = beacon.getSchool();

        String nextUuid = req.getUuid() != null ? req.getUuid().trim() : beacon.getUuid();
        Integer nextMajor = req.getMajor() != null ? req.getMajor() : beacon.getMajor();
        Integer nextMinor = req.getMinor() != null ? req.getMinor() : beacon.getMinor();
        Integer nextFloorIndex = req.getFloorIndex() != null ? req.getFloorIndex() : beacon.getFloorIndex();
        Integer nextBeaconNo = req.getBeaconNo() != null ? req.getBeaconNo() : beacon.getBeaconNo();

        if (nextFloorIndex == null || nextFloorIndex < 0) {
            throw ApiException.badRequest("INVALID_BEACON_REQUEST", "floorIndex는 0 이상이어야 합니다.");
        }
        if (nextUuid == null || nextUuid.isBlank() || nextMajor == null || nextMinor == null) {
            throw ApiException.badRequest("INVALID_BEACON_REQUEST", "uuid/major/minor는 필수입니다.");
        }

        beaconRepositoryV4
                .findBySchool_IdAndUuidAndMajorAndMinor(school.getId(), nextUuid, nextMajor, nextMinor)
                .filter(existing -> !existing.getId().equals(beaconId))
                .ifPresent(existing -> {
                    throw ApiException.conflict("DUPLICATE_BEACON", "이미 등록된 비콘입니다.");
                });

        if (nextBeaconNo != null) {
            beaconRepositoryV4
                    .findBySchool_IdAndFloorIndexAndBeaconNo(school.getId(), nextFloorIndex, nextBeaconNo)
                    .filter(existing -> !existing.getId().equals(beaconId))
                    .ifPresent(existing -> {
                        throw ApiException.conflict(
                                "DUPLICATE_BEACON",
                                "같은 학교/층에 동일한 beaconNo가 이미 등록되어 있습니다."
                        );
                    });
        }

        if (req.getFloorIndex() != null) beacon.setFloorIndex(req.getFloorIndex());
        if (req.getUuid() != null) beacon.setUuid(nextUuid);
        if (req.getMac() != null) beacon.setMac(trimToNull(req.getMac()));
        if (req.getName() != null) beacon.setName(trimToNull(req.getName()));
        if (req.getMajor() != null) beacon.setMajor(req.getMajor());
        if (req.getMinor() != null) beacon.setMinor(req.getMinor());
        if (req.getBeaconNo() != null) beacon.setBeaconNo(req.getBeaconNo());
        if (req.getX() != null) beacon.setX(req.getX());
        if (req.getY() != null) beacon.setY(req.getY());
        if (req.getRealXM() != null) beacon.setRealXM(req.getRealXM());
        if (req.getRealYM() != null) beacon.setRealYM(req.getRealYM());
        if (req.getRealZM() != null) beacon.setRealZM(req.getRealZM());
        if (req.getTxPower() != null) beacon.setTxPower(req.getTxPower());

        beacon.setUpdatedAt(LocalDateTime.now());

        BeaconV4 saved = beaconRepositoryV4.save(beacon);
        return toDto(saved);
    }

    @Transactional
    public void deleteBeacon(String beaconId) {
        BeaconV4 beacon = beaconRepositoryV4.findById(beaconId)
                .orElseThrow(() -> new IllegalArgumentException("비콘이 존재하지 않습니다."));

        boolean hasLocationHistory = studentBeaconEventRepositoryV4.existsByFromBeacon_IdOrToBeacon_Id(
                beaconId,
                beaconId
        );

        boolean usedByAssignment = scenarioAssignmentRepositoryV4.existsByBeacon_Id(beaconId);
        boolean usedByActionEvent = scenarioActionEventRepositoryV4.existsByBeacon_Id(beaconId);

        if (hasLocationHistory || usedByAssignment || usedByActionEvent) {
            throw ApiException.conflict(
                    "BEACON_IN_USE",
                    "훈련 이력 또는 미션에서 사용 중인 비콘입니다. 삭제할 수 없습니다."
            );
        }

        try {
            /*
             * 1. 구조도 매핑 참조 제거
             */
            beaconElementMapRepositoryV4.deleteByBeacon_Id(beaconId);
            beaconElementMapRepositoryV4.flush();

            /*
             * 2. 학생 현재 위치 참조 제거
             */
            studentRepositoryV4.clearLastBeaconByBeaconId(beaconId);
            studentRepositoryV4.flush();

            /*
             * 3. 비콘 삭제
             */
            beaconRepositoryV4.delete(beacon);
            beaconRepositoryV4.flush();

        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict(
                    "BEACON_IN_USE",
                    "구조도, 학생 위치, 훈련 이력 또는 미션에서 사용 중인 비콘입니다. 삭제할 수 없습니다."
            );
        }
    }

    private BeaconResponse toDto(BeaconV4 beacon) {
        return BeaconResponse.builder()
                .beaconId(beacon.getId())
                .schoolId(beacon.getSchool() != null ? beacon.getSchool().getId() : null)
                .floorIndex(beacon.getFloorIndex())
                .uuid(beacon.getUuid())
                .major(beacon.getMajor())
                .minor(beacon.getMinor())
                .beaconNo(beacon.getBeaconNo())
                .mac(beacon.getMac())
                .x(beacon.getX())
                .y(beacon.getY())
                .realXM(beacon.getRealXM())
                .realYM(beacon.getRealYM())
                .realZM(beacon.getRealZM())
                .name(beacon.getName())
                .txPower(beacon.getTxPower())
                .createdAt(beacon.getCreatedAt())
                .updatedAt(beacon.getUpdatedAt())
                .build();
    }

    public List<BeaconResponse> getBeaconsByFloor(String schoolId, Integer floorIndex) {
        if (floorIndex == null) {
            throw new IllegalArgumentException("floorIndex가 비어 있습니다.");
        }

        schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("학교가 존재하지 않습니다."));

        return beaconRepositoryV4.findBySchool_IdAndFloorIndexOrderByBeaconNoAsc(schoolId, floorIndex)
                .stream()
                .map(this::toDto)
                .toList();
    }
}