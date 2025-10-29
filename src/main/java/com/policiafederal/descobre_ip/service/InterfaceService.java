package com.policiafederal.descobre_ip.service;

import com.policiafederal.descobre_ip.dto.SnmpInterfaceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InterfaceService {
    @Autowired
    private final DeviceService deviceService;
    @Autowired
    private final NetboxClientService netboxClientService;

    public InterfaceService(DeviceService deviceService, NetboxClientService netboxClientService) {
        this.deviceService = deviceService;
        this.netboxClientService = netboxClientService;
    }

    public Mono<String> createOrUpdateInterface(String deviceName, SnmpInterfaceDto iface) {
        return deviceService.getDeviceIdByName(deviceName)
                .flatMap(deviceId -> {
                    return netboxClientService.webClient.get()
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


    private Mono<String> createInterface(Integer deviceId, SnmpInterfaceDto iface) {
        String type = mapInterfaceType(iface.ifType());
        String mac = iface.ifPhysAddress();

        Map<String, Object> body = new HashMap<>();
        body.put("device", deviceId);
        body.put("name", iface.ifDescr());
        body.put("type", type);
        body.put("enabled", iface.ifAdminStatus() == 1);
        if (mac != null && !mac.isBlank()) body.put("mac_address", mac);

        return netboxClientService.webClient.post()
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

    Mono<Integer> getInterfaceIdByName(String ifaceName) {
        return netboxClientService.webClient.get()
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
    private String mapInterfaceType(int ifType) {
        return switch (ifType) {
            case 6 -> "1000base-t";
            case 24 -> "virtual";
            case 53 -> "virtual";
            default -> "other";
        };
    }

    int maskToCidr(String netmask) {
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
