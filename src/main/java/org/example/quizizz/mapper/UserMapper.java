package org.example.quizizz.mapper;

import org.example.quizizz.model.dto.authentication.LoginResponse;
import org.example.quizizz.model.dto.authentication.RegisterRequest;
import org.example.quizizz.model.dto.authentication.RegisterResponse;
import org.example.quizizz.model.dto.user.UserResponse;
import org.example.quizizz.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "typeAccount", expression = "java(org.example.quizizz.common.constants.RoleCode.PLAYER.name())")
    @Mapping(target = "online", constant = "false")
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDelete", ignore = true)
    @Mapping(target = "avatarURL", ignore = true)
    @Mapping(target = "systemFlag", ignore = true)
    @Mapping(target = "lastOnlineTime", ignore = true)
    User toEntity(RegisterRequest request);

    @Mapping(target = "userId", source = "id")
    RegisterResponse toRegisterResponse(User user);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "refreshToken", source = "refreshToken")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "typeAccount", source = "user.typeAccount")
    LoginResponse toLoginResponse(User user, String accessToken, String refreshToken);

    UserResponse toUserResponse(User user);
}
