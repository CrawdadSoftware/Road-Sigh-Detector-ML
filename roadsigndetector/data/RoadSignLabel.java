package com.example.roadsigndetector.data;

public class RoadSignLabel {
    // Etykiety dla modelu klasyfikacyjnego
    public static final String[] classifierLabels = {
            "speed limit 10",
            "speed limit 20",
            "speed limit 30",
            "speed limit 40",
            "speed limit 5",
            "speed limit 50",
            "speed limit 60",
            "speed limit 70",
            "speed limit 80"
    };

    // Etykiety dla modelu detekcyjnego
    public static final String[] detectLabels = {
            "unknown",
            "crosswalk",
            "stop",
            "main road",
            "give road",
            "children",
            "dont stop",
            "no parking",
            "dont move",
            "dont enter",
            "dont overtake",
            "speed limit 5",
            "speed limit 10",
            "speed limit 20",
            "speed limit 30",
            "speed limit 40",
            "speed limit 50",
            "speed limit 60",
            "speed limit 70",
            "speed limit 80",
            "speed limit 90",
            "speed limit 100"
    };
}
