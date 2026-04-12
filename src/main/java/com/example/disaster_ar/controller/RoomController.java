package com.example.disaster_ar.controller;

import com.example.disaster_ar.dto.room.*;
import com.example.disaster_ar.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.disaster_ar.dto.room.StudentRoomResponse;
import com.example.disaster_ar.dto.room.StudentKickResponse;
import com.example.disaster_ar.dto.room.ActiveMapResponse;
import com.example.disaster_ar.dto.room.ActiveMapUpdateRequest;
import com.example.disaster_ar.dto.room.GameStartContextResponse;
import com.example.disaster_ar.dto.room.ActiveAssignmentResponse;
import java.util.List;
import com.example.disaster_ar.dto.room.CreateMapVersionFromChannelSetRequest;
import com.example.disaster_ar.dto.room.RoomMapResponse;

@Tag(name = "Room")
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    /**
     * 방 생성 (선생님/관리자가 호출)
     * - body: schoolId, userId, className
     * - 반환: joinCode 포함된 RoomResponse
     */
    @Operation(summary = "방 생성 (joinCode 발급)")
    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@RequestBody RoomCreateRequest req) {
        RoomResponse res = roomService.createRoom(req);
        return ResponseEntity.ok(res);
    }

    /**
     * 특정 채널(학교)의 방 목록 조회
     */
    @Operation(summary = "학교(채널) 기준 방 목록 조회")
    @GetMapping
    public ResponseEntity<List<RoomResponse>> listRoomsBySchool(@RequestParam("schoolId") String schoolId) {
        List<RoomResponse> rooms = roomService.listBySchool(schoolId);
        return ResponseEntity.ok(rooms);
    }

    /**
     * 방 정보 수정 (방 만든 사람만 가능)
     */
    @Operation(summary = "방 정보 수정 (방 생성자만 가능)")
    @PatchMapping("/{classroomId}")
    public ResponseEntity<RoomResponse> updateRoom(
            @PathVariable String classroomId,
            @RequestBody RoomUpdateRequest req
    ) {
        req.setClassroomId(classroomId);
        RoomResponse res = roomService.updateRoom(req);
        return ResponseEntity.ok(res);
    }

    /**
     * 방 삭제 (방 만든 사람만 가능)
     */
    @Operation(summary = "방 삭제 (방 생성자만 가능)")
    @DeleteMapping("/{classroomId}")
    public ResponseEntity<Void> deleteRoom(
            @PathVariable String classroomId,
            @RequestParam("userId") String userId
    ) {
        roomService.deleteRoom(classroomId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 방 입장 코드 재발급 (방 만든 사람만 가능)
     */
    @Operation(summary = "방 입장 코드 재발급 (방 생성자만 가능)")
    @PutMapping("/{classroomId}/join-code")
    public ResponseEntity<RoomResponse> regenerateJoinCode(
            @PathVariable String classroomId,
            @RequestParam("userId") String userId
    ) {
        RoomResponse res = roomService.regenerateJoinCode(classroomId, userId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{classroomId}/training/start")
    public ResponseEntity<TrainingControlResponse> startTraining(
            @PathVariable String classroomId,
            @RequestBody TrainingStartRequest req
    ) {
        return ResponseEntity.ok(roomService.startTraining(classroomId, req));
    }

    @GetMapping("/{classroomId}/training-status")
    public ResponseEntity<TrainingStatusResponse> getTrainingStatus(
            @PathVariable String classroomId,
            @RequestParam String studentId
    ) {
        return ResponseEntity.ok(roomService.getTrainingStatus(classroomId, studentId));
    }

    @GetMapping("/{classroomId}/map")
    public ResponseEntity<RoomMapResponse> getRoomMap(
            @PathVariable String classroomId
    ) {
        return ResponseEntity.ok(roomService.getRoomMap(classroomId));
    }

    @PostMapping("/{classroomId}/training/end")
    public ResponseEntity<TrainingControlResponse> endTraining(
            @PathVariable String classroomId
    ) {
        return ResponseEntity.ok(roomService.endTraining(classroomId));
    }

    @GetMapping("/{classroomId}/students")
    public ResponseEntity<List<StudentRoomResponse>> getStudents(
            @PathVariable String classroomId
    ) {
        return ResponseEntity.ok(roomService.getStudents(classroomId));
    }

    @DeleteMapping("/{classroomId}/students/{studentId}")
    public ResponseEntity<StudentKickResponse> kickStudent(
            @PathVariable String classroomId,
            @PathVariable String studentId
    ) {
        return ResponseEntity.ok(roomService.kickStudent(classroomId, studentId));
    }

    @PostMapping("/{classroomId}/map-versions")
    public ResponseEntity<RoomMapVersionResponse> createMapVersion(
            @PathVariable String classroomId,
            @RequestBody RoomMapVersionCreateRequest req
    ) {
        return ResponseEntity.ok(
                roomService.createMapVersion(classroomId, req)
        );
    }

    @PutMapping("/{classroomId}/active-map")
    public ResponseEntity<ActiveMapResponse> updateActiveMap(
            @PathVariable String classroomId,
            @RequestBody ActiveMapUpdateRequest req
    ) {
        return ResponseEntity.ok(
                roomService.updateActiveMap(classroomId, req)
        );
    }

    @GetMapping("/{classroomId}/game-start-context")
    public ResponseEntity<GameStartContextResponse> getGameStartContext(
            @PathVariable String classroomId
    ) {
        return ResponseEntity.ok(
                roomService.getGameStartContext(classroomId)
        );
    }

    @GetMapping("/{classroomId}/map-versions")
    public ResponseEntity<java.util.List<RoomMapVersionSummaryResponse>> getMapVersions(
            @PathVariable String classroomId
    ) {
        return ResponseEntity.ok(roomService.getMapVersions(classroomId));
    }

    @GetMapping("/{classroomId}/map-versions/{mapVersionId}")
    public ResponseEntity<RoomMapVersionDetailResponse> getMapVersion(
            @PathVariable String classroomId,
            @PathVariable String mapVersionId
    ) {
        return ResponseEntity.ok(roomService.getMapVersion(classroomId, mapVersionId));
    }

    @PutMapping("/{classroomId}/map-versions/{mapVersionId}")
    public ResponseEntity<RoomMapVersionDetailResponse> updateMapVersion(
            @PathVariable String classroomId,
            @PathVariable String mapVersionId,
            @RequestBody RoomMapVersionUpdateRequest req
    ) {
        return ResponseEntity.ok(roomService.updateMapVersion(classroomId, mapVersionId, req));
    }

    @PostMapping("/{classroomId}/map-versions/from-channel")
    public ResponseEntity<RoomMapVersionResponse> createMapVersionFromChannel(
            @PathVariable String classroomId,
            @RequestBody FromChannelRequest req
    ) {
        return ResponseEntity.ok(roomService.createMapVersionFromChannel(classroomId, req));
    }

    @PostMapping("/{classroomId}/map-versions/from-template")
    public ResponseEntity<RoomMapVersionResponse> createMapVersionFromTemplate(
            @PathVariable String classroomId,
            @RequestBody FromTemplateRequest req
    ) {
        return ResponseEntity.ok(roomService.createMapVersionFromTemplate(classroomId, req));
    }

    @PostMapping("/{classroomId}/map-versions/{mapVersionId}/save-as-template")
    public ResponseEntity<String> saveMapVersionAsTemplate(
            @PathVariable String classroomId,
            @PathVariable String mapVersionId,
            @RequestBody SaveTemplateRequest req
    ) {
        return ResponseEntity.ok(roomService.saveMapVersionAsTemplate(classroomId, mapVersionId, req));
    }

    @GetMapping("/{classroomId}/students/{studentId}/active-assignments")
    public ResponseEntity<List<ActiveAssignmentResponse>> getActiveAssignments(
            @PathVariable String classroomId,
            @PathVariable String studentId
    ) {
        return ResponseEntity.ok(roomService.getActiveAssignments(classroomId, studentId));
    }

    @Operation(summary = "[26.04.08] 학교 전체 채널 구조도를 맵 버전으로 복사 API")
    @PostMapping("/{classroomId}/map-versions/from-channel-set")
    public ResponseEntity<RoomMapResponse> createMapVersionFromChannelSet(
            @PathVariable String classroomId,
            @RequestBody CreateMapVersionFromChannelSetRequest request
    ) {
        return ResponseEntity.ok(
                roomService.createMapVersionFromChannelSet(classroomId, request)
        );
    }
}
