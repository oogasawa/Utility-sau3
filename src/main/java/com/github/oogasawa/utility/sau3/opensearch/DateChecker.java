package com.github.oogasawa.utility.sau3.opensearch;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateChecker {
    
    /**
     * This method checks if the given date string is within the last three days, including today.
     *
     * @param dateStr the date string in the format "yyyy-MM-dd"
     * @return true if the date is within the last three days, false otherwise
     */
    public static boolean isWithinLastNDays(String dateStr, int n) {
        // Define the date format pattern for parsing the input date string
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        // Parse the input date string to LocalDate
        LocalDate inputDate = LocalDate.parse(dateStr, formatter);
        
        // Get today's date
        LocalDate today = LocalDate.now();
        
        // Calculate the number of days between the input date and today
        long daysBetween = ChronoUnit.DAYS.between(inputDate, today);
        
        // Return true if the number of days is between 0 and 3, inclusive
        return daysBetween >= 0 && daysBetween <= n;
    }

    // public static void main(String[] args) {
    //     // Example: Check if the date "2024-08-09" is within the last three days
    //     System.out.println(isWithinLastThreeDays("2024-08-09"));  // Output the result
    // }
}
