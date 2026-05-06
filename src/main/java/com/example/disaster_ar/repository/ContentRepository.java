package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.ContentV4;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.disaster_ar.domain.v4.enums.ContentType;
import java.util.Optional;

public interface ContentRepository extends JpaRepository<ContentV4, String> {
    Optional<ContentV4> findByContentTypeAndTitle(ContentType contentType, String title);
}