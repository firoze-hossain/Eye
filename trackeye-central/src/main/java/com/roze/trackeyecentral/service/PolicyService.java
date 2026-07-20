package com.roze.trackeyecentral.service;

import com.roze.trackeyecentral.model.PolicyRule;
import com.roze.trackeyecentral.model.PolicyViolation;
import com.roze.trackeyecentral.model.User;
import com.roze.trackeyecentral.repository.PolicyRuleRepository;
import com.roze.trackeyecentral.repository.PolicyViolationRepository;
import com.roze.trackeyecentral.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Detects when tracked activity matches an organization's own policy rules
 * (e.g. "gambling site", "game launcher") and raises a violation + notifies
 * the right people.
 *
 * Deliberately ships with NO pre-seeded blocklist of specific sites - every
 * organization defines what it cares about under Settings > Policy Rules.
 * This keeps the product generic and avoids hard-coding a list of explicit
 * content sources into source control.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRuleRepository policyRuleRepository;
    private final PolicyViolationRepository policyViolationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public List<PolicyRule> listRules(Long organizationId) {
        return policyRuleRepository.findByOrganizationId(organizationId);
    }

    public PolicyRule createRule(Long organizationId, Long createdBy, String category,
                                  String matchType, String pattern, String severity) {
        PolicyRule rule = new PolicyRule();
        rule.setOrganizationId(organizationId);
        rule.setCategory(category);
        rule.setMatchType(matchType);
        rule.setPattern(pattern.toLowerCase().trim());
        rule.setSeverity(severity == null ? "MEDIUM" : severity);
        rule.setActive(true);
        rule.setCreatedAt(System.currentTimeMillis());
        rule.setCreatedBy(createdBy);
        return policyRuleRepository.save(rule);
    }

    public void deleteRule(Long organizationId, Long ruleId) {
        policyRuleRepository.findById(ruleId).ifPresent(r -> {
            if (r.getOrganizationId().equals(organizationId)) {
                policyRuleRepository.deleteById(ruleId);
            }
        });
    }

    /** Check a browser page (title + URL) against the org's rules. */
    @Transactional
    public void checkBrowserActivity(Long organizationId, Long userId, Long deviceId,
                                      String url, String pageTitle) {
        String haystackUrl = url == null ? "" : url.toLowerCase();
        String haystackTitle = pageTitle == null ? "" : pageTitle.toLowerCase();

        for (PolicyRule rule : activeRules(organizationId)) {
            boolean matched = switch (rule.getMatchType()) {
                case "URL_DOMAIN", "URL_KEYWORD" -> haystackUrl.contains(rule.getPattern());
                case "WINDOW_TITLE_KEYWORD" -> haystackTitle.contains(rule.getPattern());
                default -> false;
            };
            if (matched) {
                String matchedValue = haystackUrl.contains(rule.getPattern()) ? url : pageTitle;
                recordViolation(organizationId, userId, deviceId, rule, matchedValue);
            }
        }
    }

    /** Check a foreground app/process against the org's rules. */
    @Transactional
    public void checkAppActivity(Long organizationId, Long userId, Long deviceId,
                                  String appName, String processName) {
        String haystack = ((appName == null ? "" : appName) + " " + (processName == null ? "" : processName)).toLowerCase();

        for (PolicyRule rule : activeRules(organizationId)) {
            if ("APP_NAME".equals(rule.getMatchType()) && haystack.contains(rule.getPattern())) {
                recordViolation(organizationId, userId, deviceId, rule, appName);
            }
        }
    }

    private List<PolicyRule> activeRules(Long organizationId) {
        return policyRuleRepository.findByOrganizationIdAndActiveTrue(organizationId);
    }

    private void recordViolation(Long organizationId, Long userId, Long deviceId,
                                  PolicyRule rule, String matchedValue) {
        PolicyViolation v = new PolicyViolation();
        v.setOrganizationId(organizationId);
        v.setUserId(userId);
        v.setDeviceId(deviceId);
        v.setRuleId(rule.getId());
        v.setCategory(rule.getCategory());
        v.setSeverity(rule.getSeverity());
        v.setMatchedValue(matchedValue);
        v.setOccurredAt(System.currentTimeMillis());
        policyViolationRepository.save(v);

        log.info("Policy violation: user {} matched {} rule ({})", userId, rule.getCategory(), matchedValue);

        // Notify the org's admins, plus the employee's own manager if they have one.
        User employee = userRepository.findById(userId).orElse(null);
        String employeeName = employee != null ? employee.getFullName() : "An employee";
        String title = "Policy alert: " + rule.getCategory();
        String body = employeeName + " triggered a " + rule.getSeverity().toLowerCase()
                + " severity " + rule.getCategory().toLowerCase() + " rule (" + matchedValue + ")";

        notificationService.notifyAdminsAndManager(organizationId, userId, "POLICY_VIOLATION", title, body);
    }
}
