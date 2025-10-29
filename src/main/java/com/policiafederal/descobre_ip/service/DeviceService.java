package com.policiafederal.descobre_ip.service;

import com.policiafederal.descobre_ip.dto.SnmpDeviceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class DeviceService {

    @Autowired
    private NetboxClientService netboxClientService;

    private Mono<Boolean> deviceExists(String name) {
        return netboxClientService.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/dcim/devices/")
                        .queryParam("name", name)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
                    return results != null && !results.isEmpty();
                })
                .onErrorReturn(false);
    }

    Mono<Integer> getDeviceIdByName(String name) {
        return netboxClientService.webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/dcim/devices/").queryParam("name", name).build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
                    return results.isEmpty() ? null : (Integer) results.get(0).get("id");
                })
                .flatMap(id -> id != null ? Mono.just(id) : Mono.error(new RuntimeException("Device não encontrado: " + name)));
    }


    public Mono<String> createOrUpdateDevice(SnmpDeviceDto deviceDto) {
        return deviceExists(deviceDto.sysName())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.just("Device já existe: " + deviceDto.sysName());
                    }
                    return createDeviceWithValidIds(deviceDto);
                });
    }

    private Mono<String> createDeviceWithValidIds(SnmpDeviceDto device) {
        return Mono.zip(
                        getOrCreateRole("router"),
                        getOrCreateDeviceType(device.sysDescr()),
                        getOrCreateSite(device.sysLocation())
                )
                .flatMap(tuple -> {
                    Map<String, Object> body = Map.of(
                            "name", device.sysName(),
                            "role", tuple.getT1(),
                            "device_type", tuple.getT2(),
                            "site", tuple.getT3(),
                            "status", "active"
                    );
                    return netboxClientService.webClient.post()
                            .uri("/api/dcim/devices/")
                            .bodyValue(body)
                            .retrieve()
                            .onStatus(HttpStatusCode::is4xxClientError, resp ->
                                    resp.bodyToMono(String.class)
                                            .flatMap(error -> Mono.error(new RuntimeException("Erro 400: " + error)))
                            )
                            .bodyToMono(Map.class)
                            .map(resp -> "Device criado: " + device.sysName() + " (ID: " + resp.get("id") + ")");
                });
    }

    private Mono<Integer> getOrCreateRole(String slug) {
        return Mono.justOrEmpty(netboxClientService.roleCache.get(slug))
                .switchIfEmpty(
                       netboxClientService.webClient.get()
                                .uri("/api/dcim/device-roles/?slug={slug}", slug)
                                .retrieve()
                                .bodyToMono(Map.class)
                                .<Integer>handle((resp, sink) -> {
                                    @SuppressWarnings("unchecked")
                                    List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
                                    if (results.isEmpty()) {
                                        sink.error(new RuntimeException("Device Role não encontrado: " + slug));
                                        return;
                                    }
                                    Integer id = (Integer) results.get(0).get("id");
                                    netboxClientService.roleCache.put(slug, id);
                                    sink.next(id);
                                })
                                .onErrorResume(e -> {
                                    System.err.println("Erro ao buscar role: " + e.getMessage());
                                    return Mono.just(1); // fallback
                                })
                );
    }

    private Mono<Integer> getOrCreateDeviceType(String descr) {
        String model = extractModel(descr);
        return Mono.justOrEmpty(netboxClientService.typeCache.get(model))
                .switchIfEmpty(
                        netboxClientService.webClient.get()
                                .uri("/api/dcim/device-types/?model={model}", model)
                                .retrieve()
                                .bodyToMono(Map.class)
                                .<Integer>handle((resp, sink) -> {
                                    @SuppressWarnings("unchecked")
                                    List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
                                    if (results.isEmpty()) {
                                        sink.error(new RuntimeException("Device Type não encontrado: " + model));
                                        return;
                                    }
                                    Integer id = (Integer) results.get(0).get("id");
                                    netboxClientService.typeCache.put(model, id);
                                    sink.next(id);
                                })
                                .onErrorResume(e -> Mono.just(1))
                );
    }

    private Mono<Integer> getOrCreateSite(String location) {
        String slug = location.toLowerCase().replace(" ", "-").replaceAll("[^a-z0-9-]", "");
        return Mono.justOrEmpty(netboxClientService.siteCache.get(slug))
                .switchIfEmpty(
                        netboxClientService.webClient.get()
                                .uri("/api/dcim/sites/?slug={slug}", slug)
                                .retrieve()
                                .bodyToMono(Map.class)
                                .<Integer>handle((resp, sink) -> {
                                    @SuppressWarnings("unchecked")
                                    List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
                                    if (results.isEmpty()) {
                                        sink.error(new RuntimeException("Site não encontrado: " + slug));
                                        return;
                                    }
                                    Integer id = (Integer) results.get(0).get("id");
                                    netboxClientService.siteCache.put(slug, id);
                                    sink.next(id);
                                })
                                .onErrorResume(e -> Mono.just(1))
                );
    }

    private String extractModel(String descr) {
        if (descr == null) return "generic";
        if (descr.contains("Cisco")) return "cisco-generic";
        if (descr.contains("Juniper")) return "juniper-ex4300";
        if (descr.contains("Arista")) return "arista-7280";
        if (descr.contains("HP")) return "hp-2530";
        return "generic-switch";
    }
}
