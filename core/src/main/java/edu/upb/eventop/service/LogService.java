package edu.upb.eventop.service;

import edu.upb.eventop.repository.LogRepository;
import edu.upb.eventop.repository.entity.Log;
import edu.upb.eventop.repository.enums.LogLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@AllArgsConstructor
@Service
public class LogService {
    private final LogRepository repository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void infoTx(String message) {

        log.info("INFO: " + message);

        repository.save(Log.builder()
                .level(LogLevel.INFO)
                .message(message)
                .build());
    }
    @Async
    @Transactional
    public void info(String message) {
        repository.save(Log.builder()
                .level(LogLevel.INFO)
                .message(message)
                .build());
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void errorTx(String message) {
        repository.save(Log.builder()
                .level(LogLevel.ERROR)
                .message(message)
                .build());
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void warning(String message) {
        repository.save(Log.builder()
                .level(LogLevel.WARNING)
                .message(message)
                .build());
    }

    @Async
    @Transactional
    public void deleteAll() {
        repository.deleteAll();
    }

    @Transactional(readOnly = true)
    public Page<Log> findAllByOrderByDateDesc(LocalDateTime pInit,  LocalDateTime pEnd, Pageable pageable) {
        return repository.findAllByOrderByDateDesc(pInit, pEnd, pageable);
    }

}
