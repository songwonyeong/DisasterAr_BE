package com.example.disaster_ar.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "DisasterAR Teacher Dashboard API",
                version = "1.0",
                description = "교사용 재난 대시보드 백엔드 API 문서"
        )
)
public class OpenApiConfig {}
