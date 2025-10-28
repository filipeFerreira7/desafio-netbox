//package com.policiafederal.descobre_ip.controller;
//
//import com.policiafederal.descobre_ip.dto.PrefixDtoRequest;
//import com.policiafederal.descobre_ip.service.NetboxClientService;
//import jakarta.validation.Valid;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import reactor.core.publisher.Mono;
//
//@RestController
//@RequestMapping("/api/prefixes")
//public class NetboxController {
//
//    @Autowired
//    private final NetboxClientService netboxClientService;
//
//    public NetboxController(NetboxClientService netboxClientService) {
//        this.netboxClientService = netboxClientService;
//    }
//
//    @PostMapping
//    public Mono<ResponseEntity<String>> createPrefix(@Valid @RequestBody PrefixDtoRequest request){
//        return netboxClientService.createPrefix(request.prefix(), request.description())
//                .map(ResponseEntity::ok)
//                .onErrorResume(ex -> Mono.just(ResponseEntity.badRequest()
//                        .body("Erro ao criar prefixo" + ex.getMessage())));
//    }
//
//
//
//}
