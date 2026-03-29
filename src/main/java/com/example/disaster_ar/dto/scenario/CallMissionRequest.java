package com.example.disaster_ar.dto.scenario;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CallMissionRequest {
    private String studentId;
    private String assignmentId; // 🔥 핵심 추가
    private Boolean success;     // end에서 사용
}