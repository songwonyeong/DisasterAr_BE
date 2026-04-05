package com.example.disaster_ar.controller;

import com.example.disaster_ar.dto.beacon.BeaconRequest;
import com.example.disaster_ar.dto.beacon.BeaconResponse;
import com.example.disaster_ar.service.BeaconService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.disaster_ar.dto.beacon.BeaconScanRequest;
import com.example.disaster_ar.service.BeaconTrackingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/beacons")
@RequiredArgsConstructor
public class BeaconController {

    private final BeaconService beaconService;
    private final BeaconTrackingService beaconTrackingService;

    @Operation(summary = "비콘 등록")
    @PostMapping
    public ResponseEntity<BeaconResponse> createBeacon(
            @RequestBody BeaconRequest req
    ) {
        return ResponseEntity.ok(beaconService.createBeacon(req));
    }

    @Operation(summary = "학교별 비콘 목록 조회")
    @GetMapping
    public ResponseEntity<List<BeaconResponse>> getBeacons(
            @RequestParam String schoolId
    ) {
        return ResponseEntity.ok(beaconService.getBeacons(schoolId));
    }

    @Operation(summary = "비콘 수정")
    @PutMapping("/{beaconId}")
    public ResponseEntity<BeaconResponse> updateBeacon(
            @PathVariable String beaconId,
            @RequestBody BeaconRequest req
    ) {
        return ResponseEntity.ok(beaconService.updateBeacon(beaconId, req));
    }

    @Operation(summary = "비콘 삭제")
    @DeleteMapping("/{beaconId}")
    public ResponseEntity<Void> deleteBeacon(
            @PathVariable String beaconId
    ) {
        beaconService.deleteBeacon(beaconId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "비콘 스캔 업로드")
    @PostMapping("/scan")
    public ResponseEntity<Void> scanBeacon(
            @RequestBody BeaconScanRequest req
    ) {
        beaconTrackingService.processScan(req);
        return ResponseEntity.ok().build();
    }
}