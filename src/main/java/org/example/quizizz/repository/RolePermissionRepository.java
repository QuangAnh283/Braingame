package org.example.quizizz.repository;

import org.example.quizizz.model.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    
    List<RolePermission> findByRoleId(Long roleId);

    // flush + clear bắt buộc để DELETE chạy NGAY tại DB trước khi saveAll() insert,
    // tránh vi phạm UNIQUE(role_id, permission_id) khi update danh sách quyền của role.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM RolePermission rp WHERE rp.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);
    
    @Query("SELECT rp FROM RolePermission rp WHERE rp.roleId = :roleId AND rp.permissionId IN :permissionIds")
    List<RolePermission> findByRoleIdAndPermissionIdIn(@Param("roleId") Long roleId, @Param("permissionIds") Set<Long> permissionIds);
    
    @Query("SELECT rp FROM RolePermission rp WHERE rp.permissionId = :permissionId AND rp.roleId IN :roleIds")
    List<RolePermission> findByPermissionIdAndRoleIdIn(@Param("permissionId") Long permissionId, @Param("roleIds") Set<Long> roleIds);
    
    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.roleId = :roleId AND rp.permissionId IN :permissionIds")
    void deleteByRoleIdAndPermissionIdIn(@Param("roleId") Long roleId, @Param("permissionIds") Set<Long> permissionIds);
    
    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.permissionId = :permissionId AND rp.roleId IN :roleIds")
    void deleteByPermissionIdAndRoleIdIn(@Param("permissionId") Long permissionId, @Param("roleIds") Set<Long> roleIds);
    
    @Query("SELECT DISTINCT ur.userId FROM UserRole ur WHERE ur.roleId = :roleId")
    List<Long> findUserIdsByRoleId(@Param("roleId") Long roleId);
    
    @Query("SELECT DISTINCT ur.userId FROM UserRole ur WHERE ur.roleId IN :roleIds")
    List<Long> findUserIdsByRoleIds(@Param("roleIds") Set<Long> roleIds);
}
