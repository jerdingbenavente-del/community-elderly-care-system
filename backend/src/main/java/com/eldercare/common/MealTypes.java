package com.eldercare.common;

import java.util.List;

public final class MealTypes {

    public static final String BREAKFAST = "BREAKFAST";
    public static final String LUNCH = "LUNCH";
    public static final String DINNER = "DINNER";
    public static final List<String> ALL = List.of(BREAKFAST, LUNCH, DINNER);

    private MealTypes() {
    }

    public static boolean valid(String value) {
        return ALL.contains(value);
    }
}
