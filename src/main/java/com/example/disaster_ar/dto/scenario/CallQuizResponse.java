package com.example.disaster_ar.dto.scenario;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallQuizResponse {

    private String scenarioId;
    private String missionCode;

    private List<CallCard> cards;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallCard {
        private String code;
        private String label;
    }
}