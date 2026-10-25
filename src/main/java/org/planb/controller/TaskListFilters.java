package org.planb.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class TaskListFilters {
    private TaskListFilters() {}

    static List<Map<String, Object>> meetings(List<Map<String, Object>> rows, LocalDate from, LocalDate to, String sort) {
        if (from != null && to != null && to.isBefore(from)) throw new ApiException(400, "end before start");
        return rows.stream()
                .filter(row -> from == null || time(row, "endDateTime").isAfter(from.atStartOfDay()))
                .filter(row -> to == null || time(row, "startDateTime").isBefore(to.plusDays(1).atStartOfDay()))
                .sorted(order(sort == null ? "startDateTime" : sort)).toList();
    }

    static List<Map<String, Object>> todos(List<Map<String, Object>> rows, String priority, Boolean done, String sort) {
        if (priority != null && !List.of("LOW", "MEDIUM", "HIGH").contains(priority)) throw new ApiException(400, "invalid priority");
        return rows.stream()
                .filter(row -> priority == null || priority.equals(row.get("priority")))
                .filter(row -> done == null || done.equals(row.get("done")))
                .sorted(order(sort == null ? "endDateTime" : sort)).toList();
    }

    private static Comparator<Map<String, Object>> order(String sort) {
        return switch (sort) {
            case "startDateTime", "endDateTime" -> Comparator.comparing(row -> time(row, sort));
            case "title" -> Comparator.comparing(row -> String.valueOf(row.get("title")), String.CASE_INSENSITIVE_ORDER);
            case "priority" -> Comparator.comparingInt(row -> List.of("HIGH", "MEDIUM", "LOW").indexOf(row.get("priority")));
            default -> throw new ApiException(400, "sort attribute not allowed");
        };
    }

    private static LocalDateTime time(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime();
        return LocalDateTime.parse(value.toString());
    }
}
