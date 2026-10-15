package com.example;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The course details returned by Eastern's registration schedule.
 *
 * @param id the course identifier
 * @param subject the course subject
 * @param courseNumber the course number
 * @param meetingsFaculty the course's meeting records
 * @param courseTitle the course title, or {@code null} when it is not provided
 * @param creditHours the course credit hours, or {@code null} when not provided
 * @param creditHourLow the minimum credit hours, or {@code null} when not provided
 * @param creditHourHigh the maximum credit hours, or {@code null} when not provided
 * @param enrollment the current enrollment, or {@code null} when not provided
 * @param maximumEnrollment the enrollment capacity, or {@code null} when not provided
 * @param seatsAvailable the seats available, or {@code null} when not provided
 * @param openSection whether Eastern marks the section as open, or {@code null} when not provided
 * @param faculty the instructors listed for the course
 * @param courseReferenceNumber the section reference number, or {@code null} when not provided
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Course(
    String id,
    String subject,
    String courseNumber,
    List<MeetingFaculty> meetingsFaculty,
    String courseTitle,
    Double creditHours,
    Double creditHourLow,
    Double creditHourHigh,
    Integer enrollment,
    Integer maximumEnrollment,
    Integer seatsAvailable,
    Boolean openSection,
    List<Faculty> faculty,
    String courseReferenceNumber
) {

    /**
     * Stores an instructor name supplied by Eastern.
     *
     * @param displayName the instructor name shown in the schedule
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Faculty(String displayName) {
    }

    /**
     * Keeps the constructor with course capacity details available.
     *
     * @param id the course identifier
     * @param subject the course subject
     * @param courseNumber the course number
     * @param meetingsFaculty the course's meeting records
     * @param courseTitle the course title
     * @param creditHours the course credit hours
     * @param creditHourLow the minimum credit hours
     * @param creditHourHigh the maximum credit hours
     * @param enrollment the current enrollment
     * @param maximumEnrollment the enrollment capacity
     * @param seatsAvailable the seats available
     */
    public Course(
        String id,
        String subject,
        String courseNumber,
        List<MeetingFaculty> meetingsFaculty,
        String courseTitle,
        Double creditHours,
        Double creditHourLow,
        Double creditHourHigh,
        Integer enrollment,
        Integer maximumEnrollment,
        Integer seatsAvailable
    ) {
        this(
            id, subject, courseNumber, meetingsFaculty, courseTitle, creditHours,
            creditHourLow, creditHourHigh, enrollment, maximumEnrollment,
            seatsAvailable, null, null, null
        );
    }

    /**
     * Keeps the course constructor with the original detailed course fields available.
     *
     * @param id the course identifier
     * @param subject the course subject
     * @param courseNumber the course number
     * @param meetingsFaculty the course's meeting records
     * @param courseTitle the course title
     * @param creditHours the course credit hours
     * @param creditHourLow the minimum credit hours
     * @param creditHourHigh the maximum credit hours
     */
    public Course(
        String id,
        String subject,
        String courseNumber,
        List<MeetingFaculty> meetingsFaculty,
        String courseTitle,
        Double creditHours,
        Double creditHourLow,
        Double creditHourHigh
    ) {
        this(
            id, subject, courseNumber, meetingsFaculty, courseTitle,
            creditHours, creditHourLow, creditHourHigh, null, null, null, null, null, null
        );
    }

    /**
     * Keeps the original constructor available for existing callers.
     *
     * @param id the course identifier
     * @param subject the course subject
     * @param courseNumber the course number
     * @param meetingsFaculty the course's meeting records
     */
    public Course(String id, String subject, String courseNumber, List<MeetingFaculty> meetingsFaculty) {
        this(id, subject, courseNumber, meetingsFaculty, null, null, null, null);
    }

    /**
     * Stores the meeting time associated with a course.
     *
     * @param meetingTime the meeting's days and times
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MeetingFaculty(MeetingTime meetingTime) {

        /**
         * Returns a readable description of this meeting.
         *
         * @return the formatted meeting details
         */
        @Override
        public String toString() {
            return meetingTime == null ? "Meeting time not listed" : meetingTime.toString();
        }
    }

    /**
     * Stores the days and clock times for a course meeting.
     *
     * @param monday true when the meeting is on Monday
     * @param tuesday true when the meeting is on Tuesday
     * @param wednesday true when the meeting is on Wednesday
     * @param thursday true when the meeting is on Thursday
     * @param friday true when the meeting is on Friday
     * @param saturday true when the meeting is on Saturday
     * @param sunday true when the meeting is on Sunday
     * @param beginTime the meeting start time as provided by Eastern
     * @param endTime the meeting end time as provided by Eastern
     * @param creditHourSession the section's credit hours for this session, or {@code null} when not provided
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MeetingTime(
        boolean monday,
        boolean tuesday,
        boolean wednesday,
        boolean thursday,
        boolean friday,
        boolean saturday,
        boolean sunday,
        String beginTime,
        String endTime,
        Double creditHourSession
    ) {

        /**
         * Keeps the original constructor available for existing callers.
         *
         * @param monday true when the meeting is on Monday
         * @param tuesday true when the meeting is on Tuesday
         * @param wednesday true when the meeting is on Wednesday
         * @param thursday true when the meeting is on Thursday
         * @param friday true when the meeting is on Friday
         * @param saturday true when the meeting is on Saturday
         * @param sunday true when the meeting is on Sunday
         */
        public MeetingTime(
            boolean monday,
            boolean tuesday,
            boolean wednesday,
            boolean thursday,
            boolean friday,
            boolean saturday,
            boolean sunday
        ) {
            this(monday, tuesday, wednesday, thursday, friday, saturday, sunday, null, null, null);
        }

        /**
         * Keeps the constructor with explicit meeting times available.
         *
         * @param monday true when the meeting is on Monday
         * @param tuesday true when the meeting is on Tuesday
         * @param wednesday true when the meeting is on Wednesday
         * @param thursday true when the meeting is on Thursday
         * @param friday true when the meeting is on Friday
         * @param saturday true when the meeting is on Saturday
         * @param sunday true when the meeting is on Sunday
         * @param beginTime the meeting start time
         * @param endTime the meeting end time
         */
        public MeetingTime(
            boolean monday,
            boolean tuesday,
            boolean wednesday,
            boolean thursday,
            boolean friday,
            boolean saturday,
            boolean sunday,
            String beginTime,
            String endTime
        ) {
            this(monday, tuesday, wednesday, thursday, friday, saturday, sunday, beginTime, endTime, null);
        }

        /**
         * Returns the available days and times in a readable format.
         *
         * @return the formatted meeting days and times
         */
        @Override
        public String toString() {
            List<String> days = new ArrayList<>();
            if (monday) {
                days.add("Mon");
            }
            if (tuesday) {
                days.add("Tue");
            }
            if (wednesday) {
                days.add("Wed");
            }
            if (thursday) {
                days.add("Thu");
            }
            if (friday) {
                days.add("Fri");
            }
            if (saturday) {
                days.add("Sat");
            }
            if (sunday) {
                days.add("Sun");
            }

            String start = formatTime(beginTime);
            String end = formatTime(endTime);
            StringBuilder result = new StringBuilder();

            if (start != null && end != null) {
                result.append(start).append(" - ").append(end);
            } else if (start != null) {
                result.append(start);
            } else if (end != null) {
                result.append(end);
            }

            if (!days.isEmpty()) {
                if (result.length() > 0) {
                    result.append(" (");
                }
                result.append(String.join("/", days));
                if (start != null || end != null) {
                    result.append(')');
                }
            }

            return result.length() == 0 ? "Meeting time not listed" : result.toString();
        }

        private static String formatTime(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }

            String time = value.trim();
            if (time.length() != 4) {
                return time;
            }
            for (int i = 0; i < time.length(); i++) {
                if (time.charAt(i) < '0' || time.charAt(i) > '9') {
                    return time;
                }
            }

            int hour = Integer.parseInt(time.substring(0, 2));
            int minute = Integer.parseInt(time.substring(2, 4));
            if (hour > 23 || minute > 59) {
                return time;
            }

            int displayHour = hour % 12;
            if (displayHour == 0) {
                displayHour = 12;
            }
            return displayHour + ":" + time.substring(2) + (hour < 12 ? " AM" : " PM");
        }
    }

    /**
     * Returns a summary of the course and all its listed meetings.
     *
     * @return the formatted course details
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (hasText(subject)) {
            result.append(subject);
        }
        if (hasText(courseNumber)) {
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(courseNumber);
        }
        if (hasText(courseTitle)) {
            if (result.length() > 0) {
                result.append(" - ");
            }
            result.append(courseTitle);
        }
        if (hasText(courseReferenceNumber)) {
            appendDetail(result, "Section " + courseReferenceNumber);
        }

        if (faculty != null) {
            List<String> instructorNames = faculty.stream()
                .filter(java.util.Objects::nonNull)
                .map(Faculty::displayName)
                .filter(Course::hasText)
                .distinct()
                .toList();
            if (!instructorNames.isEmpty()) {
                appendDetail(result, "Instructor: " + String.join(", ", instructorNames));
            }
        }

        String credits = creditsDescription();
        if (credits != null) {
            appendDetail(result, credits);
        }

        if (meetingsFaculty != null) {
            for (MeetingFaculty meeting : meetingsFaculty) {
                if (meeting != null) {
                    appendDetail(result, meeting.toString());
                }
            }
        }

        return result.toString();
    }

    private String creditsDescription() {
        if (creditHours != null) {
            String description = formatCredits(creditHours) + " credits";
            if (creditHourLow != null && creditHourHigh != null
                && Double.compare(creditHourLow, creditHourHigh) != 0) {
                description += " (range: " + formatCredits(creditHourLow)
                    + "-" + formatCredits(creditHourHigh) + " credits)";
            }
            return description;
        }

        Double sectionCredits = sectionCredits();
        if (sectionCredits != null) {
            return formatCredits(sectionCredits) + " credits";
        }

        if (creditHourLow != null && creditHourHigh != null) {
            if (Double.compare(creditHourLow, creditHourHigh) == 0) {
                return formatCredits(creditHourLow) + " credits";
            }
            return formatCredits(creditHourLow) + "-" + formatCredits(creditHourHigh) + " credits";
        }
        if (creditHourLow != null) {
            return "At least " + formatCredits(creditHourLow) + " credits";
        }
        if (creditHourHigh != null) {
            return "Up to " + formatCredits(creditHourHigh) + " credits";
        }
        return null;
    }

    private Double sectionCredits() {
        if (meetingsFaculty == null || meetingsFaculty.isEmpty()) {
            return null;
        }

        Double credits = null;
        for (MeetingFaculty meeting : meetingsFaculty) {
            if (meeting == null || meeting.meetingTime() == null
                || meeting.meetingTime().creditHourSession() == null) {
                return null;
            }
            double meetingCredits = meeting.meetingTime().creditHourSession();
            if (!Double.isFinite(meetingCredits) || meetingCredits < 0
                || (credits != null && Double.compare(credits, meetingCredits) != 0)) {
                return null;
            }
            credits = meetingCredits;
        }
        return credits;
    }

    private static String formatCredits(Double value) {
        String formatted = Double.toString(value);
        return formatted.endsWith(".0") ? formatted.substring(0, formatted.length() - 2) : formatted;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static void appendDetail(StringBuilder result, String detail) {
        if (result.length() > 0) {
            result.append(" | ");
        }
        result.append(detail);
    }
}
