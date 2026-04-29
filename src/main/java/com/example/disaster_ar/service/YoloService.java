package com.example.disaster_ar.service;

import com.example.disaster_ar.util.MultipartInputStreamFileResource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class YoloService {

    @Value("${floorplan.analyzer.base-url}")
    private String aiBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> detect(MultipartFile file, Double conf) {

        String url = aiBaseUrl + "/detect?conf=" + (conf != null ? conf : 0.3);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        try {
            body.add("file", new MultipartInputStreamFileResource(
                    file.getInputStream(),
                    file.getOriginalFilename()
            ));
        } catch (IOException e) {
            throw new RuntimeException("파일 변환 실패", e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                url,
                requestEntity,
                Map.class
        );

        return response.getBody();
    }
}