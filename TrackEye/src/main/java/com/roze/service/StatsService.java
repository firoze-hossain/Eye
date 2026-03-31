package com.roze.service;

import com.roze.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final SessionRepository repository;

    public Map<String, Object> getTodayStats() {
        long startOfDay = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();
        long now = Instant.now().toEpochMilli();
        
        long totalTime = repository.getTotalTime(startOfDay, now);
        List<Map<String, Object>> topApps = repository.getTopApps(startOfDay, now, 5);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSeconds", totalTime / 1000);
        stats.put("totalMinutes", totalTime / 60000);
        stats.put("totalHours", totalTime / 3600000);
        stats.put("topApps", topApps);
        stats.put("date", LocalDate.now().toString());
        
        return stats;
    }
    
    public Map<String, Object> getDateStats(String date) {
        LocalDate localDate = LocalDate.parse(date);
        long startOfDay = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endOfDay = localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        long totalTime = repository.getTotalTime(startOfDay, endOfDay);
        List<Map<String, Object>> topApps = repository.getTopApps(startOfDay, endOfDay, 10);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSeconds", totalTime / 1000);
        stats.put("totalMinutes", totalTime / 60000);
        stats.put("totalHours", totalTime / 3600000);
        stats.put("topApps", topApps);
        stats.put("date", date);
        
        return stats;
    }
}