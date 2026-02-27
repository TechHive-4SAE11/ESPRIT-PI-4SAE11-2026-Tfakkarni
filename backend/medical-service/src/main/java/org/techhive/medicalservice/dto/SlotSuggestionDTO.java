package org.techhive.medicalservice.dto;

import java.time.LocalDateTime;

public class SlotSuggestionDTO {

    private LocalDateTime start;
    private LocalDateTime end;
    private String reason;

    public SlotSuggestionDTO() {
    }

    public SlotSuggestionDTO(LocalDateTime start, LocalDateTime end, String reason) {
        this.start = start;
        this.end = end;
        this.reason = reason;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

