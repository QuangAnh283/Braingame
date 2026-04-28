package org.example.quizizz.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "0. Kiểm tra hệ thống", description = "API kiểm tra trạng thái hoạt động của dịch vụ")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Kiểm tra sức khỏe dịch vụ", description = "Trả về trạng thái hoạt động cơ bản của hệ thống")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "quizizz-backend");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ping")
    @Operation(summary = "Kiểm tra kết nối nhanh", description = "API ping đơn giản phục vụ giám sát và kiểm thử nhanh")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}
