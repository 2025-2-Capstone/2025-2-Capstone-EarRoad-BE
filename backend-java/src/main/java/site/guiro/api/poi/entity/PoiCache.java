package site.guiro.api.poi.entity;

import jakarta.persistence.*;
import lombok.*;
import site.guiro.api.guide.entity.CaptureImage;
import site.guiro.api.guide.entity.ExtractedSlot;
import site.guiro.api.guide.entity.TourSession;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "poi_cache")
public class PoiCache {

    @Id
    @Column(name = "poi_key", length = 64)
    private String poiKey; // 예: 'kto:264126'

    @Column(name = "name_ko", length = 200, nullable = false)
    private String nameKo;

    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content; // 관광지 설명문

    @Column(name = "fetched_at", nullable = false, updatable = false)
    private Instant fetchedAt = Instant.now();

    @Column(name = "ttl_until")
    private Instant ttlUntil;

    @Builder.Default
    @OneToMany(mappedBy = "poiKey", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TourSession> tourSessionList = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "poiKey", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ExtractedSlot> extractedSlotList = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "poiKey", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CaptureImage> captureImageList = new ArrayList<>();

    public void updateDetails(String nameKo, String content, Instant fetchedAt) {
        this.nameKo = nameKo;
        this.content = content;
        this.fetchedAt = fetchedAt;
    }
}