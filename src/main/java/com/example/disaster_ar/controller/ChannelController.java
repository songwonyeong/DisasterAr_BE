package com.example.disaster_ar.controller;

import com.example.disaster_ar.domain.v4.SchoolV4;
import com.example.disaster_ar.dto.channel.*;
import com.example.disaster_ar.service.ChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Tag(name = "Channel")
@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    // 지도 업로드 기본 경로 (application.properties 에서 덮어쓸 수 있음)
    @Value("${file.upload.dir:uploads/maps}")
    private String uploadDir;

//    // ✅ 1) 학교 채널 생성 - 이미지 여러 개 업로드 가능
//    @Operation(summary = "학교 채널 생성(지도 이미지 여러 개 선택 가능)")
//    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
//    public ResponseEntity<SchoolV4> createChannel(
//            @RequestPart("schoolName") String schoolName,
//            @RequestPart(value = "mapImages", required = false) List<MultipartFile> mapImages
//    ) {
//        SchoolV4 created = channelService.createChannel(schoolName, mapImages, uploadDir);
//        return ResponseEntity.ok(created);
//    }

    // ✅ 1) 학교 채널 생성 - 이미지 여러 개 업로드 가능
    @Operation(summary = "학교 채널 생성(지도 이미지 여러 개 선택 가능)")
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<SchoolV4> createChannel(
            @RequestPart("schoolName") String schoolName,
            @RequestPart(value = "mapImages", required = false) List<MultipartFile> mapImages
    ) {
        System.out.println("===== createChannel called =====");
        System.out.println("schoolName = " + schoolName);
        System.out.println("mapImages is null? " + (mapImages == null));
        System.out.println("mapImages size = " + (mapImages == null ? "null" : mapImages.size()));

        if (mapImages != null) {
            for (int i = 0; i < mapImages.size(); i++) {
                MultipartFile f = mapImages.get(i);
                System.out.println(
                        "file[" + i + "] originalFilename=" + f.getOriginalFilename()
                                + ", empty=" + f.isEmpty()
                                + ", size=" + f.getSize()
                                + ", contentType=" + f.getContentType()
                );
            }
        }

        SchoolV4 created = channelService.createChannel(schoolName, mapImages, uploadDir);
        return ResponseEntity.ok(created);
    }

    // ✅ 2) 채널 코드 조회 (기존 room-code 엔드포인트 재사용)
    @Operation(summary = "채널 코드 조회")
    @GetMapping("/{schoolId}/room-code")
    public ResponseEntity<String> getChannelCode(@PathVariable String schoolId) {
        // 서비스 메서드 이름은 기존 getRoomCode 그대로 사용
        return ResponseEntity.ok(channelService.getRoomCode(schoolId));
    }

    // ✅ 3) 채널 코드 재발급
    @Operation(summary = "채널 코드 재발급")
    @PutMapping("/{schoolId}/room-code")
    public ResponseEntity<String> regenerateChannelCode(@PathVariable String schoolId) {
        // 서비스 메서드 이름은 기존 regenerateRoomCode 그대로 사용
        return ResponseEntity.ok(channelService.regenerateRoomCode(schoolId));
    }

    // ✅ 4) 채널 코드로 채널 입장
    @Operation(summary = "채널 코드로 채널 입장")
    @PostMapping("/join-school")
    public ResponseEntity<JoinSchoolResponse> joinSchool(@RequestBody JoinSchoolRequest req) {
        // 🔥 DTO가 이제 accessCode가 아니라 channelCode 필드를 쓰므로 여기 수정이 핵심
        return ResponseEntity.ok(channelService.joinByAccessCode(req.getChannelCode()));
    }

    // ✅ 5) 방 코드(joinCode)로 교실 입장
    @Operation(summary = "방 입장 코드로 교실 입장")
    @PostMapping("/join-classroom")
    public ResponseEntity<JoinClassroomResponse> joinClassroom(
            @RequestBody JoinClassroomRequest req
    ) {
        return ResponseEntity.ok(channelService.joinRoom(req));
    }

    @GetMapping("/{schoolId}/maps")
    public ResponseEntity<List<ChannelMapResponse>> getChannelMaps(
            @PathVariable String schoolId
    ) {
        return ResponseEntity.ok(channelService.getChannelMaps(schoolId));
    }

    @PutMapping("/{schoolId}/maps/{mapId}")
    public ResponseEntity<ChannelMapResponse> updateChannelMap(
            @PathVariable String schoolId,
            @PathVariable String mapId,
            @RequestBody ChannelMapUpdateRequest req
    ) {
        return ResponseEntity.ok(channelService.updateChannelMap(schoolId, mapId, req));
    }

    @PostMapping("/{schoolId}/maps/{mapId}/analyze")
    public ResponseEntity<FloorplanAnalyzeResponse> analyzeChannelMap(
            @PathVariable String schoolId,
            @PathVariable String mapId
    ) {
        return ResponseEntity.ok(
                channelService.analyzeChannelMap(schoolId, mapId)
        );
    }

    @PostMapping(value = "/{schoolId}/maps", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChannelMapResponse> addChannelMap(
            @PathVariable String schoolId,
            @RequestPart("image") MultipartFile image,
            @RequestPart(value = "floorIndex", required = false) Integer floorIndex,
            @RequestPart(value = "floorLabel", required = false) String floorLabel
    ) {
        return ResponseEntity.ok(
                channelService.addChannelMap(schoolId, image, floorIndex, floorLabel)
        );
    }

    @GetMapping("/{schoolId}/maps/{mapId}")
    public ResponseEntity<ChannelMapResponse> getChannelMap(
            @PathVariable String schoolId,
            @PathVariable String mapId
    ) {
        return ResponseEntity.ok(
                channelService.getChannelMap(schoolId, mapId)
        );
    }

    @DeleteMapping("/{schoolId}/maps/{mapId}")
    public ResponseEntity<Void> deleteChannelMap(
            @PathVariable String schoolId,
            @PathVariable String mapId
    ) {
        channelService.deleteChannelMap(schoolId, mapId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "[26.04.08] 구조도 분석 결과 적용 API")
    @PostMapping("/{schoolId}/maps/{mapId}/analysis/apply")
    public ResponseEntity<ChannelMapResponse> applyAnalysis(
            @PathVariable String schoolId,
            @PathVariable String mapId,
            @RequestBody ApplyAnalysisRequest request
    ) {
        return ResponseEntity.ok(
                channelService.applyAnalysis(schoolId, mapId, request)
        );
    }

    @Operation(summary = "[26.04.08] 구조도 요소 목록 조회 API")
    @GetMapping("/{schoolId}/maps/{mapId}/elements")
    public ResponseEntity<List<ChannelElementTagResponse>> getMapElements(
            @PathVariable String schoolId,
            @PathVariable String mapId
    ) {
        return ResponseEntity.ok(
                channelService.getMapElements(schoolId, mapId)
        );
    }

    @Operation(summary = "[26.04.08] 구조도 요소 단건 조회 API")
    @GetMapping("/{schoolId}/maps/{mapId}/elements/{elementId}")
    public ResponseEntity<ChannelElementTagResponse> getMapElement(
            @PathVariable String schoolId,
            @PathVariable String mapId,
            @PathVariable String elementId
    ) {
        return ResponseEntity.ok(
                channelService.getMapElement(schoolId, mapId, elementId)
        );
    }

    @Operation(summary = "[26.04.08] 구조도 요소 이름/태그 수정 API")
    @PutMapping("/{schoolId}/maps/{mapId}/elements/{elementId}")
    public ResponseEntity<ChannelElementTagResponse> updateMapElement(
            @PathVariable String schoolId,
            @PathVariable String mapId,
            @PathVariable String elementId,
            @RequestBody ChannelElementTagUpdateRequest request
    ) {
        return ResponseEntity.ok(
                channelService.updateMapElement(schoolId, mapId, elementId, request)
        );
    }
}
