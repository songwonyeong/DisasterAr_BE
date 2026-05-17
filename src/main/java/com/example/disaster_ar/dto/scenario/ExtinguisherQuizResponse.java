package com.example.disaster_ar.dto.scenario;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtinguisherQuizResponse {

    private String scenarioId;
    private String missionCode;

    private Integer life;
    private Integer cooldownSeconds;

    // 추가: Unity가 카드 구성/정답 판정을 담당하는지
    private Boolean clientManaged;

    // 추가: 백엔드가 selectedOrder를 판정하는지 여부
    private Boolean serverJudgement;

    // 추가: 프론트 안내용 메시지
    private String message;

    // 기존 호환용으로 유지. 공식 흐름에서는 빈 배열로 내려갈 예정.
    private List<Card> cards;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Card {
        private String code;
        private String label;
        private String imageKey;
    }
}