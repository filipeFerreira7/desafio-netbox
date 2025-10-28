package com.policiafederal.descobre_ip.service;

import com.policiafederal.descobre_ip.dto.DiscoverDtoRequest;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DiscoverService {
    @Autowired
    private final SnmpService snmpService;
    @Autowired
    private final NetboxClientService netboxClientService;


    public DiscoverService(SnmpService snmpService, NetboxClientService netboxClientService) {
        this.snmpService = snmpService;
        this.netboxClientService = netboxClientService;
    }

    public Mono<List<String>> processarFaixa(DiscoverDtoRequest request) throws IOException {
        Map<String, Object> snmpData = snmpService.getStaticData();
        List<String> ips = gerarRangeIps(request.start_address(),request.end_address());

        List<String> existentesNoJson = ips.stream().filter(snmpData::containsKey)
                .toList();

        if(existentesNoJson.isEmpty()) {
            return Mono.just(List.of("Erro 400: Nenhum IP da faixa existe no JSON"));
        }
        return netboxClientService.checkRangeExists(request.start_address(), request.end_address())
                .flatMap(existe -> {
                    if (existe) {
                        List<String> msg = ips.stream()
                                .map(ip -> "IP Existente no NetBox: " + ip)
                                .toList();
                        return Mono.just(msg);
                    }

                    return netboxClientService.createRange(
                                    request.start_address(),
                                    request.end_address(),
                                    request.description(),
                                    request.vrf(),
                                    request.is_filled(),
                                    request.is_used()
                            ).map(resp -> List.of("Faixa de IP adicionada: " + request.start_address() + " - " + request.end_address()))
                            .onErrorResume(BadRequestException.class, e ->
                                    Mono.just(List.of("Erro ao adicionar faixa: " + e.getMessage()))
                            )
                            .onErrorResume(Exception.class, e ->
                                    Mono.just(List.of("Erro inesperado: " + e.getMessage()))
                            );
                });
    }

    private List<String> gerarRangeIps(String start_address, String ipFim) {
        List<String> lista = new ArrayList<>();

        try{
            long incio = ipParaLong(start_address);
            long fim = ipParaLong(ipFim);

            for(long i = incio; i <= fim; i++){
                lista.add(longParaIp(i));
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar faixa de IPS" + e.getMessage());
        }
        return lista;
    }
    private long ipParaLong(String ip) throws Exception {
        InetAddress inet = InetAddress.getByName(ip);
        ByteBuffer bb = ByteBuffer.wrap(inet.getAddress());
        return Integer.toUnsignedLong(bb.getInt());
    }

    private String longParaIp(long valor) throws Exception {
        return String.format("%d.%d.%d.%d",
                (valor >> 24) & 0xFF,
                (valor >> 16) & 0xFF,
                (valor >> 8) & 0xFF,
                valor & 0xFF);
    }
}
