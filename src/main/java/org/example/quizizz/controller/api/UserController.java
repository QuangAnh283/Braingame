package org.example.quizizz.controller.api;

import org.example.quizizz.common.config.ApiResponse;
import org.example.quizizz.common.constants.MessageCode;
import org.example.quizizz.model.dto.PageResponse;
import org.example.quizizz.model.dto.user.CreateUserRequest;
import org.example.quizizz.model.dto.user.UpdateUserRequest;
import org.example.quizizz.model.dto.user.UserResponse;
import org.example.quizizz.service.Interface.IAuthService;
import org.example.quizizz.service.Interface.IUserService;
import org.example.quizizz.util.PageableUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "6. Người dùng", description = "API liên quan đến người dùng")
public class UserController {

    private final IAuthService authService;
    private final IUserService userService;

    @Operation(summary = "Tạo mới người dùng", description = "Tạo mới một người dùng trong hệ thống")
    @PostMapping
    @PreAuthorize("hasAuthority('user:manage')")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, response));
    }

    @Operation(summary = "Cập nhật người dùng", description = "Cập nhật thông tin người dùng theo ID")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:manage')")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, response));
    }

    @Operation(summary = "Xóa người dùng", description = "Xóa một người dùng theo ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:manage')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, "User deleted successfully"));
    }

    @Operation(summary = "Đếm số lượng người dùng", description = "Lấy tổng số lượng người dùng trong hệ thống")
    @GetMapping("/count")
    @PreAuthorize("hasAuthority('user:manage')")
    public ResponseEntity<ApiResponse<Long>> count() {
        Long count = authService.countUsers();
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, count));
    }

    @Operation(summary = "Tìm kiếm người dùng", description = "Tìm kiếm và phân trang người dùng")
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('user:manage')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {
        Page<UserResponse> users = userService.searchUsers(keyword, PageableUtil.createPageable(page, size, sort));
        PageResponse<UserResponse> response = PageResponse.of(users);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, response));
    }

    @Operation(summary = "Lấy người dùng theo ID", description = "Lấy thông tin chi tiết người dùng theo ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:manage')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, user));
    }
}
