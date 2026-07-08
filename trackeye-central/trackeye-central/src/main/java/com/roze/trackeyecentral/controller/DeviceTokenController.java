// src/main/java/com/roze/trackeyecentral/controller/DeviceTokenController.java
package com.roze.trackeyecentral.controller;

import com.roze.trackeyecentral.model.User;
import com.roze.trackeyecentral.repository.UserRepository;
import com.roze.trackeyecentral.security.CryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Lets a logged-in admin/supervisor mint a fresh DEVICE REGISTRATION TOKEN for
 * their own organization - the thing the desktop agent needs to register.
 *
 * Why this exists: previously the only place a device token was produced was the
 * one-time organization-registration response. If you lost it, there was no way
 * to onboard another machine. This closes that gap and is the normal onboarding
 * path (generate a token, hand it to the employee, they paste it into the agent).
 *
 * It lives under /api/admin/**, so the UserAuthenticationFilter already:
 *   - validates the Bearer token,
 *   - sets organizationId + userId,
 *   - requires admin/supervisor for POST.
 *
 * The token format matches what UserService.registerDevice expects:
 *     encrypt("<userId>:<orgId>:<timestamp>")
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final CryptoService cryptoService;
    private final UserRepository userRepository;

    /**
     * POST /api/admin/device-token
     * Optional query param ?forUserId=<id> to mint a token for another user in
     * the same org (e.g. an employee you're onboarding). Defaults to yourself.
     */
    @PostMapping("/device-token")
    public ResponseEntity<?> generate(
            @RequestAttribute Long organizationId,
            @RequestAttribute Long userId,
            @RequestParam(required = false) Long forUserId) {

        Long targetUserId = (forUserId != null) ? forUserId : userId;

        User target = userRepository.findById(targetUserId).orElse(null);
        if (target == null || !organizationId.equals(target.getOrganizationId())) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found in your organization"));
        }

        String raw = target.getId() + ":" + organizationId + ":" + System.currentTimeMillis();
        String registrationToken = cryptoService.encrypt(raw);

        log.info("Issued device-registration token for user {} (org {})", target.getId(), organizationId);

        return ResponseEntity.ok(Map.of(
                "registrationToken", registrationToken,
                "email", target.getEmail(),
                "userId", target.getId(),
                "organizationId", organizationId
        ));
    }
}
