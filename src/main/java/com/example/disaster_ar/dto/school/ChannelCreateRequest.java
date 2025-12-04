package com.example.disaster_ar.dto.school;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public class ChannelCreateRequest {
    private String schoolName;
    private List<MultipartFile> mapImages; // optional

    // getters/setters
}
