package site.guiro.api.guide.entity;

import jakarta.persistence.*;
import lombok.*;
import site.guiro.api.device.entity.Device;
import site.guiro.api.poi.entity.PoiCache;

import java.time.Instant;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "tour_session")
public class TourSession {

    @Id
    @Column(name = "session_id", length = 36, nullable = false, updatable = false)
    private String sessionId;

    @Column(name = "status", length = 16, nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;    // IDLE/DEST_SET/GUIDING/PAUSED/ENDED

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "ttl_until")
    private Instant ttlUntil;

    @Version
    @Column(name = "version")
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poi_key", nullable = false)
    private PoiCache poiKey;
}
