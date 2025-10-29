package com.policiafederal.descobre_ip.service;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class SnmpService {
    public Map<String, Object> getStaticData() throws IOException {
        ClassPathResource staticDataResource = new ClassPathResource("simulated_snmp_data.json");
        String dataString = IOUtils.toString(staticDataResource.getInputStream(), StandardCharsets.UTF_8);

       var obj = new JSONObject(dataString).toMap();
        System.out.println(obj);
       return obj;


    }
}
