package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;

/**
 * Runs the text-based menu for looking up courses and managing one student's
 * schedule.
 *
 * <p>The menu uses {@link EasternSchedule} to retrieve course data and
 * delegates registration and drop rules to {@link Student}. It does not close
 * the supplied scanner; its owner is responsible for managing that input
 * resource.</p>
 */
public class MainMenu {

    private final EasternSchedule easternSchedule;
    private Student student;
    private final Scanner scanner;
    private boolean inputEnded;

    /**
     * Creates a menu for console use. The student details are requested when
     * the menu opens, so no placeholder identity is used.
     */
    public MainMenu() {
        this.easternSchedule = new EasternSchedule();
        this.student = null;
        this.scanner = new Scanner(System.in);
    }

    /**
     * Creates a menu that reads from the console.
     *
     * @param easternSchedule the source of course schedule data
     * @param student the student whose schedule this menu manages
     */
    public MainMenu(EasternSchedule easternSchedule, Student student) {
        this(easternSchedule, student, new Scanner(System.in));
    }

    /**
     * Creates a menu using the given input source.
     *
     * @param easternSchedule the source of course schedule data
     * @param student the student whose schedule this menu manages
     * @param scanner the source of menu input; it remains open after the menu exits
     */
    public MainMenu(EasternSchedule easternSchedule, Student student, Scanner scanner) {
        this.easternSchedule = Objects.requireNonNull(easternSchedule, "Schedule must not be null");
        this.student = Objects.requireNonNull(student, "Student must not be null");
        this.scanner = Objects.requireNonNull(scanner, "Scanner must not be null");
    }

    /**
     * Displays the menu and processes choices until the student selects Exit
     * or the input stream ends.
     */
    public void open() {
        if (student == null && !promptForStudentDetails()) {
            System.out.println("\nStudent details were not provided. Closing the menu.");
            return;
        }

        boolean running = true;
        while (running) {
            displayMenu();
            String choice = readLine("Choose an option: ");
            if (choice == null) {
                System.out.println("\nNo more input. Closing the menu.");
                return;
            }

            switch (choice.trim()) {
                case "1" -> displayCoursesForSubject();
                case "2" -> searchCourses();
                case "3" -> registerForCourse();
                case "4" -> dropCourses();
                case "5" -> displayStudentSchedule();
                case "6" -> {
                    System.out.println("Goodbye.");
                    running = false;
                }
                default -> System.out.println("Please enter a menu number from 1 to 6.");
            }
        }
    }

    private boolean promptForStudentDetails() {
        System.out.println("Student Information");
        String studentId = readRequired("Student ID: ");
        if (studentId == null) {
            return false;
        }
        String firstName = readRequired("First name: ");
        if (firstName == null) {
            return false;
        }
        String lastName = readRequired("Last name: ");
        if (lastName == null) {
            return false;
        }
        student = new Student(studentId, firstName, lastName);
        return true;
    }

    private void displayMenu() {
        System.out.println();
        System.out.println("Eastern Registration System");
        System.out.println("1. Display all courses for a subject");
        System.out.println("2. Search for courses");
        System.out.println("3. Register for a course");
        System.out.println("4. Drop a course");
        System.out.println("5. Display my schedule");
        System.out.println("6. Exit");
    }

    private void displayCoursesForSubject() {
        String subject = promptSubject();
        if (subject == null) {
            return;
        }

        try {
            List<Course> courses = easternSchedule.queryCourseBySubject(subject);
            printCourses(courses, "Courses for " + subject);
        } catch (Exception exception) {
            reportScheduleError(subject, exception);
        }
    }

    private void searchCourses() {
        String subject = promptSubject();
        if (subject == null) {
            return;
        }
        String courseNumber = readRequired("Course number: ");
        if (courseNumber == null) {
            return;
        }

        Boolean openOnlyAnswer = promptYesNo("Show open courses only? (y/n, default n): ");
        if (openOnlyAnswer == null) {
            return;
        }
        boolean openOnly = openOnlyAnswer;
        String instructor = readOptional("Instructor name contains (press Enter to skip): ");
        if (inputEnded) {
            return;
        }
        Day excludedDay = promptExcludedDay();
        if (inputEnded) {
            return;
        }

        try {
            // Always retrieve the subject's results first, then apply local filters.
            List<Course> subjectCourses = easternSchedule.queryCourseBySubject(subject);
            List<Course> matches = new ArrayList<>();
            int unknownOpen = 0;
            int unknownInstructor = 0;
            int unknownDays = 0;

            for (Course course : subjectCourses) {
                if (course == null
                    || !sameText(course.subject(), subject)
                    || !sameText(course.courseNumber(), courseNumber)) {
                    continue;
                }

                boolean matchesFilters = true;
                if (openOnly) {
                    Boolean open = isOpen(course);
                    if (open == null) {
                        unknownOpen++;
                        matchesFilters = false;
                    } else if (!open) {
                        matchesFilters = false;
                    }
                }

                if (!instructor.isBlank()) {
                    List<String> names = instructorNames(course);
                    if (names.isEmpty()) {
                        unknownInstructor++;
                        matchesFilters = false;
                    } else if (names.stream().noneMatch(name ->
                        name.toLowerCase(Locale.ROOT).contains(instructor.toLowerCase(Locale.ROOT)))) {
                        matchesFilters = false;
                    }
                }

                if (excludedDay != null) {
                    DayMatch dayMatch = courseHasKnownDay(course, excludedDay);
                    if (dayMatch == DayMatch.UNKNOWN) {
                        unknownDays++;
                        matchesFilters = false;
                    } else if (dayMatch == DayMatch.MATCHES_EXCLUDED_DAY) {
                        matchesFilters = false;
                    }
                }

                if (matchesFilters) {
                    matches.add(course);
                }
            }

            printCourses(matches, "Search results for " + subject + " " + courseNumber);
            reportUnknownFilterData(openOnly, unknownOpen, !instructor.isBlank(), unknownInstructor,
                excludedDay != null, unknownDays);
        } catch (Exception exception) {
            reportScheduleError(subject, exception);
        }
    }

    private void registerForCourse() {
        String subject = promptSubject();
        if (subject == null) {
            return;
        }
        String courseNumber = readRequired("Course number: ");
        if (courseNumber == null) {
            return;
        }

        try {
            List<Course> subjectCourses = easternSchedule.queryCourseBySubject(subject);
            List<Course> matches = subjectCourses.stream()
                .filter(Objects::nonNull)
                .filter(course -> sameText(course.subject(), subject)
                    && sameText(course.courseNumber(), courseNumber))
                .toList();
            Course selectedCourse = chooseCourse(matches, "Select the course section to register for");
            if (selectedCourse == null) {
                return;
            }

            try {
                student.registerCourse(selectedCourse);
                System.out.println("Registered for " + selectedCourse + ".");
            } catch (IllegalArgumentException exception) {
                System.out.println("Registration was not completed: " + exception.getMessage());
            }
        } catch (Exception exception) {
            reportScheduleError(subject, exception);
        }
    }

    private void dropCourses() {
        List<Course> schedule = student.getSchedule();
        if (schedule.isEmpty()) {
            System.out.println("There are no registered courses to drop.");
            return;
        }

        System.out.println("1. Drop one course");
        System.out.println("2. Drop all courses");
        String choice = readLine("Choose an option (or press Enter to go back): ");
        if (choice == null || choice.isBlank()) {
            return;
        }
        if (choice.trim().equals("1")) {
            Course selectedCourse = chooseCourse(schedule, "Select the course to drop");
            if (selectedCourse != null) {
                if (student.dropCourse(selectedCourse)) {
                    System.out.println("Dropped " + selectedCourse + ".");
                } else {
                    System.out.println("That course is no longer on the schedule.");
                }
            }
        } else if (choice.trim().equals("2")) {
            boolean confirmed = promptYesNo("Drop every registered course? (y/n): ");
            if (confirmed) {
                student.dropAllCourses();
                System.out.println("All courses have been dropped.");
            }
        } else {
            System.out.println("Please enter 1 or 2.");
        }
    }

    private void displayStudentSchedule() {
        printCourses(student.getSchedule(), "Your current schedule");
    }

    private Course chooseCourse(List<Course> courses, String prompt) {
        if (courses.isEmpty()) {
            System.out.println("No matching courses were found.");
            return null;
        }

        printNumberedCourses(courses);
        while (true) {
            String value = readLine(prompt + " (enter 1-" + courses.size() + ", or 0 to cancel): ");
            if (value == null) {
                return null;
            }
            try {
                int selection = Integer.parseInt(value.trim());
                if (selection == 0) {
                    return null;
                }
                if (selection >= 1 && selection <= courses.size()) {
                    return courses.get(selection - 1);
                }
            } catch (NumberFormatException ignored) {
                // Show the same helpful prompt for text that is not a number.
            }
            System.out.println("Please enter a valid number from the list, or 0 to cancel.");
        }
    }

    private void printCourses(List<Course> courses, String heading) {
        System.out.println();
        System.out.println(heading + ":");
        if (courses == null || courses.isEmpty()) {
            System.out.println("No courses found.");
            return;
        }
        printNumberedCourses(courses);
        System.out.println(courses.size() + " course(s).");
    }

    private void printNumberedCourses(List<Course> courses) {
        for (int i = 0; i < courses.size(); i++) {
            System.out.println((i + 1) + ". " + courses.get(i));
        }
    }

    private String promptSubject() {
        while (true) {
            String subject = readRequired("Subject code: ");
            if (subject == null) {
                return null;
            }
            if (subject.matches("[A-Za-z0-9]+")) {
                return subject.toUpperCase(Locale.ROOT);
            }
            System.out.println("Enter a subject code using letters and numbers only.");
        }
    }

    private String readRequired(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (value == null) {
                return null;
            }
            if (!value.isBlank()) {
                return value.trim();
            }
            System.out.println("This value is required.");
        }
    }

    private String readOptional(String prompt) {
        String value = readLine(prompt);
        return value == null ? null : value.trim();
    }

    private Boolean promptYesNo(String prompt) {
        while (true) {
            String answer = readLine(prompt);
            if (answer == null) {
                return null;
            }
            if (answer.isBlank() || answer.trim().equalsIgnoreCase("n")
                || answer.trim().equalsIgnoreCase("no")) {
                return false;
            }
            if (answer.trim().equalsIgnoreCase("y") || answer.trim().equalsIgnoreCase("yes")) {
                return true;
            }
            System.out.println("Please enter y or n.");
        }
    }

    private Day promptExcludedDay() {
        while (true) {
            String value = readOptional(
                "Exclude a meeting day (Mon, Tue, Wed, Thu, Fri, Sat, Sun; Enter to skip): "
            );
            if (value == null || value.isBlank()) {
                return null;
            }
            Day day = Day.fromInput(value);
            if (day != null) {
                return day;
            }
            System.out.println("Enter a day name such as Tue or Thu, or press Enter to skip.");
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            inputEnded = true;
            return null;
        }
        return scanner.nextLine();
    }

    private void reportScheduleError(String subject, Exception exception) {
        if (exception instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        String message = exception.getMessage();
        System.out.println(
            "Could not retrieve courses for " + subject + "."
                + (message == null || message.isBlank() ? "" : " " + message)
        );
    }

    private void reportUnknownFilterData(
        boolean openRequested,
        int unknownOpen,
        boolean instructorRequested,
        int unknownInstructor,
        boolean dayRequested,
        int unknownDays
    ) {
        if (openRequested && unknownOpen > 0) {
            System.out.println(
                "Open-course filtering omitted " + unknownOpen
                    + " matching course(s) because Eastern did not provide usable availability data."
            );
        }
        if (instructorRequested && unknownInstructor > 0) {
            System.out.println(
                "Instructor filtering omitted " + unknownInstructor
                    + " matching course(s) because no instructor name was available."
            );
        }
        if (dayRequested && unknownDays > 0) {
            System.out.println(
                "Day filtering omitted " + unknownDays
                    + " matching course(s) because their meeting days were not listed."
            );
        }
    }

    private static Boolean isOpen(Course course) {
        Boolean sectionStatus = course.openSection();
        Boolean seatsStatus = course.seatsAvailable() == null
            ? null : course.seatsAvailable() > 0;

        if (sectionStatus != null && seatsStatus != null && !sectionStatus.equals(seatsStatus)) {
            return null;
        }
        if (sectionStatus != null) {
            return sectionStatus;
        }
        return seatsStatus;
    }

    private static List<String> instructorNames(Course course) {
        if (course.faculty() == null) {
            return List.of();
        }
        return course.faculty().stream()
            .filter(Objects::nonNull)
            .map(Course.Faculty::displayName)
            .filter(name -> name != null && !name.isBlank())
            .toList();
    }

    private static DayMatch courseHasKnownDay(Course course, Day excludedDay) {
        List<Course.MeetingFaculty> meetings = course.meetingsFaculty();
        if (meetings == null || meetings.isEmpty()) {
            return DayMatch.UNKNOWN;
        }

        for (Course.MeetingFaculty meeting : meetings) {
            Course.MeetingTime time = meeting == null ? null : meeting.meetingTime();
            if (time == null) {
                return DayMatch.UNKNOWN;
            }
            if (!hasAnyDay(time)) {
                return DayMatch.UNKNOWN;
            }
            if (excludedDay.isScheduled(time)) {
                return DayMatch.MATCHES_EXCLUDED_DAY;
            }
        }
        return DayMatch.DOES_NOT_MATCH_EXCLUDED_DAY;
    }

    private static boolean hasAnyDay(Course.MeetingTime time) {
        return time.monday() || time.tuesday() || time.wednesday() || time.thursday()
            || time.friday() || time.saturday() || time.sunday();
    }

    private static boolean sameText(String first, String second) {
        return first != null && second != null && first.trim().equalsIgnoreCase(second.trim());
    }

    private enum DayMatch {
        MATCHES_EXCLUDED_DAY,
        DOES_NOT_MATCH_EXCLUDED_DAY,
        UNKNOWN
    }

    private enum Day {
        MONDAY("mon", "monday"),
        TUESDAY("tue", "tuesday"),
        WEDNESDAY("wed", "wednesday"),
        THURSDAY("thu", "thursday"),
        FRIDAY("fri", "friday"),
        SATURDAY("sat", "saturday"),
        SUNDAY("sun", "sunday");

        private final String shortName;
        private final String fullName;

        Day(String shortName, String fullName) {
            this.shortName = shortName;
            this.fullName = fullName;
        }

        private boolean isScheduled(Course.MeetingTime time) {
            return switch (this) {
                case MONDAY -> time.monday();
                case TUESDAY -> time.tuesday();
                case WEDNESDAY -> time.wednesday();
                case THURSDAY -> time.thursday();
                case FRIDAY -> time.friday();
                case SATURDAY -> time.saturday();
                case SUNDAY -> time.sunday();
            };
        }

        private static Day fromInput(String value) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (Day day : values()) {
                if (day.shortName.equals(normalized) || day.fullName.equals(normalized)) {
                    return day;
                }
            }
            return null;
        }
    }
}
