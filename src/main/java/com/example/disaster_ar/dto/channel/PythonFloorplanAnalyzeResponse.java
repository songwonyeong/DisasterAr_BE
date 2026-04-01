package com.example.disaster_ar.dto.channel;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class PythonFloorplanAnalyzeResponse {
    private List<Map<String, Object>> elements;
    private Boolean ocr_available;
}