package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreakResponse {

  /** Number of consecutive qualifying days (score >= 85) in the current run. */
  private int currentStreak;

  /** Remaining "lives" (tolerance for missed/failed days). Max 2, min 0. */
  private int livesRemaining;

  /** True when currentStreak >= 14 — placeholder for premium unlock. */
  private boolean premiumUnlocked;

  /** The last 14 calendar days (today → 13 days ago), ordered newest first. */
  private List<StreakDay> last14Days;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class StreakDay {
    /** ISO date string (yyyy-MM-dd). */
    private String date;
    /** Computed health score for this day (0-100). 0 if no daily log. */
    private int score;
    /** True if score >= 85. */
    private boolean passed;
    /** True if this day is today. */
    private boolean today;
    /** Short day-of-week label, e.g. "Lun", "Mar". */
    private String dayLabel;
    /**
     * True if the day is within the streak tracking period (i.e. on or after
     * patient's first daily log).
     */
    private boolean active;
  }
}
