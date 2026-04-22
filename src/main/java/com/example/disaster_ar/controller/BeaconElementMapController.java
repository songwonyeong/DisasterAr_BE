package com.example.disaster_ar.controller;

import com.example.disaster_ar.dto.beacon.BeaconElementMapCreateRequest;
import com.example.disaster_ar.dto.beacon.BeaconElementMapResponse;
import com.example.disaster_ar.service.BeaconElementMapService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/beacon-element-maps")
@RequiredArgsConstructor
public class BeaconElementMapController {

    private final BeaconElementMapService beaconElementMapService;

    @Operation(summary = "[26.04.15] 비콘-구조도 요소 매핑 생성 API")
    @PostMapping
    public ResponseEntity<BeaconElementMapResponse> createMapping(
            @RequestBody BeaconElementMapCreateRequest request
    ) {
        return ResponseEntity.ok(
                beaconElementMapService.createMapping(request)
        );
    }

    @Operation(summary = "[26.04.15] 비콘-구조도 요소 매핑 목록 조회 API")
    @GetMapping
    public ResponseEntity<List<BeaconElementMapResponse>> getMappings(
            @RequestParam String schoolId,
            @RequestParam Integer floorIndex
    ) {
        return ResponseEntity.ok(
                beaconElementMapService.getMappings(schoolId, floorIndex)
        );
    }

    @Operation(summary = "[26.04.15] 비콘-구조도 요소 매핑 삭제 API")
    @DeleteMapping("/{mappingId}")
    public ResponseEntity<Void> deleteMapping(
            @PathVariable String mappingId
    ) {
        beaconElementMapService.deleteMapping(mappingId);
        return ResponseEntity.noContent().build();
    }
}