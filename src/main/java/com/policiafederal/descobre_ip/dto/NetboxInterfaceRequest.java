package com.policiafederal.descobre_ip.dto;

public record NetboxInterfaceRequest(
        String name,
        Integer device,
        String type,
        String mac_address,
        Boolean enabled
) {}

