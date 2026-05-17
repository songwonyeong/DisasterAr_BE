package com.example.disaster_ar.dto.scenario;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtinguisherQuizResultRequest {

    private String studentId;
    private String assignmentId;

    // Unity가 로컬 카드 기준으로 판정한 결과
    private Boolean isCorrect;

    // Unity에서 관리한 남은 목숨
    private Integer remainingLife;

    // 선택값. 필요하면 프론트에서 보내기
    private Integer attemptCount;
}