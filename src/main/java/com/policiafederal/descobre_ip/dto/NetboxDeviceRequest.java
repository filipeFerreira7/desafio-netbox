package com.policiafederal.descobre_ip.dto;

public record NetboxDeviceRequest (
        String name,
        String device_role,
        String device_type,
        String site,
        String status
) {
}
