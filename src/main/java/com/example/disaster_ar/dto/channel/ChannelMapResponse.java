package com.example.disaster_ar.dto.channel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelMapResponse {
    private String mapId;
    private Integer floorIndex;
    private String floorLabel;
    private String uploadedImage;
    private String outlineJson;
    private Double scaleMPerPx;
    private Double originX;
    private Double originY;
    private String elementsJson;
    private LocalDateTime updatedAt;
}