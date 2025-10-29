package com.policiafederal.descobre_ip.dto;

import jakarta.validation.constraints.NotBlank;


public record DiscoverDtoRequest(
    @NotBlank(message = "O ip inicial deve ser informado")
    String start_address,
    @NotBlank(message = "O ip final deve ser informado")
    String end_address
    )
{
}
