package org.example.quizizz.repository;

import org.example.quizizz.model.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    // flush + clear bắt buộc để DELETE chạy NGAY tại DB trước khi save() insert,
    // tránh vi phạm UNIQUE(user_id, role_id) khi admin cập nhật role cho user.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM UserRole ur WHERE ur.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    List<UserRole> findByUserId(Long userId);
}
