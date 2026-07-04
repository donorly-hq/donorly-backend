package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.dto.LoginRequest;
import org.donorly.backend.dto.LoginResponse;
import org.donorly.backend.dto.MeResponse;
import org.donorly.backend.model.AuthToken;
import org.donorly.backend.model.Organization;
import org.donorly.backend.model.OrganizationMembership;
import org.donorly.backend.model.Role;
import org.donorly.backend.model.User;
import org.donorly.backend.model.UserSession;
import org.donorly.backend.repository.AuthTokenRepository;
import org.donorly.backend.repository.OrganizationMembershipRepository;
import org.donorly.backend.repository.OrganizationRepository;
import org.donorly.backend.repository.RoleRepository;
import org.donorly.backend.repository.UserRepository;
import org.donorly.backend.repository.UserSessionRepository;
import org.donorly.backend.security.JwtUtil;
import org.donorly.backend.security.LoginRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final Duration SESSION_TTL = Duration.ofHours(24);
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(60);
    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final int OTP_MAX_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final UserSessionRepository sessionRepository;
    private final AuthTokenRepository authTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final OrgLogoService orgLogoService;
    private final EmailService emailService;
    private final LoginRateLimiter rateLimiter;

    @Value("${donorly.mail.app-base-url}")
    private String appBaseUrl;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String limiterKey = "login:" + request.email();
        if (rateLimiter.isBlocked(limiterKey)) {
            throw new BadRequestException("Too many failed attempts. Try again in "
                    + rateLimiter.minutesUntilUnblocked(limiterKey) + " minute(s).");
        }

        User user = userRepository.findByEmailIgnoreCase(request.email()).orElse(null);
        if (user == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            rateLimiter.recordFailure(limiterKey);
            throw new BadRequestException("Invalid credentials");
        }
        if (!"active".equals(user.getStatus())) {
            throw new BadRequestException("Account is not active");
        }
        rateLimiter.reset(limiterKey);

        ResolvedContext ctx = resolveContext(user, request.organizationSlug());

        // Platform admins carry the keys to every org — require an emailed one-time code.
        if (user.isPlatformAdmin()) {
            return startOtpChallenge(user, request.organizationSlug());
        }

        return completeLogin(user, ctx);
    }

    @Transactional
    public LoginResponse verifyOtp(String challengeId, String code) {
        AuthToken challenge = authTokenRepository
                .findByTokenAndPurpose(challengeId, AuthToken.PURPOSE_LOGIN_OTP)
                .orElseThrow(() -> new BadRequestException("Invalid or expired code"));

        if (challenge.getUsedAt() != null || challenge.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Invalid or expired code");
        }
        if (challenge.getAttempts() >= OTP_MAX_ATTEMPTS) {
            throw new BadRequestException("Too many attempts. Sign in again to get a new code.");
        }

        challenge.setAttempts(challenge.getAttempts() + 1);
        authTokenRepository.save(challenge);

        if (!challenge.getCode().equals(code.trim())) {
            throw new BadRequestException("Incorrect code");
        }

        challenge.setUsedAt(Instant.now());
        authTokenRepository.save(challenge);

        User user = userRepository.findById(challenge.getUserId())
                .orElseThrow(() -> new BadRequestException("Invalid account"));
        if (!"active".equals(user.getStatus())) {
            throw new BadRequestException("Account is not active");
        }

        ResolvedContext ctx = resolveContext(user, challenge.getContext());
        return completeLogin(user, ctx);
    }

    @Transactional
    public void forgotPassword(String email) {
        String limiterKey = "forgot:" + email;
        if (rateLimiter.isBlocked(limiterKey)) {
            return; // silently drop — do not reveal lockout state
        }
        rateLimiter.recordFailure(limiterKey);

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !"active".equals(user.getStatus()) || user.getPasswordHash() == null) {
            return; // never reveal whether the account exists
        }

        authTokenRepository.deleteByUserIdAndPurpose(user.getId(), AuthToken.PURPOSE_PASSWORD_RESET);

        AuthToken token = new AuthToken();
        token.setUserId(user.getId());
        token.setToken(randomToken());
        token.setPurpose(AuthToken.PURPOSE_PASSWORD_RESET);
        token.setExpiresAt(Instant.now().plus(RESET_TOKEN_TTL));
        authTokenRepository.save(token);

        String link = appBaseUrl + "/reset-password/" + token.getToken();
        emailService.sendHtml(user.getEmail(), "Reset your Donorly password", """
                <p>Hi %s,</p>
                <p>We received a request to reset your Donorly password. Click the link below to choose a new one. \
                This link expires in 60 minutes.</p>
                <p><a href="%s">Reset my password</a></p>
                <p>If you didn't request this, you can safely ignore this email — your password is unchanged.</p>
                """.formatted(user.getFullName(), link));
        log.info("Password reset link issued for {}", user.getEmail());
    }

    @Transactional
    public void resetPassword(String tokenValue, String newPassword) {
        AuthToken token = authTokenRepository
                .findByTokenAndPurpose(tokenValue, AuthToken.PURPOSE_PASSWORD_RESET)
                .orElseThrow(() -> new BadRequestException("Reset link is invalid or has expired"));

        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Reset link is invalid or has expired");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BadRequestException("Invalid account"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        authTokenRepository.save(token);

        // Force re-login everywhere with the new password
        sessionRepository.deleteByUserId(user.getId());
        log.info("Password reset completed for {}", user.getEmail());
    }

    /** Refreshes org branding and role info for the current JWT context. */
    @Transactional(readOnly = true)
    public MeResponse getCurrentUser(UUID userId, UUID organizationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Invalid account"));
        if (!"active".equals(user.getStatus())) {
            throw new BadRequestException("Account is not active");
        }

        Organization org = null;
        Role role = null;

        if (organizationId != null) {
            org = organizationRepository.findById(organizationId).orElse(null);
            OrganizationMembership membership = membershipRepository
                    .findByOrganizationIdAndUserId(organizationId, userId)
                    .orElse(null);
            if (membership != null && "active".equals(membership.getStatus())) {
                role = roleRepository.findById(membership.getRoleId()).orElse(null);
            } else if (!user.isPlatformAdmin()) {
                org = null;
            }
        }

        SessionInfo info = buildSessionInfo(user, org, role);
        return new MeResponse(
                info.userId(),
                info.fullName(),
                info.platformAdmin(),
                info.organizationId(),
                info.organizationName(),
                info.organizationPrimaryColor(),
                info.organizationLogo(),
                info.roleCode(),
                info.permissions()
        );
    }

    /** Ends only the session used to make this call; other devices stay signed in. */
    @Transactional
    public void logout(UUID userId, String jti) {
        if (jti != null) {
            sessionRepository.deleteById(jti);
        }
    }

    // ── internals ─────────────────────────────────────────────────────────

    private record ResolvedContext(Organization org, Role role) {}

    private ResolvedContext resolveContext(User user, String organizationSlug) {
        Organization org = null;
        OrganizationMembership membership = null;
        Role role = null;

        List<OrganizationMembership> memberships = membershipRepository.findByUserId(user.getId());

        if (organizationSlug != null && !organizationSlug.isBlank()) {
            org = organizationRepository.findBySlug(organizationSlug)
                    .orElseThrow(() -> new BadRequestException("Unknown organization"));
            final UUID orgId = org.getId();
            membership = memberships.stream()
                    .filter(m -> m.getOrganizationId().equals(orgId) && "active".equals(m.getStatus()))
                    .findFirst().orElse(null);
        } else {
            membership = memberships.stream()
                    .filter(m -> "active".equals(m.getStatus()))
                    .findFirst().orElse(null);
            if (membership != null) {
                org = organizationRepository.findById(membership.getOrganizationId()).orElse(null);
            }
        }

        if (!user.isPlatformAdmin() && membership == null) {
            throw new BadRequestException("No active organization membership");
        }
        if (membership != null) {
            role = roleRepository.findById(membership.getRoleId()).orElse(null);
        }
        return new ResolvedContext(org, role);
    }

    private LoginResponse startOtpChallenge(User user, String organizationSlug) {
        AuthToken challenge = new AuthToken();
        challenge.setUserId(user.getId());
        challenge.setToken(randomToken());
        challenge.setPurpose(AuthToken.PURPOSE_LOGIN_OTP);
        challenge.setCode(randomOtpCode());
        challenge.setContext(organizationSlug);
        challenge.setExpiresAt(Instant.now().plus(OTP_TTL));
        authTokenRepository.save(challenge);

        emailService.sendHtml(user.getEmail(), "Your Donorly sign-in code", """
                <p>Hi %s,</p>
                <p>Your one-time sign-in code is:</p>
                <p style="font-size:28px;font-weight:bold;letter-spacing:6px">%s</p>
                <p>It expires in 10 minutes. If you didn't try to sign in, change your password immediately.</p>
                """.formatted(user.getFullName(), challenge.getCode()));
        log.info("Login OTP issued for platform admin {}", user.getEmail());

        return LoginResponse.otpChallenge(challenge.getToken());
    }

    private LoginResponse completeLogin(User user, ResolvedContext ctx) {
        String jti = UUID.randomUUID().toString();
        UUID orgId = ctx.org() != null ? ctx.org().getId() : null;
        String token = jwtUtil.generateToken(user.getId(), orgId, user.isPlatformAdmin(), jti);

        // Register this session and sweep this user's expired ones
        sessionRepository.deleteByUserIdAndExpiresAtBefore(user.getId(), Instant.now());
        UserSession session = new UserSession();
        session.setJti(jti);
        session.setUserId(user.getId());
        session.setExpiresAt(Instant.now().plus(SESSION_TTL));
        sessionRepository.save(session);

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        SessionInfo info = buildSessionInfo(user, ctx.org(), ctx.role());
        return new LoginResponse(
                token,
                info.userId(),
                info.fullName(),
                info.platformAdmin(),
                info.organizationId(),
                info.organizationName(),
                info.organizationPrimaryColor(),
                info.organizationLogo(),
                info.roleCode(),
                info.permissions(),
                false,
                null
        );
    }

    private SessionInfo buildSessionInfo(User user, Organization org, Role role) {
        List<String> permissions = role != null
                ? role.getPermissions().stream().map(p -> p.getCode()).sorted().toList()
                : Collections.emptyList();

        String roleCode = role != null ? role.getCode()
                : (user.isPlatformAdmin() ? "platform_super_admin" : null);

        UUID orgId = org != null ? org.getId() : null;
        String logoUrl = org != null ? orgLogoService.resolveLogoUrl(org) : null;

        return new SessionInfo(
                user.getId(),
                user.getFullName(),
                user.isPlatformAdmin(),
                orgId,
                org != null ? org.getName() : null,
                org != null ? org.getPrimaryColor() : null,
                logoUrl,
                roleCode,
                permissions
        );
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String randomOtpCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private record SessionInfo(
            UUID userId,
            String fullName,
            boolean platformAdmin,
            UUID organizationId,
            String organizationName,
            String organizationPrimaryColor,
            String organizationLogo,
            String roleCode,
            List<String> permissions
    ) {}
}
