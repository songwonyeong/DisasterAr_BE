package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.ContentV4;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepository extends JpaRepository<ContentV4, String> {
}