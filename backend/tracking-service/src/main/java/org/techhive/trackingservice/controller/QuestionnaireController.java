package org.techhive.trackingservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.trackingservice.dto.*;
import org.techhive.trackingservice.entity.Questionnaire;
import org.techhive.trackingservice.service.QuestionnaireService;

import java.util.List;

@RestController
@RequestMapping("/api/questionnaires")
@RequiredArgsConstructor
public class QuestionnaireController {

    private final QuestionnaireService questionnaireService;

    @GetMapping
    public ResponseEntity<List<Questionnaire>> getAllQuestionnaires() {
        return ResponseEntity.ok(questionnaireService.getAllQuestionnaires());
    }

    @PostMapping("/submit")
    public ResponseEntity<CarePlanResponseDTO> submitAndRecommend(@RequestBody QuestionnaireSubmissionDTO submission) {
        questionnaireService.submitAnswers(submission);
        CarePlanResponseDTO recommendation = questionnaireService.recommendCarePlan(submission);
        return ResponseEntity.ok(recommendation);
    }

    @PostMapping("/recommend")
    public ResponseEntity<CarePlanResponseDTO> getRecommendation(@RequestBody QuestionnaireSubmissionDTO submission) {
        return ResponseEntity.ok(questionnaireService.recommendCarePlan(submission));
    }
}
