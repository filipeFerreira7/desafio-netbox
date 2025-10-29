package com.policiafederal.descobre_ip.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class IpAddressService {
    @Autowired
    private InterfaceService interfaceService;
    @Autowired
    private NetboxClientService netboxClientService;

    public Mono<String> createOrUpdateIpAddress(String ifaceName, String ip, String netmask) {
        if (ip == null || ip.equals("null") || ip.trim().isEmpty()) {
            return Mono.just("IP ignorado (nulo)");
        }

        String cidr = ip + "/" + interfaceService.maskToCidr(netmask);

        return interfaceService.getInterfaceIdByName(ifaceName)
                .flatMap(ifaceId -> {
                    return netboxClientService.webClient.get()
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


    private Mono<String> createIpAddress(Integer interfaceId, String address) {
        Map<String, Object> body = Map.of(
                "address", address,
                "assigned_object_type", "dcim.interface",
                "assigned_object_id", interfaceId,
                "status", "active",
                "description", "SNMP Discovery"
        );
        return netboxClientService.webClient.post()
                .uri("/api/ipam/ip-addresses/")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> "IP criado: " + address);
    }
}
