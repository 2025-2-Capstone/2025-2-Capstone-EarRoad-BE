package site.guiro.api.poi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import site.guiro.api.poi.config.TourApiProperties;
import site.guiro.api.poi.dto.NearbyResponse;
import site.guiro.api.poi.dto.PoiResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TourApiClient {

    private static final String PATH_LOCATION_BASED_LIST = "/locationBasedList2";
    private static final String PATH_DETAIL_COMMON = "/detailCommon2";
    private static final String NUMOFROWS = "50";
    private static final String QUERY_PARAM_MOBILE_OS = "ETC";
    private static final String QUERY_PARAM_MOBILE_APP = "guiro";
    private static final String QUERY_PARAM_TYPE = "json";
    private static final String QUERY_PARAM_ARRANGE = "E";
    private static final String CONTENT_TYPE_ID_TOURIST_ATTRACTION = "12";
    private static final String DEFAULT_IMAGE_PLACEHOLDER = "NO_IMAGE";

    private final WebClient tourWebClient;
    private final TourApiProperties properties;

    public NearbyResponse fetchNearbyPois(double latitude, double longitude, double radiusMeters) {
        JsonNode responseNode = tourWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PATH_LOCATION_BASED_LIST)
                        .queryParam("serviceKey", properties.serviceKey())
                        .queryParam("numOfRows", NUMOFROWS)
                        .queryParam("MobileOS", QUERY_PARAM_MOBILE_OS)
                        .queryParam("MobileApp", QUERY_PARAM_MOBILE_APP)
                        .queryParam("_type", QUERY_PARAM_TYPE)
                        .queryParam("arrange", QUERY_PARAM_ARRANGE)
                        .queryParam("mapX", doubleToString(longitude))
                        .queryParam("mapY", doubleToString(latitude))
                        .queryParam("radius", Math.round(radiusMeters))
                        .queryParam("contentTypeId", CONTENT_TYPE_ID_TOURIST_ATTRACTION)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        return mapToNearbyResponse(responseNode);
    }

    public PoiResponse fetchPoiDetail(String poiKey) {
        JsonNode responseNode = tourWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PATH_DETAIL_COMMON)
                        .queryParam("serviceKey", properties.serviceKey())
                        .queryParam("MobileOS", QUERY_PARAM_MOBILE_OS)
                        .queryParam("MobileApp", QUERY_PARAM_MOBILE_APP)
                        .queryParam("_type", QUERY_PARAM_TYPE)
                        .queryParam("contentId", poiKey)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        return mapToPoiResponse(responseNode);
    }

    private NearbyResponse mapToNearbyResponse(JsonNode root) {
        if (root == null) {
            return emptyResponse();
        }

        JsonNode responseNode = root.path("response");
        JsonNode bodyNode = responseNode.isMissingNode() ? root.path("body") : responseNode.path("body");
        if (bodyNode.isMissingNode() || bodyNode.isNull()) {
            return emptyResponse();
        }

        int pageNo = bodyNode.path("pageNo").asInt(0);
        int numOfRows = bodyNode.path("numOfRows").asInt(0);
        int totalCount = bodyNode.path("totalCount").asInt(0);

        JsonNode itemsNode = bodyNode.path("items");
        JsonNode itemNode = itemsNode.path("item");
        List<NearbyResponse.PoiSummaryItem> items = new ArrayList<>();

        if (itemNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) itemNode;
            arrayNode.forEach(node -> items.add(mapToPoiSummary(node)));
        } else if (itemNode.isObject()) {
            items.add(mapToPoiSummary(itemNode));
        }

        return NearbyResponse.builder()
                .content(items)
                .page(pageNo)
                .size(numOfRows)
                .totalElements(totalCount)
                .build();
    }

    private NearbyResponse.PoiSummaryItem mapToPoiSummary(JsonNode node) {
        if (node == null || node.isNull()) {
            return NearbyResponse.PoiSummaryItem.builder()
                    .poiKey("")
                    .name("")
                    .distance(0.0)
                    .build();
        }

        String poiKey = node.path("contentid").asText("");
        String name = node.path("title").asText("");
        double distance = parseDistance(node.path("dist").asText(null));
        String imageUrl = normalizeImageUrl(node.path("firstimage").asText(null));

        return NearbyResponse.PoiSummaryItem.builder()
                .poiKey(poiKey)
                .name(name)
                .distance(distance)
                .imageUrl(imageUrl)
                .build();
    }

    private double parseDistance(String value) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignore) {
            return 0.0;
        }
    }

    private PoiResponse mapToPoiResponse(JsonNode root) {
        if (root == null) {
            return PoiResponse.builder()
                    .poiKey("")
                    .name("")
                    .content("")
                    .address("")
                    .imageUrl(DEFAULT_IMAGE_PLACEHOLDER)
                    .build();
        }

        JsonNode responseNode = root.path("response");
        JsonNode bodyNode = responseNode.isMissingNode() ? root.path("body") : responseNode.path("body");
        if (bodyNode.isMissingNode() || bodyNode.isNull()) {
            return PoiResponse.builder()
                    .poiKey("")
                    .name("")
                    .content("")
                    .address("")
                    .imageUrl(DEFAULT_IMAGE_PLACEHOLDER)
                    .build();
        }

        JsonNode itemsNode = bodyNode.path("items");
        JsonNode itemNode = itemsNode.path("item");
        if (itemNode.isArray()) {
            itemNode = itemNode.get(0);
        }

        if (itemNode == null || itemNode.isMissingNode() || itemNode.isNull()) {
            return PoiResponse.builder()
                    .poiKey("")
                    .name("")
                    .content("")
                    .address("")
                    .imageUrl(DEFAULT_IMAGE_PLACEHOLDER)
                    .build();
        }

        String poiKey = itemNode.path("contentid").asText("");
        String name = itemNode.path("title").asText("");
        String content = itemNode.path("overview").asText("");
        String address = buildAddress(itemNode);
        String imageUrl = normalizeImageUrl(itemNode.path("firstimage").asText(null));

        return PoiResponse.builder()
                .poiKey(poiKey)
                .name(name)
                .content(content)
                .address(address)
                .imageUrl(imageUrl)
                .build();
    }

    private String buildAddress(JsonNode itemNode) {
        String addr1 = normalizeAddressPart(itemNode.path("addr1").asText(null));
        String addr2 = normalizeAddressPart(itemNode.path("addr2").asText(null));

        if (addr1 == null && addr2 == null) {
            return "";
        }

        if (addr1 == null) {
            return addr2;
        }

        if (addr2 == null) {
            return addr1;
        }

        return addr1 + " " + addr2;
    }

    private String normalizeAddressPart(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeImageUrl(String value) {
        if (value == null) {
            return DEFAULT_IMAGE_PLACEHOLDER;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? DEFAULT_IMAGE_PLACEHOLDER : trimmed;
    }

    private String doubleToString(double value) {
        return Double.toString(value);
    }

    private NearbyResponse emptyResponse() {
        return NearbyResponse.builder()
                .content(Collections.emptyList())
                .page(0)
                .size(0)
                .totalElements(0)
                .build();
    }

}
