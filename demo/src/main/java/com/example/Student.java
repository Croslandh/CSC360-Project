package com.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a student and the courses the student is registered for.
 *
 * <p>Registration checks use only credit, enrollment, capacity, and meeting
 * information supplied by the course data. If exact credit hours are missing,
 * registration is rejected because the 17-credit limit cannot be checked
 * safely. Meeting conflicts can only be detected when both meetings have
 * usable day and start/end-time data.</p>
 */
public class Student {

    /** The maximum number of credits a student may register for. */
    public static final int MAX_CREDITS = 17;

    private final String studentId;
    private final String firstName;
    private final String lastName;
    private final List<Course> registeredCourses = new ArrayList<>();

    /**
     * Creates a student with no registered courses.
     *
     * @param studentId the student's ID
     * @param firstName the student's first name
     * @param lastName the student's last name
     * @throws IllegalArgumentException if an ID or name is missing
     */
    public Student(String studentId, String firstName, String lastName) {
        this.studentId = requireText(studentId, "Student ID");
        this.firstName = requireText(firstName, "First name");
        this.lastName = requireText(lastName, "Last name");
    }

    /**
     * Returns this student's ID.
     *
     * @return the student ID
     */
    public String getStudentId() {
        return studentId;
    }

    /**
     * Returns this student's first name.
     *
     * @return the first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Returns this student's last name.
     *
     * @return the last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Registers the student for one course.
     *
     * @param course the course to register for
     * @throws IllegalArgumentException if the course is null, already registered,
     *         full, over the credit limit, or has an identifiable time conflict;
     *         also thrown when exact credits are unavailable
     */
    public void registerCourse(Course course) {
        registerCourses(List.of(Objects.requireNonNull(course, "Course must not be null")));
    }

    /**
     * Registers the student for multiple courses as one operation. Either every
     * course passes the checks and is added, or none of them are added.
     *
     * @param courses the courses to register for
     * @throws IllegalArgumentException if a course is null, duplicated, full,
     *         over the credit limit, or has an identifiable time conflict;
     *         also thrown when exact credits are unavailable
     */
    public void registerCourses(List<Course> courses) {
        Objects.requireNonNull(courses, "Courses must not be null");

        List<Course> requestedCourses = new ArrayList<>(courses.size());
        for (Course course : courses) {
            if (course == null) {
                throw new IllegalArgumentException("Course must not be null");
            }
            if (containsCourse(registeredCourses, course)
                || containsCourse(requestedCourses, course)) {
                throw new IllegalArgumentException("Course is already registered: " + course);
            }
            if (isFull(course)) {
                throw new IllegalArgumentException("Course is full: " + course);
            }
            requestedCourses.add(course);
        }

        List<Course> allCourses = new ArrayList<>(registeredCourses);
        allCourses.addAll(requestedCourses);

        double totalCredits = 0;
        for (Course course : allCourses) {
            totalCredits += exactCredits(course);
        }
        if (totalCredits > MAX_CREDITS) {
            throw new IllegalArgumentException(
                "Registration would exceed the " + MAX_CREDITS + "-credit limit"
            );
        }

        for (int i = registeredCourses.size(); i < allCourses.size(); i++) {
            Course course = allCourses.get(i);
            for (int j = 0; j < i; j++) {
                if (hasMeetingConflict(allCourses.get(j), course)) {
                    throw new IllegalArgumentException(
                        "Course conflicts with an already selected course: " + course
                    );
                }
            }
        }

        registeredCourses.addAll(requestedCourses);
    }

    /**
     * Drops one course from the student's schedule.
     *
     * @param course the course to drop
     * @return {@code true} if the course was registered and removed;
     *         {@code false} if it was not on the schedule
     * @throws IllegalArgumentException if the course is null
     */
    public boolean dropCourse(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("Course must not be null");
        }
        for (int i = 0; i < registeredCourses.size(); i++) {
            if (sameCourse(registeredCourses.get(i), course)) {
                registeredCourses.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Drops every course from the student's schedule.
     */
    public void dropAllCourses() {
        registeredCourses.clear();
    }

    /**
     * Returns a read-only copy of the student's registered courses.
     *
     * @return the current course schedule
     */
    public List<Course> getSchedule() {
        return Collections.unmodifiableList(new ArrayList<>(registeredCourses));
    }

    private static boolean isFull(Course course) {
        if (course.seatsAvailable() != null) {
            return course.seatsAvailable() <= 0;
        }
        return course.enrollment() != null
            && course.maximumEnrollment() != null
            && course.enrollment() >= course.maximumEnrollment();
    }

    private static double exactCredits(Course course) {
        if (course.creditHours() != null) {
            return requireValidCredits(course.creditHours(), course);
        }
        if (course.creditHourLow() != null && course.creditHourHigh() != null
            && Double.compare(course.creditHourLow(), course.creditHourHigh()) == 0) {
            return requireValidCredits(course.creditHourLow(), course);
        }

        List<Course.MeetingFaculty> meetings = course.meetingsFaculty();
        if (meetings != null && !meetings.isEmpty()) {
            Double sessionCredits = null;
            for (Course.MeetingFaculty meeting : meetings) {
                if (meeting == null || meeting.meetingTime() == null
                    || meeting.meetingTime().creditHourSession() == null) {
                    throw missingCredits(course);
                }
                double value = requireValidCredits(meeting.meetingTime().creditHourSession(), course);
                if (sessionCredits != null && Double.compare(sessionCredits, value) != 0) {
                    throw missingCredits(course);
                }
                sessionCredits = value;
            }
            if (sessionCredits != null) {
                return sessionCredits;
            }
        }
        throw missingCredits(course);
    }

    private static double requireValidCredits(double credits, Course course) {
        if (!Double.isFinite(credits) || credits < 0) {
            throw new IllegalArgumentException("Invalid credit value for course: " + course);
        }
        return credits;
    }

    private static IllegalArgumentException missingCredits(Course course) {
        return new IllegalArgumentException(
            "Exact credit hours are unavailable for course: " + course
                + ". Provide an exact credit value before registering."
        );
    }

    private static boolean containsCourse(List<Course> courses, Course candidate) {
        for (Course existing : courses) {
            if (sameCourse(existing, candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameCourse(Course first, Course second) {
        if (first.id() != null && !first.id().isBlank()
            && second.id() != null && !second.id().isBlank()) {
            return first.id().equals(second.id());
        }
        return first.equals(second);
    }

    private static boolean hasMeetingConflict(Course first, Course second) {
        List<Course.MeetingFaculty> firstMeetings = first.meetingsFaculty();
        List<Course.MeetingFaculty> secondMeetings = second.meetingsFaculty();
        if (firstMeetings == null || secondMeetings == null) {
            return false;
        }

        for (Course.MeetingFaculty firstMeeting : firstMeetings) {
            Course.MeetingTime firstTime = firstMeeting == null ? null : firstMeeting.meetingTime();
            if (firstTime == null) {
                continue;
            }
            int firstStart = parseTime(firstTime.beginTime());
            int firstEnd = parseTime(firstTime.endTime());
            if (firstStart < 0 || firstEnd <= firstStart) {
                continue;
            }

            for (Course.MeetingFaculty secondMeeting : secondMeetings) {
                Course.MeetingTime secondTime = secondMeeting == null ? null : secondMeeting.meetingTime();
                if (secondTime == null) {
                    continue;
                }
                int secondStart = parseTime(secondTime.beginTime());
                int secondEnd = parseTime(secondTime.endTime());
                if (secondStart < 0 || secondEnd <= secondStart) {
                    continue;
                }

                if (sameMeetingDay(firstTime, secondTime)
                    && firstStart < secondEnd && secondStart < firstEnd) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean sameMeetingDay(Course.MeetingTime first, Course.MeetingTime second) {
        return (first.monday() && second.monday())
            || (first.tuesday() && second.tuesday())
            || (first.wednesday() && second.wednesday())
            || (first.thursday() && second.thursday())
            || (first.friday() && second.friday())
            || (first.saturday() && second.saturday())
            || (first.sunday() && second.sunday());
    }

    private static int parseTime(String value) {
        if (value == null) {
            return -1;
        }
        String digits = value.trim().replace(":", "");
        if (digits.length() != 4) {
            return -1;
        }
        for (int i = 0; i < digits.length(); i++) {
            if (digits.charAt(i) < '0' || digits.charAt(i) > '9') {
                return -1;
            }
        }

        int hours = Integer.parseInt(digits.substring(0, 2));
        int minutes = Integer.parseInt(digits.substring(2, 4));
        if (hours > 23 || minutes > 59) {
            return -1;
        }
        return hours * 60 + minutes;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }
}
