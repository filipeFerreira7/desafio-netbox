package com.policiafederal.descobre_ip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DiscoverDtoRequest(
        @NotBlank(message = "O endereço inicial é obrigatório")
        @Pattern(regexp = "^([0-9]{1,3}\\.){3}[0-9]{1,3}$",
                message = "Formato IPv4 inválido: use ex: 192.168.1.1")
        String start_address,

        @NotBlank(message = "O endereço final é obrigatório")
        @Pattern(regexp = "^([0-9]{1,3}\\.){3}[0-9]{1,3}$",
                message = "Formato IPv4 inválido: use ex: 192.168.1.1")
        String end_address
) {}
