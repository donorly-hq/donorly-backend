package org.donorly.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.ForgotPasswordRequest;
import org.donorly.backend.dto.LoginRequest;
import org.donorly.backend.dto.LoginResponse;
import org.donorly.backend.dto.MeResponse;
import org.donorly.backend.dto.OrgChoice;
import org.donorly.backend.dto.ResetPasswordRequest;
import org.donorly.backend.dto.SelectOrgRequest;
import org.donorly.backend.dto.SwitchOrgRequest;
import org.donorly.backend.dto.VerifyOtpRequest;
import org.donorly.backend.service.AuthService;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<LoginResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request.challengeId(), request.code()));
    }

    /** Finishes login for users who belong to multiple organizations. */
    @PostMapping("/select-org")
    public ResponseEntity<LoginResponse> selectOrg(@Valid @RequestBody SelectOrgRequest request) {
        return ResponseEntity.ok(
                authService.selectOrganization(request.challengeId(), request.organizationId()));
    }

    /** Moves the current session to another org the user belongs to; returns a new token. */
    @PostMapping("/switch-org")
    public ResponseEntity<LoginResponse> switchOrg(
            Authentication authentication,
            @Valid @RequestBody SwitchOrgRequest request) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID userId)) {
            throw new org.donorly.backend.common.BadRequestException("Not authenticated");
        }
        String jti = authentication.getCredentials() instanceof String s ? s : null;
        return ResponseEntity.ok(authService.switchOrganization(userId, jti, request.organizationId()));
    }

    /** All organizations the current user is an active member of. */
    @GetMapping("/my-organizations")
    public List<OrgChoice> myOrganizations(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID userId)) {
            throw new org.donorly.backend.common.BadRequestException("Not authenticated");
        }
        return authService.myOrganizations(userId);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        // Always the same response — never reveal whether the account exists
        return ResponseEntity.ok(Map.of(
                "message", "If an account exists for that email, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password updated. You can now sign in."));
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID userId)) {
            throw new org.donorly.backend.common.BadRequestException("Not authenticated");
        }
        return authService.getCurrentUser(userId, TenantContext.getOrganizationId());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UUID userId) {
            String jti = authentication.getCredentials() instanceof String s ? s : null;
            authService.logout(userId, jti);
        }
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }
}
