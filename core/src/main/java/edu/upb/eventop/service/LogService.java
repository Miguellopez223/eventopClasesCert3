package edu.upb.eventop.service;

import edu.upb.eventop.repository.LogRepository;
import edu.upb.eventop.repository.entity.Log;
import edu.upb.eventop.repository.enums.LogLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Servicio de logging transaccional ASÍNCRONO.
 *
 * Cada método @Async("taskLog") se ejecuta en un hilo del pool "taskLog" (no en
 * el hilo del método que lo invoca), por lo que la escritura del log NO impacta
 * en el tiempo real de la operación. Los métodos *Tx abren además su propia
 * transacción (Propagation.REQUIRES_NEW), así el log se persiste con su propio
 * commit aunque la transacción "padre" haga rollback.
 *
 * IMPORTANTE: para que @Async y @Transactional funcionen, este servicio debe ser
 * un bean DISTINTO al que lo invoca (Spring aplica los proxies entre beans, nunca
 * en auto-invocaciones dentro de la misma clase).
 */
@Slf4j
@AllArgsConstructor
@Service
public class LogService {

    private final LogRepository repository;

    @Async("taskLog")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void infoTx(String message) {
        log.info("INFO: " + message);
        repository.save(Log.builder()
                .level(LogLevel.INFO)
                .message(message)
                .build());
    }

    @Async("taskLog")
    @Transactional
    public void info(String message) {
        repository.save(Log.builder()
                .level(LogLevel.INFO)
                .message(message)
                .build());
    }

    @Async("taskLog")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void errorTx(String message) {
        repository.save(Log.builder()
                .level(LogLevel.ERROR)
                .message(message)
                .build());
    }

    @Async("taskLog")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void warning(String message) {
        repository.save(Log.builder()
                .level(LogLevel.WARNING)
                .message(message)
                .build());
    }

    @Async("taskLog")
    @Transactional
    public void deleteAll() {
        repository.deleteAll();
    }

    @Async
    @Transactional
    public Future<String> delete(String id) {
        repository.deleteById(id);
        try {
            Thread.sleep(5000);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(id);
    }

    @Transactional(readOnly = true)
    public Page<Log> findAllByOrderByDateDesc(LocalDateTime pInit, LocalDateTime pEnd, Pageable pageable) {
        return repository.findAllByOrderByDateDesc(pInit, pEnd, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Log> findAllByOrderByDateDesc(Pageable pageable) {
        return repository.findAllByOrderByDateDesc(pageable);
    }
}
