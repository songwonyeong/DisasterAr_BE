package com.example.disaster_ar.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "school_maps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolMap {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;   // DDL: school_id FK (NULL 허용)

    @Column(name = "floor", length = 50)
    private String floor;

    @Column(name = "map_file", length = 255)
    private String mapFile;
}
