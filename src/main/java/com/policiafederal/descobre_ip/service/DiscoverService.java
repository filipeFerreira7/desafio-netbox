package com.policiafederal.descobre_ip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.policiafederal.descobre_ip.dto.DiscoverDtoRequest;
import com.policiafederal.descobre_ip.dto.SnmpDeviceDto;
import com.policiafederal.descobre_ip.dto.SnmpInterfaceDto;;
import com.policiafederal.descobre_ip.infra.exception.RangeIpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class DiscoverService {
    @Autowired
    private final SnmpService snmpService;
    @Autowired
    private final IpAddressService ipAddressService;

    @Autowired
    private final InterfaceService interfaceService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private DeviceService deviceService;


    public DiscoverService(SnmpService snmpService, IpAddressService ipAddressService, InterfaceService interfaceService) {
        this.snmpService = snmpService;
        this.ipAddressService = ipAddressService;
        this.interfaceService = interfaceService;
    }

    public Mono<List<String>> processarFaixa(DiscoverDtoRequest request) {
        return Mono.fromCallable(() -> gerarRangeIps(request.start_address(), request.end_address()))
                .flatMapMany(Flux::fromIterable)
                .flatMap(ip -> Mono.fromCallable(snmpService::getStaticData)
                        .map(data -> (Map<String, Object>) data.get(ip))
                        .filter(Objects::nonNull)
                        .map(deviceData -> {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> rawInterfaces = (List<Map<String, Object>>) deviceData.get("interfaces");

                            List<SnmpInterfaceDto> interfaces = rawInterfaces.stream()
                                    .map(map -> objectMapper.convertValue(map, SnmpInterfaceDto.class))
                                    .toList();

                            return new SnmpDeviceDto(
                                    (String) deviceData.get("sysName"),
                                    (String) deviceData.get("sysDescr"),
                                    (String) deviceData.get("sysObjectID"),
                                    (String) deviceData.get("sysUpTime"),
                                    (String) deviceData.get("sysContact"),
                                    (String) deviceData.get("sysLocation"),
                                    interfaces
                            );
                        })
                        .flatMap(device -> deviceService.createOrUpdateDevice(device)
                                .flatMap(deviceMsg -> Flux.fromIterable(device.interfaces())
                                        .flatMap(iface -> interfaceService.createOrUpdateInterface(device.sysName(), iface)
                                                .flatMap(ifaceMsg -> {
                                                    if (iface.ipAddress() != null && !iface.ipAddress().equals("null")) {
                                                        return ipAddressService.createOrUpdateIpAddress(iface.ifDescr(), iface.ipAddress(), iface.ipNetmask())
                                                                .map(ipMsg -> deviceMsg + " | " + ifaceMsg + " | " + ipMsg);
                                                    }
                                                    return Mono.just(deviceMsg + " | " + ifaceMsg + " | IP ignorado");
                                                })
                                        )
                                        .collectList()
                                        .map(list -> String.join("\n", list))
                                )
                        )
                        .defaultIfEmpty("Nenhum dado para IP: " + ip)
                )
                .collectList();
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
            throw new RangeIpException("Erro ao gerar faixa de IP: " + e.getMessage());
        }
        return lista;
    }
    private long ipParaLong(String ip) throws Exception {
        InetAddress inet = InetAddress.getByName(ip);
        ByteBuffer bb = ByteBuffer.wrap(inet.getAddress());
        return Integer.toUnsignedLong(bb.getInt());
    }

    private String longParaIp(long valor) {
        return String.format("%d.%d.%d.%d",
                (valor >> 24) & 0xFF,
                (valor >> 16) & 0xFF,
                (valor >> 8) & 0xFF,
                valor & 0xFF);
    }
}
