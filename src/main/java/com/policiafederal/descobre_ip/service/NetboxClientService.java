package com.policiafederal.descobre_ip.service;

import com.policiafederal.descobre_ip.dto.SnmpDeviceDto;
import com.policiafederal.descobre_ip.dto.SnmpInterfaceDto;
import com.policiafederal.descobre_ip.service.SnmpService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NetboxClientService {

    private final WebClient webClient;
    @Autowired
    private final SnmpService snmpService;

    private final Map<String, Integer> roleCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> typeCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> siteCache = new ConcurrentHashMap<>();

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
    // ===========================
    // 1. DEVICE
    // ===========================
    private Mono<Boolean> deviceExists(String name) {
        return webClient.get()
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

//    private Mono<Integer> getExistingDeviceId(String name) {
//        return webClient.get()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/api/dcim/devices/")
//                        .queryParam("name", name)
//                        .build())
//                .retrieve()
//                .bodyToMono(Map.class)
//                .map(resp -> {
//                    @SuppressWarnings("unchecked")
//                    List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
//                    return results.isEmpty() ? null : (Integer) results.get(0).get("id");
//                })
//                .flatMap(id -> id != null ? Mono.just(id) : Mono.empty());
//    }

    public Mono<String> createOrUpdateDevice(SnmpDeviceDto deviceDto) {
        return deviceExists(deviceDto.sysName())  // ← CHAMADO AQUI
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
                    return webClient.post()
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

    // ===========================
    // 2. INTERFACE
    // ===========================
    public Mono<String> createOrUpdateInterface(String deviceName, SnmpInterfaceDto iface) {
        return getDeviceIdByName(deviceName)
                .flatMap(deviceId -> {
                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/api/dcim/interfaces/")
                                    .queryParam("device_id", deviceId)
                                    .queryParam("name", iface.ifDescr())
                                    .build())
                            .retrieve()
                            .bodyToMono(Map.class)
                            .flatMap(resp -> {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
                                if (results != null && !results.isEmpty()) {
                                    return Mono.just("Interface já existe: " + iface.ifDescr());
                                }
                                return createInterface(deviceId, iface);
                            });
                });
    }

    private Mono<Integer> getDeviceIdByName(String name) {
        return webClient.get()
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

    private Mono<String> createInterface(Integer deviceId, SnmpInterfaceDto iface) {
        String type = mapInterfaceType(iface.ifType());
        String mac = iface.ifPhysAddress();

        Map<String, Object> body = new HashMap<>();
        body.put("device", deviceId);
        body.put("name", iface.ifDescr());
        if (type != null) body.put("type", type);
        body.put("enabled", iface.ifAdminStatus() == 1);
        if (mac != null && !mac.isBlank()) body.put("mac_address", mac);

        return webClient.post()
                .uri("/api/dcim/interfaces/")
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class).flatMap(error -> {
                            System.err.println("Erro ao criar interface: " + error);
                            return Mono.error(new RuntimeException("Erro ao criar interface: " + error));
                        })
                )
                .bodyToMono(Map.class)
                .map(resp -> "Interface criada: " + iface.ifDescr());
    }


    // ===========================
    // 3. IP ADDRESS
    // ===========================
    public Mono<String> createOrUpdateIpAddress(String ifaceName, String ip, String netmask) {
        if (ip == null || ip.equals("null") || ip.trim().isEmpty()) {
            return Mono.just("IP ignorado (nulo)");
        }

        String cidr = ip + "/" + maskToCidr(netmask);

        return getInterfaceIdByName(ifaceName)
                .flatMap(ifaceId -> {
                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/api/ipam/ip-addresses/")
                                    .queryParam("address", cidr)
                                    .queryParam("interface_id", ifaceId)
                                    .build())
                            .retrieve()
                            .bodyToMono(Map.class)
                            .flatMap(resp -> {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
                                if (results != null && !results.isEmpty()) {
                                    return Mono.just("IP já existe: " + cidr);
                                }
                                return createIpAddress(ifaceId, cidr);
                            });
                });
    }

    private Mono<Integer> getInterfaceIdByName(String ifaceName) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/dcim/interfaces/")
                        .queryParam("name", ifaceName)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
                    return results.isEmpty() ? null : (Integer) results.get(0).get("id");
                })
                .flatMap(id -> id != null ? Mono.just(id) : Mono.error(new RuntimeException("Interface não encontrada: " + ifaceName)));
    }

    private Mono<String> createIpAddress(Integer interfaceId, String address) {
        Map<String, Object> body = Map.of(
                "address", address,
                "assigned_object_type", "dcim.interface",
                "assigned_object_id", interfaceId,
                "status", "active",
                "description", "SNMP Discovery"
        );
        return webClient.post()
                .uri("/api/ipam/ip-addresses/")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> "IP criado: " + address);
    }

    // ===========================
    // AUXILIARES (IDs válidos)
    // ===========================
    private Mono<Integer> getOrCreateRole(String slug) {
        return Mono.justOrEmpty(roleCache.get(slug))
                .switchIfEmpty(
                        webClient.get()
                                .uri("/api/dcim/device-roles/?slug={slug}", slug)
                                .retrieve()
                                .bodyToMono(Map.class)
                                .map(resp -> {
                                    @SuppressWarnings("unchecked")
                                    List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
                                    if (results.isEmpty()) {
                                        throw new RuntimeException("Device Role não encontrado: " + slug);
                                    }
                                    Integer id = (Integer) results.get(0).get("id");
                                    roleCache.put(slug, id);
                                    return id;
                                })
                                .onErrorResume(e -> {
                                    System.err.println("Erro ao buscar role: " + e.getMessage());
                                    return Mono.just(1); // fallback
                                })
                );
    }

    private Mono<Integer> getOrCreateDeviceType(String descr) {
        String model = extractModel(descr);
        return Mono.justOrEmpty(typeCache.get(model))
                .switchIfEmpty(
                        webClient.get()
                                .uri("/api/dcim/device-types/?model={model}", model)
                                .retrieve()
                                .bodyToMono(Map.class)
                                .map(resp -> {
                                    @SuppressWarnings("unchecked")
                                    List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
                                    if (results.isEmpty()) {
                                        throw new RuntimeException("Device Type não encontrado: " + model);
                                    }
                                    Integer id = (Integer) results.get(0).get("id");
                                    typeCache.put(model, id);
                                    return id;
                                })
                                .onErrorResume(e -> Mono.just(1))
                );
    }

    private Mono<Integer> getOrCreateSite(String location) {
        String slug = location.toLowerCase().replace(" ", "-").replaceAll("[^a-z0-9-]", "");
        return Mono.justOrEmpty(siteCache.get(slug))
                .switchIfEmpty(
                        webClient.get()
                                .uri("/api/dcim/sites/?slug={slug}", slug)
                                .retrieve()
                                .bodyToMono(Map.class)
                                .map(resp -> {
                                    @SuppressWarnings("unchecked")
                                    List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
                                    if (results.isEmpty()) {
                                        throw new RuntimeException("Site não encontrado: " + slug);
                                    }
                                    Integer id = (Integer) results.get(0).get("id");
                                    siteCache.put(slug, id);
                                    return id;
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

    private String mapInterfaceType(int ifType) {
        return switch (ifType) {
            case 6 -> "1000base-t";
            case 24 -> "virtual";
            case 53 -> "virtual";
            default -> "other";
        };
    }

    private int maskToCidr(String netmask) {
        if (netmask == null || netmask.equals("null") || netmask.trim().isEmpty()) return 32;
        try {
            String[] parts = netmask.split("\\.");
            long mask = 0;
            for (String p : parts) {
                mask = (mask << 8) | Integer.parseInt(p.trim());
            }
            return Long.bitCount(mask);
        } catch (Exception e) {
            return 32;
        }
    }
}
