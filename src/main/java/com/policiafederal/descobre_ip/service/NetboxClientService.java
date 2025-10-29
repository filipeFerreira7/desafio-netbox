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

    final WebClient webClient;

    final Map<String, Integer> roleCache = new ConcurrentHashMap<>();
    final Map<String, Integer> typeCache = new ConcurrentHashMap<>();
    final Map<String, Integer> siteCache = new ConcurrentHashMap<>();

    public NetboxClientService(WebClient.Builder webClientBuilder,
                               @Value("https://geto8314.cloud.netboxapp.com") String url,
                               @Value("eda089375bd3086c8818a94d1b70b7001e56e10e") String token) {
        this.webClient = webClientBuilder
                .baseUrl(url)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Token " + token)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

}
