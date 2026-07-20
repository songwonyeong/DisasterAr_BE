package com.example.disaster_ar.dto.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiRouteRequest {

    @JsonAlias("target_element_id")
    private String targetElementId;

    @JsonAlias("target_node_id")
    private String targetNodeId;

    @JsonAlias("current_beacon_element_id")
    private String currentBeaconElementId;

    /**
     * 신규 형식: floor + element_id 조합으로 목적지를 식별한다.
     * 기존 targetElementId도 하위 호환을 위해 유지한다.
     */
    private ElementRef target;

    /**
     * 테스트/AI 직접 호출 시 현재 위치를 명시하고 싶을 때 사용한다.
     * 값이 없으면 학생의 lastBeacon -> beacon_element_maps 기준으로 계산한다.
     */
    @JsonAlias("current_beacon")
    private ElementRef currentBeacon;

    @Getter
    @Setter
    public static class ElementRef {
        private Integer floor;

        @JsonAlias({"element_id", "elementId"})
        private String elementId;
    }
}
