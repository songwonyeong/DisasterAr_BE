package com.example.disaster_ar.dto.channel;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FloorplanAnalyzeResponse {
    private String mapId;
    private Integer floorIndex;
    private String floorLabel;
    private String uploadedImage;
    private List<Map<String, Object>> elements;
    private Boolean ocrAvailable;
}