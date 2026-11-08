package org.planb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final int MAX_CALENDARS = 250;
    private static final int CALENDAR_BATCH_SIZE = 50;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GoogleCalendarService(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
    }

    public GoogleCalendarBusyResponse getBusyPeriods(
            GoogleCalendarBusyRequest request,
            String googleAccessToken
    ) {
        if (googleAccessToken == null || googleAccessToken.isBlank()) {
            throw new GoogleCalendarException(400, "Google access token is required");
        }

        TimeRange range = validateRange(request);
        JsonNode calendars = send(
                "GET",
                "/users/me/calendarList?minAccessRole=reader&maxResults=" + MAX_CALENDARS,
                null,
                googleAccessToken
        );
        List<String> calendarIds = readableCalendarIds(calendars);
        List<GoogleCalendarBusyResponse.BusyPeriod> periods = new ArrayList<>();

        for (int offset = 0; offset < calendarIds.size(); offset += CALENDAR_BATCH_SIZE) {
            List<String> batch = calendarIds.subList(
                    offset,
                    Math.min(offset + CALENDAR_BATCH_SIZE, calendarIds.size())
            );
            var body = objectMapper.createObjectNode()
                    .put("timeMin", range.timeMin())
                    .put("timeMax", range.timeMax());
            var items = body.putArray("items");
            batch.forEach(id -> items.addObject().put("id", id));

            JsonNode busyResponse = send(
                    "POST",
                    "/freeBusy",
                    body.toString(),
                    googleAccessToken
            );
            busyResponse.path("calendars").fields().forEachRemaining(entry -> {
                JsonNode busy = entry.getValue().path("busy");
                if (!busy.isArray()) {
                    return;
                }
                busy.forEach(period -> {
                    String from = period.path("start").asText("");
                    String to = period.path("end").asText("");
                    if (!from.isBlank() && !to.isBlank()) {
                        periods.add(new GoogleCalendarBusyResponse.BusyPeriod(from, to));
                    }
                });
            });
        }
        return new GoogleCalendarBusyResponse(periods);
    }

    private TimeRange validateRange(GoogleCalendarBusyRequest request) {
        if (request == null) {
            throw new GoogleCalendarException(400, "Google Calendar time range is required");
        }
        try {
            OffsetDateTime timeMin = OffsetDateTime.parse(request.getTimeMin());
            OffsetDateTime timeMax = OffsetDateTime.parse(request.getTimeMax());
            if (!timeMin.isBefore(timeMax)) {
                throw new GoogleCalendarException(400, "Google Calendar time range is invalid");
            }
            if (timeMax.isAfter(timeMin.plusDays(370))) {
                throw new GoogleCalendarException(400, "Google Calendar time range is too large");
            }
            return new TimeRange(timeMin.toString(), timeMax.toString());
        } catch (DateTimeParseException exception) {
            throw new GoogleCalendarException(400, "Google Calendar time range is invalid");
        }
    }

    private List<String> readableCalendarIds(JsonNode response) {
        List<String> ids = new ArrayList<>();
        JsonNode items = response.path("items");
        if (!items.isArray()) {
            return ids;
        }
        items.forEach(calendar -> {
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
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(GOOGLE_API + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token);
            if ("POST".equals(method)) {
                builder.header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body));
            } else {
                builder.GET();
            }

            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw mapGoogleError(response.statusCode());
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

    private GoogleCalendarException mapGoogleError(int status) {
        if (status == 400) {
            return new GoogleCalendarException(400, "Google Calendar request is invalid");
        }
        if (status == 401 || status == 403) {
            return new GoogleCalendarException(401, "Google Calendar authorization failed");
        }
        return new GoogleCalendarException(502, "Google Calendar is currently unavailable");
    }

    private record TimeRange(String timeMin, String timeMax) {
    }
}
