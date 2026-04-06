package com.example.apimock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.apimock.service.ApiConfigService;
import com.example.apimock.dto.*;
import com.example.apimock.entity.*;
import java.util.List;

@SpringBootTest
public class ApiConfigControllerTest {

    @Autowired
    private ApiConfigService service;

    @Test
    public void testCreate() {
        ApiConfigRequest req = new ApiConfigRequest();
        req.setPath("/api/t_test");
        req.setMethod("GET");
        req.setDescription("");
        
        FieldConfigDto field = new FieldConfigDto();
        field.setName("a");
        field.setType(FieldType.STRING);
        field.setValue("a");
        req.setFields(List.of(field));
        
        try {
            service.create(req);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
