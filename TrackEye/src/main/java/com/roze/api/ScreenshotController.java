package com.roze.api;

import com.roze.model.ScreenshotRecord;
import com.roze.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/screenshots")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ScreenshotController {

    private final SessionRepository repository;

    @GetMapping
    public List<ScreenshotRecord> getScreenshots(@RequestParam(required = false) String date) {
        long from, to;
        
        if (date != null) {
            LocalDate localDate = LocalDate.parse(date);
            from = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            to = localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } else {
            from = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            to = Instant.now().toEpochMilli();
        }
        
        return repository.getScreenshots(from, to);
    }
    
    @GetMapping("/image")
    public ResponseEntity<Resource> getScreenshotImage(@RequestParam String path) {
        File file = new File(path);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .body(resource);
    }
}