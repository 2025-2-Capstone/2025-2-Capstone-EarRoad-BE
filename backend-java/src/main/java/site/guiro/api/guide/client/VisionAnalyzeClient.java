package site.guiro.api.guide.client;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import site.guiro.api.config.VisionApiProperties;
import site.guiro.api.guide.dto.AnalysisResult;
import org.springframework.web.multipart.MultipartFile;

@Component
public class VisionAnalyzeClient {

    private final WebClient webClient;
    private final VisionApiProperties props;

    public VisionAnalyzeClient(WebClient visionWebClient, VisionApiProperties props) {
        this.webClient = visionWebClient;
        this.props = props;
    }

    public AnalysisResult analyzePhoto(MultipartFile image, String poiKey, String sessionId) {
        ByteArrayResource filePart = new ByteArrayResource(toBytes(image)) {
            @Override public String getFilename() { return image.getOriginalFilename(); }
        };

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("image", buildFilePart(filePart, image.getContentType()));
        form.add("poiKey", poiKey);
        form.add("sessionId", sessionId);

        return webClient.post()
                .uri("/analyze/photo")
                .header("X-Auth-Token", props.token())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(form))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp ->
                        resp.bodyToMono(String.class).flatMap(msg ->
                                Mono.error(new IllegalStateException("Vision 4xx: " + msg))))
                .onStatus(HttpStatusCode::is5xxServerError, resp ->
                        resp.bodyToMono(String.class).flatMap(msg ->
                                Mono.error(new IllegalStateException("Vision 5xx: " + msg))))
                .bodyToMono(AnalysisResult.class)
                .block();
    }

    private static HttpEntity<ByteArrayResource> buildFilePart(ByteArrayResource file, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE));
        headers.setContentDispositionFormData("image", file.getFilename());
        return new HttpEntity<>(file, headers);
    }

    private static byte[] toBytes(MultipartFile file) {
        try { return file.getBytes(); }
        catch (Exception e) { throw new IllegalStateException("Failed to read multipart file", e); }
    }
}