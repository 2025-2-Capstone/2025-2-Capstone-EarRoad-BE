package site.guiro.api.guide.entity;

import jakarta.persistence.*;
import lombok.*;
import site.guiro.api.poi.entity.PoiCache;

import java.time.Instant;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "extracted_slot")
public class ExtractedSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_id")
    private Long slotId;

    @Column(name = "model_name", length = 100, nullable = false)
    private String modelName; // 'koelectra-bigru' 등

    @Column(name = "slot_json", columnDefinition = "json", nullable = false)
    private String slotJson; // JSON 문자열

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poi_key", nullable = false)
    private PoiCache poiKey;
}
