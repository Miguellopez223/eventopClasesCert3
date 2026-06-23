package edu.upb.eventop.controller;

import edu.upb.eventop.quartz.service.JobDto;
import edu.upb.eventop.quartz.service.JobService;
import edu.upb.eventop.quartz.service.JobUtil;
import edu.upb.eventop.repository.dto.request.EmpresaRequestDto;
import edu.upb.eventop.repository.dto.response.EmpresaDto;
import edu.upb.eventop.repository.entity.User;
import edu.upb.eventop.service.EmpresaService;
import edu.upb.eventop.service.exception.OperationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Secured({"ROLE_ADMIN", "ROLE_ROOT"})
@Slf4j
@AllArgsConstructor
@Controller
@RequestMapping("/api/v1/jobs")
public class JobController {
    private final JobService jobService;

    @GetMapping()
    public ResponseEntity<List<JobDto>> listar() {

        try {
            return ResponseEntity.ok(jobService.getAllJobs(JobUtil.GROUP_NAME));
        }catch (Exception e) {
            log.error("Error al listar empresas", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/pausar")
    public ResponseEntity<Void> pausar(@RequestParam("jobName")String jobName) {
        try {
            this.jobService.pauseJob(JobUtil.GROUP_NAME, jobName);
            return ResponseEntity.ok().build();
        }catch (OperationException e){
            log.error("Error al guardar empresa. Message: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }catch (Exception e) {
            log.error("Error al guardar empresa", e);
            //return ResponseEntity.internalServerError().build();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Se generó un error genérico al guardar empresa");
        }
    }

}
