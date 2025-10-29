package com.policiafederal.descobre_ip.dto;

public record NetboxIpAddressRequest(
        String address,
        Integer assigned_object_id,
        String assigned_object_type,
        String status
) {
}
