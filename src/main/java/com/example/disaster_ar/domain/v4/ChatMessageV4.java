package com.example.disaster_ar.domain.v4;

import com.example.disaster_ar.domain.v4.enums.SenderRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_messages",
        indexes = {
                @Index(name = "idx_chat_scenario_time", columnList = "scenario_id, created_at"),
                @Index(name = "idx_chat_classroom_time", columnList = "classroom_id, created_at")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessageV4 {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id")
    private ScenarioV4 scenario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id")
    private ClassroomV4 classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private StudentV4 student;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_role", nullable = false, length = 20)
    private SenderRole senderRole;

    @Column(name = "message_text", nullable = false, columnDefinition = "text")
    private String messageText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "meta_json", columnDefinition = "json")
    private String metaJson;
}