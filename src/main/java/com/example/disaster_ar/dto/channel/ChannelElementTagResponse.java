package com.example.disaster_ar.dto.channel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ChannelElementTagResponse {
    private String elementId;
    private Integer floorIndex;
    private String elementType;
    private String name;
    private String tagsJson;
    private LocalDateTime updatedAt;
}