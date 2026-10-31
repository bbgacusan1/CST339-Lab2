package edu.gcu.cst339.lab2_chinook_api.user;

import jakarta.validation.constraints.NotBlank;

public class UserUpdateDto {

    @NotBlank
    private String role;
    private boolean enabled;
    private boolean passwordChangeRequired;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPasswordChangeRequired() {
        return passwordChangeRequired;
    }

    public void setPasswordChangeRequired(boolean passwordChangeRequired) {
        this.passwordChangeRequired = passwordChangeRequired;
    }
}
