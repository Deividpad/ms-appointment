package com.ms.appointment.controller;

import com.ms.appointment.dto.request.DoctorRequestDTO;
import com.ms.appointment.dto.response.DoctorResponseDTO;
import com.ms.appointment.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/doctor")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @PostMapping
    public ResponseEntity<DoctorResponseDTO> registerDoctor(@Valid @RequestBody DoctorRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.registerDoctor(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorResponseDTO> getDoctorById(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getDoctorById(id));
    }
}
