package com.estacionamento.api.web.camera;

import com.estacionamento.api.domain.camera.CameraRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cameras")
public class CameraController {

    private final CameraRepository cameraRepository;

    public CameraController(CameraRepository cameraRepository) {
        this.cameraRepository = cameraRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CAMERA_READ')")
    public List<CameraResponse> list() {
        return cameraRepository.findAllByOrderByNameAsc().stream().map(CameraResponse::from).toList();
    }
}
