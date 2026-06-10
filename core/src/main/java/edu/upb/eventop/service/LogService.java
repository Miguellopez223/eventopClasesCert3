package edu.upb.eventop.service;

import edu.upb.eventop.repository.LogRepository;
import edu.upb.eventop.repository.entity.Log;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio de logging transaccional ASÍNCRONO.
 *
 * Cada método se ejecuta en otro hilo (@Async) y abre su propia transacción
 * independiente (Propagation.REQUIRES_NEW). De esta forma:
 *   - El método que invoca el log NO espera a que el insert termine, por lo
 *     que la escritura del log no impacta en el tiempo real de la operación.
 *   - El log se persiste con su propio commit, así sobrevive aunque la
 *     transacción "padre" haga rollback.
 *
 * IMPORTANTE: para que @Async y @Transactional funcionen, este servicio debe
 * ser un bean DISTINTO al que lo invoca (Spring aplica los proxies entre beans,
 * nunca en auto-invocaciones dentro de la misma clase).
 */
@Slf4j
@AllArgsConstructor
@Service
public class LogService {

    private final LogRepository repository;

    @Async("logExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void infoTxAsync(String mensaje, String usuario) {
        guardar("INFO", mensaje, usuario);
    }

    @Async("logExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void errorTxAsync(String mensaje, String usuario) {
        guardar("ERROR", mensaje, usuario);
    }

    private void guardar(String nivel, String mensaje, String usuario) {
        try {
            Log entidad = Log.builder()
                    .nivel(nivel)
                    .mensaje(mensaje)
                    .usuario(usuario)
                    .fecha(LocalDateTime.now())
                    .build();
            this.repository.save(entidad);
        } catch (Exception e) {
            // El logging nunca debe tumbar la operación de negocio.
            log.error("No se pudo registrar el log en base de datos: {}", e.getMessage());
        }
    }
}
