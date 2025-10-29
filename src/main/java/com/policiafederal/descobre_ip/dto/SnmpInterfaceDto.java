package com.policiafederal.descobre_ip.dto;

public record SnmpInterfaceDto(
        Integer ifIndex,
        String ifDescr,
        Integer ifType,
        Long ifSpeed,
        String ifPhysAddress,
        Integer ifAdminStatus,
        Integer ifOperStatus,
        String ipAddress,
        String ipNetmask
) {}
