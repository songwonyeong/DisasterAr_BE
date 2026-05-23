package com.example.disaster_ar.domain.v4;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "beacon_element_maps",
        indexes = {
                @Index(name = "idx_beacon_element_maps_school_floor", columnList = "school_id, floor_index"),
                @Index(name = "idx_beacon_element_maps_element", columnList = "school_id, floor_index, element_id"),

                // 신규 인덱스
                @Index(name = "idx_beacon_element_maps_school_floor_active", columnList = "school_id, floor_index, is_active"),
                @Index(name = "idx_beacon_element_maps_zone", columnList = "school_id, floor_index, zone_element_id"),
                @Index(name = "idx_beacon_element_maps_beacon_element", columnList = "school_id, floor_index, beacon_element_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_beacon_element_maps_beacon", columnNames = {"beacon_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BeaconElementMapV4 {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private SchoolV4 school;

    @Column(name = "floor_index", nullable = false)
    private Integer floorIndex;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beacon_id", nullable = false)
    private BeaconV4 beacon;

    /**
     * 기존 호환용 필드.
     *
     * 1차 반영에서는 element_id를 zone_element_id와 같은 의미로 유지한다.
     * 즉, SAFE_ZONE 비콘이면 element_id = SAFE_ZONE element ID.
     */
    @Column(name = "element_id", nullable = false, length = 64)
    private String elementId;

    /**
     * 구조도 JSON 안의 비콘 마커 element ID.
     * 예: beacon-marker-1
     */
    @Column(name = "beacon_element_id", length = 64)
    private String beaconElementId;

    /**
     * 비콘이 대표하는 구역 element ID.
     * 예: safe-1, disaster-1, restricted-1
     */
    @Column(name = "zone_element_id", length = 64)
    private String zoneElementId;

    /**
     * monitoring-map 표시 이름 snapshot.
     * 예: 운동장, 3층 복도, 과학실
     */
    @Column(name = "placement_name", length = 255)
    private String placementName;

    /**
     * 구역 타입 snapshot.
     * 예: SAFE_ZONE, DISASTER_ZONE, RESTRICTED_ZONE, NORMAL
     */
    @Column(name = "zone_type", length = 50)
    private String zoneType;

    /**
     * 해당 비콘 매핑의 RSSI 인정 기준.
     * 예: -85, -78
     */
    @Builder.Default
    @Column(name = "threshold_rssi", nullable = false)
    private Integer thresholdRssi = -85;

    /**
     * 위치 판정/모니터링 사용 여부.
     *
     * DB 컬럼명은 is_active지만,
     * Java 필드는 active로 두는 게 Spring Data 메서드 작성 시 덜 헷갈린다.
     */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 실제 미션/트리거/SAFE_ZONE 판정에 사용할 element ID.
     * 신규 zoneElementId가 있으면 그걸 쓰고,
     * 없으면 기존 elementId를 fallback으로 사용한다.
     */
    public String getEffectiveZoneElementId() {
        if (zoneElementId != null && !zoneElementId.isBlank()) {
            return zoneElementId;
        }
        return elementId;
    }

    /**
     * thresholdRssi가 null인 예전 데이터 보호용.
     */
    public Integer getEffectiveThresholdRssi() {
        return thresholdRssi != null ? thresholdRssi : -85;
    }

    /**
     * active가 null인 예전 데이터 보호용.
     */
    public boolean isEffectivelyActive() {
        return !Boolean.FALSE.equals(active);
    }

    /**
     * 저장 전에 legacy elementId와 신규 zoneElementId를 맞추고 싶을 때 사용.
     * Service에서 호출해도 되고, 필요 없으면 안 써도 된다.
     */
    public void syncLegacyElementId() {
        if (zoneElementId != null && !zoneElementId.isBlank()) {
            this.elementId = zoneElementId;
        } else if (elementId != null && !elementId.isBlank()) {
            this.zoneElementId = elementId;
        }
    }
}