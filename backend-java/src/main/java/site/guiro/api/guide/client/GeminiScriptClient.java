package site.guiro.api.guide.client;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import site.guiro.api.config.VisionApiProperties;
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
        form.add("file", buildFilePart(filePart, image.contentType()));

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

    private static HttpEntity<ByteArrayResource> buildFilePart(ByteArrayResource file, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE));
        headers.setContentDispositionFormData("file", file.getFilename());
        return new HttpEntity<>(file, headers);
    }
}