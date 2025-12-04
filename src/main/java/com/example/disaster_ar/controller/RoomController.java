package com.example.disaster_ar.controller;

import com.example.disaster_ar.dto.room.RoomCreateRequest;
import com.example.disaster_ar.dto.room.RoomResponse;
import com.example.disaster_ar.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.disaster_ar.dto.room.RoomUpdateRequest;

import java.util.List;

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
}
