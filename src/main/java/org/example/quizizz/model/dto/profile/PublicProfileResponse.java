package org.example.quizizz.model.dto.profile;

import lombok.Data;

@Data
public class PublicProfileResponse {
    private Long id;
    private String username;
    private String fullName;
    private String avatarURL;
}
