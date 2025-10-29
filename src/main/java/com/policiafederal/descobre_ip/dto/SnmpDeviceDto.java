package com.policiafederal.descobre_ip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SnmpDeviceDto(
        @NotBlank(message = "O nome do sistema (sysName) deve ser informado")
        String sysName,

        String sysDescr,

        @NotBlank(message = "O ID do sistema (sysObjectID) deve ser informado")
        String sysObjectID,

        String sysUpTime,

        String sysContact,

        String sysLocation,

        @NotNull(message = "A lista de interfaces não pode ser nula")
        @NotEmpty(message = "O dispositivo deve ter pelo menos uma interface")
        List<SnmpInterfaceDto> interfaces
) {}