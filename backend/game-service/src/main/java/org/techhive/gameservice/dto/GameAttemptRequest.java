package org.techhive.gameservice.dto;

import java.util.List;

public class GameAttemptRequest {
    private Long miniGameId;
    private List<AnswerEntry> answers;
    private int durationSeconds;

    public GameAttemptRequest() {
    }

    public Long getMiniGameId() { return miniGameId; }
    public void setMiniGameId(Long miniGameId) { this.miniGameId = miniGameId; }
    public List<AnswerEntry> getAnswers() { return answers; }
    public void setAnswers(List<AnswerEntry> answers) { this.answers = answers; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public static class AnswerEntry {
        private Long imageId;
        private String selectedName;

        public AnswerEntry() {
        }

        public Long getImageId() { return imageId; }
        public void setImageId(Long imageId) { this.imageId = imageId; }
        public String getSelectedName() { return selectedName; }
        public void setSelectedName(String selectedName) { this.selectedName = selectedName; }
    }
}
