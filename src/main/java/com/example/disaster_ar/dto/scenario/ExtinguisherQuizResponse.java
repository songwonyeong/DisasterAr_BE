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

    private List<Card> cards;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Card {
        private String code;
        private String label;
    }
}