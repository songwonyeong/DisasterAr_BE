package com.example.disaster_ar.dto.monitoring;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringImageResponse {

    private String src;
    private Double naturalWidth;
    private Double naturalHeight;
}