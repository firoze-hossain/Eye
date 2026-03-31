package com.roze.api;

import com.roze.repository.SessionRepository;
import com.roze.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StatsController {

    private final StatsService statsService;
    private final SessionRepository repository;

    @GetMapping("/today")
    public Map<String, Object> getTodayStats() {
        return statsService.getTodayStats();
    }
    
    @GetMapping("/date/{date}")
    public Map<String, Object> getDateStats(@PathVariable String date) {
        return statsService.getDateStats(date);
    }
    
    @GetMapping("/top-apps")
    public List<Map<String, Object>> getTopApps(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String date) {
        
        long from, to;
        
        if (date != null) {
            LocalDate localDate = LocalDate.parse(date);
            from = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            to = localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } else {
            from = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            to = Instant.now().toEpochMilli();
        }
        
        return repository.getTopApps(from, to, limit);
    }
}