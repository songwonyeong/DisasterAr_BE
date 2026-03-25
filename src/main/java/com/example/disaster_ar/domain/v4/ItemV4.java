package com.example.disaster_ar.domain.v4;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_items_item_code", columnNames = "item_code")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ItemV4 extends BaseTimeEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "item_type", length = 30)
    private String itemType;

    @Column(name = "meta_json", columnDefinition = "json")
    private String metaJson;
}