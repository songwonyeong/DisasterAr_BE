package com.example.disaster_ar.domain.v4;

import com.example.disaster_ar.domain.v4.enums.*;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contents", indexes = {
        @Index(name = "idx_contents_school", columnList = "school_id"),
        @Index(name = "idx_contents_type", columnList = "content_type")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ContentV4 extends BaseTimeEntity {

    @Id
    @Column(length = 64)
    private String id; // CSV에서 들어오는 ID일 가능성 높음(자동생성 X 추천)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private SchoolV4 school;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 30)
    private ContentType contentType;

    // SCENARIO_EVENT
    @Column(name = "place", length = 255)
    private String place;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    // QUIZ
    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_type", length = 10)
    private QuizType quizType;

    @Column(name = "question", columnDefinition = "text")
    private String question;

    @Column(name = "option1", columnDefinition = "text") private String option1;
    @Column(name = "option2", columnDefinition = "text") private String option2;
    @Column(name = "option3", columnDefinition = "text") private String option3;
    @Column(name = "option4", columnDefinition = "text") private String option4;

    @Column(name = "answer")
    private Integer answer;

    // MISSION
    @Enumerated(EnumType.STRING)
    @Column(name = "mission_scope", length = 20)
    private MissionScope missionScope;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "objective_json", columnDefinition = "json")
    private String objectiveJson;

    @Column(name = "answer_json", columnDefinition = "json")
    private String answerJson;
}