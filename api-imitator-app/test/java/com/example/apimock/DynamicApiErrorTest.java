package com.example.apimock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import com.example.apimock.entity.*;
import com.example.apimock.repository.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DynamicApiErrorTest {

    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ApiConfigRepository repo;

    @Test
    public void testDynamicApi() {
        ApiConfig c = new ApiConfig();
        c.setMethod("GET");
        c.setPath("/api/test2");
        repo.save(c);
        
        try {
            ResponseEntity<String> response = restTemplate.getForEntity("/api/test2", String.class);
            System.out.println("STATUS_CODE_xyz: " + response.getStatusCode());
            System.out.println("RESPONSE_BODY_xyz: " + response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
