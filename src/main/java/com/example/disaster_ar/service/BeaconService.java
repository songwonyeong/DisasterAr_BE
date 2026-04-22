package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.BeaconV4;
import com.example.disaster_ar.domain.v4.SchoolV4;
import com.example.disaster_ar.dto.beacon.BeaconRequest;
import com.example.disaster_ar.dto.beacon.BeaconResponse;
import com.example.disaster_ar.repository.BeaconRepositoryV4;
import com.example.disaster_ar.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BeaconService {

    private final BeaconRepositoryV4 beaconRepositoryV4;
    private final SchoolRepository schoolRepository;

    public BeaconResponse createBeacon(BeaconRequest req) {
        SchoolV4 school = schoolRepository.findById(req.getSchoolId())
                .orElseThrow(() -> new IllegalArgumentException("학교가 존재하지 않습니다."));

        BeaconV4 beacon = new BeaconV4();
        beacon.setId(UUID.randomUUID().toString());
        beacon.setSchool(school);
        beacon.setFloorIndex(req.getFloorIndex());
        beacon.setUuid(req.getUuid());
        beacon.setMajor(req.getMajor());
        beacon.setMinor(req.getMinor());
        beacon.setBeaconNo(req.getBeaconNo());
        beacon.setMac(req.getMac());
        beacon.setX(req.getX());
        beacon.setY(req.getY());
        beacon.setRealXM(req.getRealXM());
        beacon.setRealYM(req.getRealYM());
        beacon.setRealZM(req.getRealZM());
        beacon.setName(req.getName());
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

    public BeaconResponse updateBeacon(String beaconId, BeaconRequest req) {
        BeaconV4 beacon = beaconRepositoryV4.findById(beaconId)
                .orElseThrow(() -> new IllegalArgumentException("비콘이 존재하지 않습니다."));

        if (req.getFloorIndex() != null) beacon.setFloorIndex(req.getFloorIndex());
        if (req.getUuid() != null) beacon.setUuid(req.getUuid());
        if (req.getMajor() != null) beacon.setMajor(req.getMajor());
        if (req.getMinor() != null) beacon.setMinor(req.getMinor());
        if (req.getBeaconNo() != null) beacon.setBeaconNo(req.getBeaconNo());
        if (req.getMac() != null) beacon.setMac(req.getMac());
        if (req.getX() != null) beacon.setX(req.getX());
        if (req.getY() != null) beacon.setY(req.getY());
        if (req.getRealXM() != null) beacon.setRealXM(req.getRealXM());
        if (req.getRealYM() != null) beacon.setRealYM(req.getRealYM());
        if (req.getRealZM() != null) beacon.setRealZM(req.getRealZM());
        if (req.getName() != null) beacon.setName(req.getName());
        if (req.getTxPower() != null) beacon.setTxPower(req.getTxPower());

        beacon.setUpdatedAt(LocalDateTime.now());

        BeaconV4 saved = beaconRepositoryV4.save(beacon);
        return toDto(saved);
    }

    public void deleteBeacon(String beaconId) {
        BeaconV4 beacon = beaconRepositoryV4.findById(beaconId)
                .orElseThrow(() -> new IllegalArgumentException("비콘이 존재하지 않습니다."));

        beaconRepositoryV4.delete(beacon);
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