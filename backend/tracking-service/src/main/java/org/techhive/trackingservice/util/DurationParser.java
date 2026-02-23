package org.techhive.trackingservice.util;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to parse medication duration strings and calculate end dates
 * 
 * Supports formats like:
 * - "3 months", "3 mois"
 * - "30 days", "30 jours"
 * - "2 weeks", "2 semaines"
 * - "1 year", "1 an", "1 année"
 * - "ongoing", "en cours", "permanent", "long-term"
 */
@Slf4j
public class DurationParser {

    private static final Pattern DURATION_PATTERN = Pattern.compile(
        "(\\d+)\\s*(day|days|jour|jours|week|weeks|semaine|semaines|month|months|mois|year|years|an|ans|année|années)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Calculates the end date based on a start date and duration string
     * 
     * @param startDate The start date of the medication
     * @param duration The duration string (e.g., "3 months", "30 days")
     * @return The calculated end date, or null if duration is ongoing
     */
    public static LocalDate calculateEndDate(LocalDate startDate, String duration) {
        if (startDate == null || duration == null || duration.trim().isEmpty()) {
            return null;
        }

        String durationLower = duration.toLowerCase().trim();

        // Check for ongoing/permanent medications
        if (isOngoing(durationLower)) {
            return null; // No end date for ongoing medications
        }

        // Parse the duration
        Matcher matcher = DURATION_PATTERN.matcher(durationLower);
        if (matcher.find()) {
            int quantity = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();

            return calculateEndDateFromUnits(startDate, quantity, unit);
        }

        log.warn("Unable to parse duration: {}. Setting no end date.", duration);
        return null;
    }

    /**
     * Checks if the duration indicates an ongoing/permanent medication
     */
    public static boolean isOngoing(String duration) {
        if (duration == null) {
            return false;
        }

        String durationLower = duration.toLowerCase().trim();
        return durationLower.contains("ongoing") 
            || durationLower.contains("en cours") 
            || durationLower.contains("permanent")
            || durationLower.contains("long-term")
            || durationLower.contains("longue durée")
            || durationLower.contains("indéfini")
            || durationLower.contains("continue");
    }

    /**
     * Calculates end date based on quantity and unit
     */
    private static LocalDate calculateEndDateFromUnits(LocalDate startDate, int quantity, String unit) {
        switch (unit) {
            case "day":
            case "days":
            case "jour":
            case "jours":
                return startDate.plusDays(quantity);

            case "week":
            case "weeks":
            case "semaine":
            case "semaines":
                return startDate.plusWeeks(quantity);

            case "month":
            case "months":
            case "mois":
                return startDate.plusMonths(quantity);

            case "year":
            case "years":
            case "an":
            case "ans":
            case "année":
            case "années":
                return startDate.plusYears(quantity);

            default:
                log.warn("Unknown duration unit: {}", unit);
                return null;
        }
    }

    /**
     * Determines the appropriate medication status based on dates
     */
    public static org.techhive.trackingservice.enums.MedicationStatus determineStatus(
            LocalDate startDate, 
            LocalDate endDate, 
            LocalDate currentDate) {
        
        if (currentDate == null) {
            currentDate = LocalDate.now();
        }

        // Ongoing medications (no end date)
        if (endDate == null) {
            if (startDate != null && currentDate.isBefore(startDate)) {
                return org.techhive.trackingservice.enums.MedicationStatus.ACTIVE;
            }
            return org.techhive.trackingservice.enums.MedicationStatus.ONGOING;
        }

        // Expired medications
        if (currentDate.isAfter(endDate)) {
            return org.techhive.trackingservice.enums.MedicationStatus.EXPIRED;
        }

        // Active medications
        return org.techhive.trackingservice.enums.MedicationStatus.ACTIVE;
    }
}
