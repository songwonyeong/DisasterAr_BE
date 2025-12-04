package com.example.disaster_ar.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "schools",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_schools_school_name",
                        columnNames = "school_name"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 64) // DB: VARCHAR(64)
    private String id;

    @Column(name = "school_name", nullable = false, length = 255)
    private String schoolName;

    // DDL: map_file VARCHAR(255)
    @Column(name = "map_file", length = 255)
    private String mapFile;

    @Column(name = "access_code", length = 255)
    private String accessCode;
}