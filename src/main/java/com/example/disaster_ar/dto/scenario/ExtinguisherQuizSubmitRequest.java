package com.example.disaster_ar.dto.scenario;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtinguisherQuizSubmitRequest {

    private String studentId;
    private String assignmentId;

    // 예: ["PULL_PIN", "AIM_NOZZLE", "SQUEEZE_HANDLE", "SWEEP_SIDE_TO_SIDE"]
    private List<String> selectedOrder;
}