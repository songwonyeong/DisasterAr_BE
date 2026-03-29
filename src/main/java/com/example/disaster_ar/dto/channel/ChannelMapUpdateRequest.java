package com.example.disaster_ar.dto.channel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChannelMapUpdateRequest {
    private Integer floorIndex;
    private String floorLabel;
    private String outlineJson;
    private Double scaleMPerPx;
    private Double originX;
    private Double originY;
    private String elementsJson;
}