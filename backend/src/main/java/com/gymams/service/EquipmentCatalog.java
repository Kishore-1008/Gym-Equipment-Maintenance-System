package com.gymams.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single source of truth for the 8 allowed equipment names and their
 * Name -> Category mapping. The frontend keeps a copy for instant UX
 * (populating the dropdown, previewing the category before submit),
 * but this backend copy is the one that is actually enforced — the
 * frontend copy is never trusted.
 */
public final class EquipmentCatalog {

    private EquipmentCatalog() {}

    private static final Map<String, String> NAME_TO_CATEGORY = new LinkedHashMap<>();
    static {
        NAME_TO_CATEGORY.put("Treadmill", "Cardio");
        NAME_TO_CATEGORY.put("Exercise Bike", "Cardio");
        NAME_TO_CATEGORY.put("Bench Press", "Strength");
        NAME_TO_CATEGORY.put("Elliptical Trainer", "Cardio");
        NAME_TO_CATEGORY.put("Rowing Machine", "Cardio");
        NAME_TO_CATEGORY.put("Squat Rack", "Strength");
        NAME_TO_CATEGORY.put("Lat Pulldown Machine", "Strength");
        NAME_TO_CATEGORY.put("Leg Press Machine", "Strength");
    }

    public static List<String> allowedNames() {
        return List.copyOf(NAME_TO_CATEGORY.keySet());
    }

    public static boolean isAllowedName(String name) {
        return name != null && NAME_TO_CATEGORY.containsKey(name);
    }

    public static String categoryFor(String name) {
        return NAME_TO_CATEGORY.get(name);
    }
}
