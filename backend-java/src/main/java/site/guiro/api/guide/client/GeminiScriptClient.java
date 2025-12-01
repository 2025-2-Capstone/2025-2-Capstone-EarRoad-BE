package site.guiro.api.guide.client;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import site.guiro.api.config.VisionApiProperties;
import site.guiro.api.guide.dto.DemoScriptResponse;
import site.guiro.api.guide.dto.GeminiVisionResponse;

@Component
public class GeminiScriptClient {

    public record ImagePayload(byte[] bytes, String filename, String contentType) {}

    private final WebClient webClient;
    private final VisionApiProperties props;

    public GeminiScriptClient(WebClient visionWebClient, VisionApiProperties props) {
        this.webClient = visionWebClient;
        this.props = props;
    }

    public DemoScriptResponse requestDemoScript(String name, String content, MultipartFile image) {
        ByteArrayResource filePart = new ByteArrayResource(toBytes(image)) {
            @Override public String getFilename() { return image.getOriginalFilename(); }
        };

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("name", name);
        form.add("content", content);
        form.add("image", buildFilePart("image", filePart, image.getContentType()));

        return webClient.post()
                .uri("/api/v1/script/demo")
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
                .bodyToMono(DemoScriptResponse.class)
                .block();
    }


    public GeminiVisionResponse requestShortVision(String place, String overview, ImagePayload image) {
        return requestVisionGuide("/vision/guide/short", place, overview, image);
    }

    public GeminiVisionResponse requestLongVision(String place, String overview, ImagePayload image) {
        return requestVisionGuide("/vision/guide/long", place, overview, image);
    }

    private GeminiVisionResponse requestVisionGuide(String uri, String place, String overview, ImagePayload image) {
        ByteArrayResource filePart = new ByteArrayResource(image.bytes()) {
            @Override public String getFilename() { return image.filename(); }
        };

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("place", place);
        form.add("overview", overview);
        form.add("file", buildFilePart("file", filePart, image.contentType()));

        return webClient.post()
                .uri(uri)
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
                .bodyToMono(GeminiVisionResponse.class)
                .block();
    }

    private static HttpEntity<ByteArrayResource> buildFilePart(String fieldName, ByteArrayResource file, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE));
        headers.setContentDispositionFormData(fieldName, file.getFilename());
        return new HttpEntity<>(file, headers);
    }

    private static byte[] toBytes(MultipartFile file) {
        try { return file.getBytes(); }
        catch (Exception e) { throw new IllegalStateException("Failed to read multipart file", e); }
    }


}