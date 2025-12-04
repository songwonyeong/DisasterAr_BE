package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.School;
import com.example.disaster_ar.domain.SchoolMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolMapRepository extends JpaRepository<SchoolMap, String> {

    List<SchoolMap> findBySchool(School school);
}
