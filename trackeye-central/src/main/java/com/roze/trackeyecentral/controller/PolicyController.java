package com.roze.trackeyecentral.controller;

import com.roze.trackeyecentral.model.PolicyRule;
import com.roze.trackeyecentral.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Lets an org define what counts as a policy violation - gambling sites, game
 * launchers, adult content, or any custom pattern - without any list being
 * hard-coded into the product. Admin/supervisor only (enforced by
 * UserAuthenticationFilter for write methods on /api/admin/**).
 */
@RestController
@RequestMapping("/api/admin/policy-rules")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping
    public ResponseEntity<List<PolicyRule>> list(@RequestAttribute Long organizationId) {
        return ResponseEntity.ok(policyService.listRules(organizationId));
    }

    @PostMapping
    public ResponseEntity<PolicyRule> create(
            @RequestAttribute Long organizationId,
            @RequestAttribute("userId") Long callerUserId,
            @RequestBody CreateRuleRequest req) {
        PolicyRule rule = policyService.createRule(
                organizationId, callerUserId, req.getCategory(), req.getMatchType(),
                req.getPattern(), req.getSeverity());
        return ResponseEntity.ok(rule);
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Map<String, Object>> delete(
            @RequestAttribute Long organizationId,
            @PathVariable Long ruleId) {
        policyService.deleteRule(organizationId, ruleId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @lombok.Data
    public static class CreateRuleRequest {
        private String category;
        private String matchType;
        private String pattern;
        private String severity;
    }
}
