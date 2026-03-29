package com.example.disaster_ar.controller;

import com.example.disaster_ar.dto.item.ItemAcquireRequest;
import com.example.disaster_ar.dto.item.StudentItemResponse;
import com.example.disaster_ar.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scenarios/{scenarioId}/students/{studentId}/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    // 획득
    @PostMapping("/acquire")
    public ResponseEntity<StudentItemResponse> acquire(
            @PathVariable String scenarioId,
            @PathVariable String studentId,
            @RequestBody ItemAcquireRequest req
    ) {
        return ResponseEntity.ok(itemService.acquireItem(scenarioId, studentId, req));
    }

    // 조회
    @GetMapping
    public ResponseEntity<List<StudentItemResponse>> getItems(
            @PathVariable String scenarioId,
            @PathVariable String studentId
    ) {
        return ResponseEntity.ok(itemService.getStudentItems(scenarioId, studentId));
    }

    // 사용
    @PostMapping("/{itemId}/consume")
    public ResponseEntity<StudentItemResponse> consume(
            @PathVariable String scenarioId,
            @PathVariable String studentId,
            @PathVariable String itemId
    ) {
        return ResponseEntity.ok(itemService.consumeItem(scenarioId, studentId, itemId));
    }
}