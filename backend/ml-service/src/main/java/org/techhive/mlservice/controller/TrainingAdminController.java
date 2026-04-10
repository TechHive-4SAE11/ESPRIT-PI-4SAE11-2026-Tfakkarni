package org.techhive.mlservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.techhive.mlservice.entity.TrainingModule;
import org.techhive.mlservice.service.TrainingService;

import java.util.List;

@RestController
@RequestMapping("/api/ml/admin/training")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TrainingAdminController {

    private final TrainingService trainingService;

    @PostMapping("/modules")
    public TrainingModule createModule(@RequestBody TrainingModule module) {
        return trainingService.createModule(module);
    }

    @PutMapping("/modules/{id}")
    public TrainingModule updateModule(@PathVariable Long id, @RequestBody TrainingModule module) {
        return trainingService.updateModule(id, module);
    }

    @DeleteMapping("/modules/{id}")
    public void deleteModule(@PathVariable Long id) {
        trainingService.deleteModule(id);
    }

    @GetMapping("/modules")
    public List<TrainingModule> getAllModules() {
        return trainingService.getAllModules();
    }
}