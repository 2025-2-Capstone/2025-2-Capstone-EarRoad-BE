package site.guiro.api.guide.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "script")
public class Script {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "script_id")
    private Long scriptId;

    @Column(name = "llm_model", length = 100)
    private String llmModel; // 'gpt-4o-mini' 등

    @Column(name = "prompt_version", length = 50)
    private String promptVersion;

    @Lob
    @Column(name = "script_text", nullable = false, columnDefinition = "TEXT")
    private String scriptText;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "image_id")
    private CaptureImage imageId;
}
