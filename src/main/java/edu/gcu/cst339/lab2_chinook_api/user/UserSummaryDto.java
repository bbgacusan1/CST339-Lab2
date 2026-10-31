package edu.gcu.cst339.lab2_chinook_api.user;

public record UserSummaryDto(
        Integer userId,
        String username,
        String role,
        boolean enabled,
        boolean passwordChangeRequired
        ) {

    static UserSummaryDto fromEntity(User user) {
        return new UserSummaryDto(
                user.getUserId(),
                user.getUsername(),
                user.getRole(),
                user.isEnabled(),
                user.isPasswordChangeRequired());
    }
}
