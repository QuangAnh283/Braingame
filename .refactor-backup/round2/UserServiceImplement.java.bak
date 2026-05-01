package org.example.quizizz.service.Implement;

import lombok.RequiredArgsConstructor;
import org.example.quizizz.common.constants.MessageCode;
import org.example.quizizz.common.exception.ApiException;
import org.example.quizizz.mapper.UserMapper;
import org.example.quizizz.model.dto.user.CreateUserRequest;
import org.example.quizizz.model.dto.user.UpdateUserRequest;
import org.example.quizizz.model.dto.user.UserResponse;
import org.example.quizizz.model.entity.Role;
import org.example.quizizz.model.entity.User;
import org.example.quizizz.model.entity.UserRole;
import org.example.quizizz.repository.RoleRepository;
import org.example.quizizz.repository.UserRepository;
import org.example.quizizz.repository.UserRoleRepository;
import org.example.quizizz.service.Interface.IFileStorageService;
import org.example.quizizz.service.Interface.IUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImplement implements IUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final IFileStorageService fileStorageService;

    @Override
    public Page<UserResponse> searchUsers(String keyword, Pageable pageable) {
        Page<User> users;
        if (keyword != null && !keyword.trim().isEmpty()) {
            users = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                keyword, keyword, keyword, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return users.map(this::convertToResponse);
    }
    
    private UserResponse convertToResponse(User user) {
        UserResponse response = userMapper.toUserResponse(user);
        // avatarURL đã là presignedUrl, không cần convert
        return response;
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), MessageCode.USER_NOT_FOUND, "User not found"));
        return convertToResponse(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), MessageCode.USER_ALREADY_EXISTS, "Username already exists");
        }
        
        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), MessageCode.USER_ALREADY_EXISTS, "Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setDob(request.getDob());
        user.setAvatarURL(request.getAvatarURL());
        user.setTypeAccount(request.getTypeAccount());
        user.setSystemFlag(request.getSystemFlag() != null ? request.getSystemFlag() : "0");
        user.setEmailVerified(request.getEmailVerified() != null ? request.getEmailVerified() : false);
        user.setOnline(false);

        User savedUser = userRepository.save(user);
        
        // Tự động gán role dựa trên typeAccount
        String roleName = request.getTypeAccount().toUpperCase();
        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), MessageCode.ROLE_NOT_FOUND, "Role " + roleName + " not found"));
        
        UserRole userRole = new UserRole();
        userRole.setUserId(savedUser.getId());
        userRole.setRoleId(role.getId());
        userRoleRepository.save(userRole);
        
        return convertToResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), MessageCode.USER_NOT_FOUND, "User not found"));

        // Update fields if provided
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(), MessageCode.USER_ALREADY_EXISTS, "Username already exists");
            }
            user.setUsername(request.getUsername());
        }
        
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(), MessageCode.USER_ALREADY_EXISTS, "Email already exists");
            }
            user.setEmail(request.getEmail());
        }
        
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        
        if (request.getDob() != null) {
            user.setDob(request.getDob());
        }
        
        if (request.getAvatarURL() != null) {
            user.setAvatarURL(request.getAvatarURL());
        }
        
        if (request.getTypeAccount() != null) {
            user.setTypeAccount(request.getTypeAccount());
            
            // Tự động cập nhật role khi thay đổi typeAccount
            String roleName = request.getTypeAccount().toUpperCase();
            Role role = roleRepository.findByRoleName(roleName)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), MessageCode.ROLE_NOT_FOUND, "Role " + roleName + " not found"));
            
            // Xóa role cũ
            userRoleRepository.deleteByUserId(id);
            
            // Gán role mới
            UserRole userRole = new UserRole();
            userRole.setUserId(id);
            userRole.setRoleId(role.getId());
            userRoleRepository.save(userRole);
        }
        
        if (request.getEmailVerified() != null) {
            user.setEmailVerified(request.getEmailVerified());
        }

        User savedUser = userRepository.save(user);
        return convertToResponse(savedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), MessageCode.USER_NOT_FOUND, "User not found"));
        
        if ("1".equals(user.getSystemFlag())) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), MessageCode.OPERATION_NOT_ALLOWED, "Cannot delete system user");
        }
        
        userRepository.delete(user);
    }
}