package site.guiro.api.guide.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import site.guiro.api.common.storage.ImageStorageService;
import site.guiro.api.device.entity.Device;
import site.guiro.api.guide.client.VisionAnalyzeClient;
import site.guiro.api.guide.dto.AnalysisResponse;
import site.guiro.api.guide.dto.AnalysisResult;
import site.guiro.api.guide.entity.CaptureImage;
import site.guiro.api.guide.repository.CaptureImageRepository;
import site.guiro.api.poi.entity.PoiCache;
import site.guiro.api.poi.repository.PoiCacheRepository;

/**
 * - 이미지 분석(FastAPI/Python) : 여러 장 중 품질/콘텐츠 기준으로 대표 이미지 1장 선택
 * - 텍스트 컨텍스트 준비(예: poiKey로 POI 콘텐츠 조회)
 * - Gemini API 호출로 최종 스크립트 생성
 * - 스크립트 ID 발급/저장(선택) 후 응답 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScriptService {

    private final VisionAnalyzeClient visionAnalyzeClient;
    private final PoiCacheRepository poiCacheRepository;
    private final CaptureImageRepository captureImageRepository;
    private final ImageStorageService imageStorageService;
    private final ObjectMapper objectMapper;

    public AnalysisResult analyzePhoto(MultipartFile image, String poiKey, String sessionId) {
        // FastAPI에 실제 분석 요청 → 품질검사 실패 시에도 JSON 형식은 동일
        return visionAnalyzeClient.analyzePhoto(image, poiKey, sessionId);
    }

    /**
     * 사진 한 장을 분석하고, S3에 업로드한 뒤 DB에 결과를 저장한다.
     */
    @Transactional
    public AnalysisResponse analyzeAndStore(MultipartFile image, String poiKey, String sessionId, Device device) {
        PoiCache poiCache = poiCacheRepository.findByPoiKey(poiKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "POI not found: " + poiKey));

        AnalysisResult result = analyzePhoto(image, poiKey, sessionId);

        log.info(
                "[ANALYZE] poiKey={} sessionId={} qualityPassed={} score={} colorfulness={} warmRatio={}",
                poiKey,
                sessionId,
                result.isQualityPassed(),
                result.getScore(),
                result.getColorfulness(),
                result.getWarmRatio()
        );

        String keyPrefix = result.isQualityPassed() ? "tourist/" : "non-tourist/";
        ImageStorageService.UploadedImage uploaded = imageStorageService.upload(image, keyPrefix, poiKey);

        CaptureImage captureImage = CaptureImage.builder()
                .imageKey(uploaded.getKey())
                .device(device)
                .poiKey(poiCache)
                .sessionId(sessionId)
                .qualityPassed(result.isQualityPassed())
                .colorfulness(result.getColorfulness())
                .warmRatio(result.getWarmRatio())
                .pHash(result.getPHash())
                .score(result.getScore())
                .objectsJson(writeObjectsJson(result.getObjects()))
                .build();

        captureImageRepository.save(captureImage);

        return AnalysisResponse.builder()
                .qualityPassed(result.isQualityPassed())
                .imageKey(uploaded.getKey())
                .imageUrl(uploaded.getUrl())
                .score(result.getScore())
                .build();
    }

    private String writeObjectsJson(Object objects) {
        if (objects == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(objects);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("객체 탐지 결과 직렬화에 실패했습니다", e);
        }
    }
}
