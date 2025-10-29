package com.policiafederal.descobre_ip.dto;

import java.util.List;

public record SnmpDeviceDto(
        String sysName,
        String sysDescr,
        String sysObjectID,
        String sysUpTime,
        String sysContact,
        String sysLocation,
        List<SnmpInterfaceDto> interfaces
) {
}
