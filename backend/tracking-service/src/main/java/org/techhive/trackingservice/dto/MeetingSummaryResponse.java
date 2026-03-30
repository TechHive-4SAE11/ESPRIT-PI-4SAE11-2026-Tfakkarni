package org.techhive.trackingservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingSummaryResponse {
    private Long meetingId;
    private String summary;
    private Integer durationMinutes;
}
