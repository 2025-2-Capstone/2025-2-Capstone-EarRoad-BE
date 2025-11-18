package site.guiro.api.guide.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import site.guiro.api.device.entity.Device;
import site.guiro.api.guide.dto.TourSessionDto;
import site.guiro.api.guide.entity.Status;
import site.guiro.api.guide.entity.TourSession;
import site.guiro.api.guide.repository.TourSessionRepository;
import site.guiro.api.poi.entity.PoiCache;
import site.guiro.api.poi.repository.PoiCacheRepository;

import java.time.Instant;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class TourSessionService {

    private final TourSessionRepository tourSessionRepository;
    private final PoiCacheRepository poiCacheRepository;

    @Transactional
    public TourSessionDto setDestination(Device device, String poiKey) {
        PoiCache poiCache = poiCacheRepository.findByPoiKey(poiKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "POI not found"));

        Instant now = Instant.now();

        TourSession session = tourSessionRepository.findTopByDeviceAndStatusNotOrderByStartedAtDesc(device, Status.ENDED)
                .map(existing -> {
                    existing.setDestination(poiCache, now);
                    return existing;
                })
                .orElseGet(() -> TourSession.builder()
                        .sessionId(UUID.randomUUID().toString())
                        .device(device)
                        .poiKey(poiCache)
                        .status(Status.DEST_SET)
                        .startedAt(now)
                        .updatedAt(now)
                        .endedAt(null)
                        .ttlUntil(null)
                        .build());

        return TourSessionDto.from(tourSessionRepository.save(session));
    }

    @Transactional
    public TourSessionDto start(Device device) {
        TourSession session = getActiveSessionOrThrow(device);
        if (session.getStatus() == Status.DEST_SET) {
            session.transitionToGuiding(Instant.now());
            tourSessionRepository.save(session);
        } else if (session.getStatus() != Status.GUIDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot start guiding from status: " + session.getStatus());
        }

        return TourSessionDto.from(session);
    }

    @Transactional
    public TourSessionDto pause(Device device) {
        TourSession session = getActiveSessionOrThrow(device);
        if (session.getStatus() == Status.GUIDING) {
            session.transitionToPaused(Instant.now());
            tourSessionRepository.save(session);
        } else if (session.getStatus() != Status.PAUSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot pause session from status: " + session.getStatus());
        }

        return TourSessionDto.from(session);
    }

    @Transactional
    public TourSessionDto resume(Device device) {
        TourSession session = getActiveSessionOrThrow(device);
        if (session.getStatus() == Status.PAUSED) {
            session.transitionToGuiding(Instant.now());
            tourSessionRepository.save(session);
        } else if (session.getStatus() != Status.GUIDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot resume session from status: " + session.getStatus());
        }

        return TourSessionDto.from(session);
    }

    @Transactional
    public TourSessionDto end(Device device) {
        TourSession session = getActiveSessionOrThrow(device);
        if (session.getStatus() == Status.DEST_SET || session.getStatus() == Status.GUIDING || session.getStatus() == Status.PAUSED) {
            session.end(Instant.now());
            tourSessionRepository.save(session);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot end session from status: " + session.getStatus());
        }

        return TourSessionDto.from(session);
    }

    private TourSession getActiveSessionOrThrow(Device device) {
        return tourSessionRepository.findTopByDeviceAndStatusNotOrderByStartedAtDesc(device, Status.ENDED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active session not found"));
    }

}
