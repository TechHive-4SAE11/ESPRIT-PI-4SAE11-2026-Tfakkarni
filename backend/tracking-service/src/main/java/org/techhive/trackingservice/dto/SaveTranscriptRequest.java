package org.techhive.trackingservice.dto;

import lombok.*;

/** Sent from the frontend to append transcript chunks and request periodic AI summary. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaveTranscriptRequest {

    /** Full accumulated transcript text so far */
    private String transcript;

    /** If true, generate a fresh Groq mini-summary for this chunk */
    private boolean requestPartialSummary;

    /** Label for this segment, e.g. "0:00–3:00" */
    private String segmentLabel;
}
