package com.example;

/**
 * Starts the Eastern Registration System.
 */
public final class EasternRegistration {

    private EasternRegistration() {
        // Prevent instantiation.
    }

    /**
     * Creates and opens the registration menu.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        MainMenu menu = new MainMenu();
        menu.open();
    }
}
