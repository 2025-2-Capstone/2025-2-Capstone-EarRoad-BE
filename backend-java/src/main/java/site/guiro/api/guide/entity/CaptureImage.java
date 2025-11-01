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
@Table(name = "capture_image")
public class CaptureImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    @Setter
    @Column(name = "url", length = 100)
    private String imageKey; // S3에 들어갈 사진 키값

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poi_key")
    private PoiCache poiKey;

}
