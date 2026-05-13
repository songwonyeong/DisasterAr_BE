package com.example.disaster_ar.controller;

import com.example.disaster_ar.dto.monitoring.MonitoringMapResponse;
import com.example.disaster_ar.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;

    @Operation(summary = "[26.05.11] 구조도 비콘 모니터링 조회")
    @GetMapping("/{classroomId}/monitoring-map")
    public ResponseEntity<MonitoringMapResponse> getMonitoringMap(
            @PathVariable String classroomId
    ) {
        return ResponseEntity.ok(
                monitoringService.getMonitoringMap(classroomId)
        );
    }
}