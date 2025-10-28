package com.policiafederal.descobre_ip.controller;

import com.policiafederal.descobre_ip.dto.DiscoverDtoRequest;
import com.policiafederal.descobre_ip.service.DiscoverService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class DiscoverController {
    @Autowired
    private DiscoverService discoverService;

    @PostMapping("/discover")
    public Mono<List<String>> varrerFaixa(@Valid @RequestBody DiscoverDtoRequest request) throws IOException {
        return discoverService.processarFaixa(request);
    }
}
