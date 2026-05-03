package org.techhive.trackingservice.util;

import org.junit.jupiter.api.Test;
import org.techhive.trackingservice.enums.MedicationStatus;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DurationParserTest {

    @Test
    void calculateEndDate_supportsEnglishAndFrenchDurationUnits() {
        LocalDate start = LocalDate.of(2026, 1, 15);

        assertThat(DurationParser.calculateEndDate(start, "10 days")).isEqualTo(LocalDate.of(2026, 1, 25));
        assertThat(DurationParser.calculateEndDate(start, "2 semaines")).isEqualTo(LocalDate.of(2026, 1, 29));
        assertThat(DurationParser.calculateEndDate(start, "3 mois")).isEqualTo(LocalDate.of(2026, 4, 15));
        assertThat(DurationParser.calculateEndDate(start, "1 année")).isEqualTo(LocalDate.of(2027, 1, 15));
    }

    @Test
    void calculateEndDate_returnsNullForOngoingBlankOrUnknownDurations() {
        LocalDate start = LocalDate.of(2026, 1, 15);

        assertThat(DurationParser.calculateEndDate(start, "ongoing treatment")).isNull();
        assertThat(DurationParser.calculateEndDate(start, "en cours")).isNull();
        assertThat(DurationParser.calculateEndDate(start, "permanent")).isNull();
        assertThat(DurationParser.calculateEndDate(start, "longue durée")).isNull();
        assertThat(DurationParser.calculateEndDate(start, "indéfini")).isNull();
        assertThat(DurationParser.calculateEndDate(start, "continue")).isNull();
        assertThat(DurationParser.calculateEndDate(start, "unknown format")).isNull();
        assertThat(DurationParser.calculateEndDate(start, " ")).isNull();
        assertThat(DurationParser.calculateEndDate(null, "5 days")).isNull();
        assertThat(DurationParser.calculateEndDate(start, null)).isNull();
    }

    @Test
    void isOngoing_detectsSupportedPhrasesAndRejectsNullOrFixedDurations() {
        assertThat(DurationParser.isOngoing("Long-term medication")).isTrue();
        assertThat(DurationParser.isOngoing("traitement indéfini")).isTrue();
        assertThat(DurationParser.isOngoing(null)).isFalse();
        assertThat(DurationParser.isOngoing("7 jours")).isFalse();
    }

    @Test
    void determineStatus_handlesOngoingExpiredActiveAndFutureStarts() {
        LocalDate today = LocalDate.of(2026, 5, 3);

        assertThat(DurationParser.determineStatus(LocalDate.of(2026, 1, 1), null, today))
                .isEqualTo(MedicationStatus.ONGOING);
        assertThat(DurationParser.determineStatus(LocalDate.of(2026, 5, 10), null, today))
                .isEqualTo(MedicationStatus.ACTIVE);
        assertThat(DurationParser.determineStatus(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 2), today))
                .isEqualTo(MedicationStatus.EXPIRED);
        assertThat(DurationParser.determineStatus(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 3), today))
                .isEqualTo(MedicationStatus.ACTIVE);
        assertThat(DurationParser.determineStatus(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 4), today))
                .isEqualTo(MedicationStatus.ACTIVE);
    }
}
