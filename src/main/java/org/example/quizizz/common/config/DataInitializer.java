package org.example.quizizz.common.config;

import org.example.quizizz.common.constants.RoleCode;
import org.example.quizizz.common.constants.SystemFlag;
import org.example.quizizz.model.entity.*;
import org.example.quizizz.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY_MS = 2000;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting data initialization...");
        
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                initializePermissions();
                initializeRoles();
                initializeUsers();
                assignRolePermissions();
                log.info("Data initialization completed!");
                return;
            } catch (Exception e) {
                log.warn("Initialization attempt {} failed: {}", i + 1, e.getMessage());
                if (i < MAX_RETRY - 1) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        log.error("Data initialization failed after {} retries", MAX_RETRY);
    }

    private void initializePermissions() {
        List<Permission> permissions = Arrays.asList(
                createPermission("user:manage", "Quản lý người dùng và phân quyền"),
                createPermission("user:manage_profile", "Quản lý hồ sơ cá nhân"),
                createPermission("role:manage", "Quản lý vai trò hệ thống"),
                createPermission("permission:manage", "Quản lý quyền hạn"),
                createPermission("question:manage", "Quản lý câu hỏi"),
                createPermission("topic:manage", "Quản lý chủ đề"),
                createPermission("room:manage", "Quản lý phòng chơi"),
                createPermission("room:join", "Tham gia phòng chơi"),
                createPermission("room:kick_player", "Kick người chơi khỏi phòng"),
                createPermission("room:leave", "Rời khỏi phòng"),
                createPermission("room:invite", "Mời người chơi vào phòng"),
                createPermission("game:start", "Bắt đầu trò chơi"),
                createPermission("game:answer", "Trả lời câu hỏi"),
                createPermission("game:view_score", "Xem điểm số"),
                createPermission("rank:view", "Xem bảng xếp hạng")
        );

        permissions.forEach(permission -> {
            if (!permissionRepository.findByPermissionName(permission.getPermissionName()).isPresent()) {
                permission.setSystemFlag(SystemFlag.SYSTEM.getValue());
                permissionRepository.save(permission);
                log.info("Created permission: {}", permission.getPermissionName());
            }
        });
    }

    private void initializeRoles() {
        List<Role> roles = Arrays.asList(
                createRole("PLAYER", "Người chơi - Có thể tham gia game, quản lý profile cá nhân"),
                createRole("HOST", "Chủ phòng - Có thể tạo phòng, quản lý game, kick player"),
                createRole("ADMIN", "Quản trị viên - Toàn quyền quản lý hệ thống")
        );

        roles.forEach(role -> {
            if (!roleRepository.findByRoleName(role.getRoleName()).isPresent()) {
                role.setSystemFlag(SystemFlag.SYSTEM.getValue());
                roleRepository.save(role);
                log.info("Created role: {}", role.getRoleName());
            }
        });
    }

    private void initializeUsers() {
        // Admin user
        if (!userRepository.findByUsername("admin").isPresent()) {
            User admin = createUser("admin", "admin123", "admin@quizizz.com", "System Administrator", RoleCode.ADMIN.name());
            userRepository.save(admin);

            Role adminRole = roleRepository.findByRoleName("ADMIN").get();
            UserRole userRole = new UserRole();
            userRole.setUserId(admin.getId());
            userRole.setRoleId(adminRole.getId());
            userRoleRepository.save(userRole);
            log.info("Created dev admin user: admin");
        }

        // Player user
        if (!userRepository.findByUsername("player").isPresent()) {
            User player = createUser("player", "player123", "player@quizizz.com", "Test Player", RoleCode.PLAYER.name());
            userRepository.save(player);

            Role playerRole = roleRepository.findByRoleName("PLAYER").get();
            UserRole userRole = new UserRole();
            userRole.setUserId(player.getId());
            userRole.setRoleId(playerRole.getId());
            userRoleRepository.save(userRole);
            log.info("Created dev player user: player");
        }
    }

    private void assignRolePermissions() {
        Role playerRole = roleRepository.findByRoleName("PLAYER").get();
        Role hostRole = roleRepository.findByRoleName("HOST").get();
        Role adminRole = roleRepository.findByRoleName("ADMIN").get();

        // PLAYER permissions
        assignPermissionsToRole(playerRole, Arrays.asList(
                "user:manage_profile", "room:join", "room:leave", "game:answer", "game:view_score", "rank:view"
        ));

        // HOST permissions (includes PLAYER + host-specific)
        assignPermissionsToRole(hostRole, Arrays.asList(
                "user:manage_profile", "room:manage", "room:join", "room:leave", "room:kick_player",
                "room:invite", "game:start", "game:answer", "game:view_score", "rank:view"
        ));

        // ADMIN permissions (all permissions)
        List<Permission> allPermissions = permissionRepository.findAll();
        allPermissions.forEach(permission -> {
            if (rolePermissionRepository.findByRoleIdAndPermissionIdIn(adminRole.getId(),
                    java.util.Set.of(permission.getId())).isEmpty()) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleId(adminRole.getId());
                rolePermission.setPermissionId(permission.getId());
                rolePermissionRepository.save(rolePermission);
            }
        });

        log.info("Assigned permissions to all roles");
    }

    private Permission createPermission(String name, String description) {
        Permission permission = new Permission();
        permission.setPermissionName(name);
        permission.setDescription(description);
        return permission;
    }

    private Role createRole(String name, String description) {
        Role role = new Role();
        role.setRoleName(name);
        role.setDescription(description);
        return role;
    }

    private User createUser(String username, String password, String email, String fullName, String typeAccount) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setFullName(fullName);
        user.setTypeAccount(typeAccount);
        user.setOnline(false);
        user.setDeleted(false);
        user.setSystemFlag(SystemFlag.SYSTEM.getValue());
        user.setDob(LocalDate.of(1990, 1, 1));
        return user;
    }

    private void assignPermissionsToRole(Role role, List<String> permissionNames) {
        permissionNames.forEach(permissionName -> {
            permissionRepository.findByPermissionName(permissionName).ifPresent(permission -> {
                if (rolePermissionRepository.findByRoleIdAndPermissionIdIn(role.getId(),
                        java.util.Set.of(permission.getId())).isEmpty()) {
                    RolePermission rolePermission = new RolePermission();
                    rolePermission.setRoleId(role.getId());
                    rolePermission.setPermissionId(permission.getId());
                    rolePermissionRepository.save(rolePermission);
                }
            });
        });
    }
}
