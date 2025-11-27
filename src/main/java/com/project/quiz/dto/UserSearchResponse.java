package com.project.quiz.dto;

import com.project.quiz.domain.User;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchResponse {
    private Long id;                    
    private Long userId;                
    private String email;
    private String username;
    private String profileImage;
    private String friendshipStatus;

    /**
     * User만으로 변환 (User ID 기준)
     */
    public static UserSearchResponse from(User user) {
        return UserSearchResponse.builder()
                .id(user.getId())
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUserProfile() != null ? user.getUserProfile().getUsername() : null)
                .profileImage(user.getUserProfile() != null ? user.getUserProfile().getProfileImage() : null)
                .friendshipStatus(null)
                .build();
    }

    /**
     * User + 친구 상태로 변환
     */
    public static UserSearchResponse from(User user, String friendshipStatus) {
        return UserSearchResponse.builder()
                .id(user.getId())
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUserProfile() != null ? user.getUserProfile().getUsername() : null)
                .profileImage(user.getUserProfile() != null ? user.getUserProfile().getProfileImage() : null)
                .friendshipStatus(friendshipStatus)
                .build();
    }

    /**
     * ⭐ User + Friendship ID로 변환 (받은 요청용)
     * id = Friendship ID (수락/거절 버튼용)
     * userId = 요청 보낸 사람의 User ID
     */
    public static UserSearchResponse fromFriendship(User user, Long friendshipId, String friendshipStatus) {
        return UserSearchResponse.builder()
                .id(friendshipId)           // Friendship ID (중요!)
                .userId(user.getId())       // User ID
                .email(user.getEmail())
                .username(user.getUserProfile() != null ? user.getUserProfile().getUsername() : null)
                .profileImage(user.getUserProfile() != null ? user.getUserProfile().getProfileImage() : null)
                .friendshipStatus(friendshipStatus)
                .build();
    }
}
