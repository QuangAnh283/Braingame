package org.example.quizizz.model.dto.profile;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UpdateProfileResponse {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private LocalDate dob;
    private String avatarURL;
    private String typeAccount;
    private LocalDateTime createdAt;
}
