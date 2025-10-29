package com.policiafederal.descobre_ip.infra.exception;

public class RangeIpException extends RuntimeException {
    public RangeIpException(String message) {
        super(message);
    }
}