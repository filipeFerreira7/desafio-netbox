package com.policiafederal.descobre_ip;

import com.policiafederal.descobre_ip.service.SnmpService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class DescobreIpApplication implements CommandLineRunner {

	private final SnmpService snmpService;

	public DescobreIpApplication(SnmpService snmpService) {
		this.snmpService = snmpService;
	}

	public static void main(String[] args) {
		SpringApplication.run(DescobreIpApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		Map<String, Object> data = snmpService.getStaticData();
		System.out.println(data);
	}

}
