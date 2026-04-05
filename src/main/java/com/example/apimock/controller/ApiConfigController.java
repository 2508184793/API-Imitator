package com.example.apimock.controller;

import com.example.apimock.dto.ApiConfigRequest;
import com.example.apimock.dto.ApiConfigResponse;
import com.example.apimock.service.ApiConfigService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configs")
public class ApiConfigController {

    private final ApiConfigService apiConfigService;

    public ApiConfigController(ApiConfigService apiConfigService) {
        this.apiConfigService = apiConfigService;
    }

    @GetMapping
    public ResponseEntity<List<ApiConfigResponse>> findAll() {
        return ResponseEntity.ok(apiConfigService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiConfigResponse> findById(@PathVariable Long id) {
        return apiConfigService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiConfigResponse> create(@Valid @RequestBody ApiConfigRequest request) {
        ApiConfigResponse response = apiConfigService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiConfigResponse> update(@PathVariable Long id, @Valid @RequestBody ApiConfigRequest request) {
        return apiConfigService.update(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (apiConfigService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
