package com.example.disaster_ar.dto.channel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplyAnalysisRequest {
    private String outlineJson;
    private String elementsJson;
}