package site.guiro.api.device.entity;


import jakarta.persistence.*;
import lombok.*;
import site.guiro.api.guide.entity.CaptureImage;
import site.guiro.api.guide.entity.TourSession;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "device")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // BIGINT AUTO_INCREMENT
    private Long id;

    @Column(name = "uuid", length = 44, nullable = false) // SHA-256(Base64/Url)
    private String uuId;

    @Column(name = "platform", length = 16) // 'ANDROID' | 'IOS'
    @Enumerated(EnumType.STRING)
    private  Platform platform;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Builder.Default
    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CaptureImage> captureImageList = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TourSession> tourSessionList = new ArrayList<>();

}
