package org.techhive.trackingservice.dto;

import lombok.*;

/** Response for a periodic / partial AI mini-summary of the live transcript. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartialSummaryResponse {

    private Long meetingId;

    /** Human-readable segment label, e.g. "Segment 1 (0:00–3:00)" */
    private String segmentLabel;

    /** The AI mini-summary text */
    private String summary;

    /** Updated full transcriptSummaries JSON stored in DB */
    private String transcriptSummaries;
}
