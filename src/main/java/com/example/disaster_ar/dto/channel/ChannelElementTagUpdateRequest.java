package com.example.disaster_ar.dto.channel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChannelElementTagUpdateRequest {
    private String name;
    private String tagsJson;
}