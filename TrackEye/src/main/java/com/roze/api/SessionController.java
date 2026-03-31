package com.roze.api;

import com.roze.model.ActivitySession;
import com.roze.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SessionController {

    private final SessionRepository repository;

    @GetMapping
    public List<ActivitySession> getSessions(@RequestParam(required = false) String date) {
        long from, to;
        
        if (date != null) {
            LocalDate localDate = LocalDate.parse(date);
            from = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            to = localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } else {
            from = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            to = Instant.now().toEpochMilli();
        }
        
        return repository.getActivitySessions(from, to);
    }
    
    @GetMapping("/range")
    public List<ActivitySession> getSessionsByRange(
            @RequestParam long from,
            @RequestParam long to) {
        return repository.getActivitySessions(from, to);
    }
}