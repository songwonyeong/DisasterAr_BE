package com.example.disaster_ar.domain.v4;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "beacons",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_ibeacon_triplet_per_school",
                        columnNames = {"school_id", "uuid", "major", "minor"}
                ),
                @UniqueConstraint(
                        name = "uq_beacon_no_per_school_floor",
                        columnNames = {"school_id", "floor_index", "beacon_no"}
                )
        },
        indexes = {
                @Index(name = "idx_beacons_school_floor", columnList = "school_id, floor_index")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BeaconV4 extends BaseTimeEntity {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private SchoolV4 school;

    @Column(name = "floor_index", nullable = false)
    private Integer floorIndex;

    @Column(name = "uuid", nullable = false, length = 36)
    private String uuid;

    @Column(name = "major", nullable = false)
    private Integer major;

    @Column(name = "minor", nullable = false)
    private Integer minor;

    @Column(name = "mac", length = 17)
    private String mac;

    @Column(name = "x", nullable = false)
    private Double x;

    @Column(name = "y", nullable = false)
    private Double y;

    @Column(name = "real_x_m")
    private Double realXM;

    @Column(name = "real_y_m")
    private Double realYM;

    @Column(name = "real_z_m")
    private Double realZM;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "tx_power")
    private Integer txPower;

    @Column(name = "beacon_no")
    private Integer beaconNo;
}