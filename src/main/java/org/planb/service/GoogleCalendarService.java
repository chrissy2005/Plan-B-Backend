package org.planb.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.planb.dto.GoogleCalendarBusyRequest;
import org.planb.dto.GoogleCalendarBusyResponse;
import org.planb.exception.GoogleCalendarException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleCalendarService {
    private static final String GOOGLE_API = "https://www.googleapis.com/calendar/v3";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper;

    public GoogleCalendarService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public GoogleCalendarBusyResponse getBusyPeriods(
            GoogleCalendarBusyRequest request,
            String googleAccessToken
    ) {
        if (googleAccessToken == null || googleAccessToken.isBlank()) {
            throw new GoogleCalendarException(400, "Google access token is required");
        }
        String timeMin = normalizeTime(request == null ? null : request.getTimeMin());
        String timeMax = normalizeTime(request == null ? null : request.getTimeMax());
        validateRange(timeMin, timeMax);

        JsonNode calendars = send(
                "GET",
                "/users/me/calendarList?minAccessRole=reader&maxResults=250",
                null,
                googleAccessToken
        );
        List<String> ids = readableCalendarIds(calendars);
        List<GoogleCalendarBusyResponse.BusyPeriod> periods = new ArrayList<>();

        for (int offset = 0; offset < ids.size(); offset += 50) {
            List<String> batch = ids.subList(offset, Math.min(offset + 50, ids.size()));
            var body = objectMapper.createObjectNode()
                    .put("timeMin", timeMin)
                    .put("timeMax", timeMax);
            var items = body.putArray("items");
            batch.forEach(id -> items.addObject().put("id", id));
            JsonNode busy = send("POST", "/freeBusy", body.toString(), googleAccessToken);
            busy.path("calendars").properties().forEach(entry ->
                    entry.getValue().path("busy").forEach(period -> {
                        String from = period.path("start").asText("");
                        String to = period.path("end").asText("");
                        if (!from.isBlank() && !to.isBlank()) {
                            periods.add(new GoogleCalendarBusyResponse.BusyPeriod(from, to));
                        }
                    }));
        }
        return new GoogleCalendarBusyResponse(periods);
    }

    private String normalizeTime(String value) {
        if (value == null || value.isBlank()) {
            throw new GoogleCalendarException(400, "Google Calendar time range is required");
        }
        try {
            return OffsetDateTime.parse(value).toString();
        } catch (DateTimeParseException exception) {
            throw new GoogleCalendarException(400, "Google Calendar time range is invalid");
        }
    }

    private void validateRange(String timeMin, String timeMax) {
        OffsetDateTime from = OffsetDateTime.parse(timeMin);
        OffsetDateTime to = OffsetDateTime.parse(timeMax);
        if (!from.isBefore(to) || to.isAfter(from.plusDays(370))) {
            throw new GoogleCalendarException(400, "Google Calendar time range is invalid");
        }
    }

    private List<String> readableCalendarIds(JsonNode response) {
        List<String> ids = new ArrayList<>();
        response.path("items").forEach(calendar -> {
            String role = calendar.path("accessRole").asText("");
            String id = calendar.path("id").asText("");
            if (!id.isBlank() && List.of("owner", "writer", "reader").contains(role)) {
                ids.add(id);
            }
        });
        return ids;
    }

    private JsonNode send(String method, String path, String body, String token) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(GOOGLE_API + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token);
            if ("POST".equals(method)) {
                request.header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body));
            } else {
                request.GET();
            }
            HttpResponse<String> response = httpClient.send(
                    request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                int status = response.statusCode() == 401 || response.statusCode() == 403
                        ? 401 : response.statusCode() == 400 ? 400 : 502;
                String message = status == 401
                        ? "Google Calendar authorization failed"
                        : status == 400 ? "Google Calendar request is invalid"
                        : "Google Calendar is currently unavailable";
                throw new GoogleCalendarException(status, message);
            }
            return objectMapper.readTree(response.body());
        } catch (GoogleCalendarException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new GoogleCalendarException(502, "Google Calendar is currently unavailable");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GoogleCalendarException(502, "Google Calendar request was interrupted");
        }
    }
}
