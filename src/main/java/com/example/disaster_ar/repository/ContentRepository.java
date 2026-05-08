package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.ContentV4;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.disaster_ar.domain.v4.enums.ContentType;
import java.util.*;

public interface ContentRepository extends JpaRepository<ContentV4, String> {

    List<ContentV4> findByContentTypeAndTitleOrderByIdAsc(
            ContentType contentType,
            String title
    );

    List<ContentV4> findByContentTypeAndPlace(ContentType contentType, String place);

    List<ContentV4> findByContentType(ContentType contentType);
}