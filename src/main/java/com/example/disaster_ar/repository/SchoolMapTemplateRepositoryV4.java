package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.SchoolMapTemplateV4;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchoolMapTemplateRepositoryV4 extends JpaRepository<SchoolMapTemplateV4, String> {
    List<SchoolMapTemplateV4> findBySchool_IdOrderByUpdatedAtDesc(String schoolId);
    Optional<SchoolMapTemplateV4> findBySchool_IdAndTemplateName(String schoolId, String templateName);
}