package com.policiafederal.descobre_ip.service;

import com.policiafederal.descobre_ip.dto.DiscoverDtoRequest;
import com.policiafederal.descobre_ip.dto.PrefixDtoRequest;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class NetboxClientService {

    private final WebClient webClient;
    @Autowired
    private final SnmpService snmpService;

    public NetboxClientService(WebClient.Builder webClientBuilder, SnmpService snmpService,
                               @Value("https://geto8314.cloud.netboxapp.com") String url,
                               @Value("eda089375bd3086c8818a94d1b70b7001e56e10e") String token) {
        this.webClient = webClientBuilder
                .baseUrl(url)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Token " + token)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.snmpService = snmpService;
    }

    public Mono<String> createPrefix(String prefix, String description) {
        var body = new PrefixDtoRequest(prefix, description);

        return webClient.post()
                .uri("/api/ipam/prefixes/")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> createRange(String start, String end, String description, String vrf, boolean isFilled, boolean isUsed) {
        Map<String, Object> body = new HashMap<>();
        body.put("start_address", start);
        body.put("end_address", end);
        body.put("description", description);
        body.put("vrf", vrf);
        body.put("status", "active");
        body.put("is_filled", isFilled);
        body.put("is_used", isUsed);

        return webClient.post()
                .uri("/api/ipam/ip-ranges/")
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp ->
                        resp.bodyToMono(String.class).map(bodyStr ->
                                new BadRequestException("400 Bad Request from Netbox: " + bodyStr)
                        )
                )
                .onStatus(HttpStatusCode::is5xxServerError, resp ->
                        Mono.error(new RuntimeException("Netbox server error"))
                )
                .bodyToMono(String.class);
    }
    public Mono<Boolean> checkRangeExists(String start, String end) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/ipam/ip-ranges/")
                        .queryParam("start_address", start)
                        .queryParam("end_address", end)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Integer count = (Integer) response.get("count");
                    return count != null && count > 0;
                })
                .onErrorResume(e -> {

                    return Mono.just(false);
                });
    }

}

