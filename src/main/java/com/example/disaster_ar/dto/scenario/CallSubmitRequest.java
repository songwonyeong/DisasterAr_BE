package com.example.disaster_ar.dto.scenario;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallSubmitRequest {

    private String studentId;
    private String assignmentId;

    // 예: ["NAME", "ADDRESS", "PHONE", "CAUSE"]
    private List<String> selectedOrder;
}