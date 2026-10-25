package org.planb;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "PLANB_TEST_DATABASE_URL", matches = "jdbc:postgresql://(localhost|127\\.0\\.0\\.1)(:[0-9]+)?/planb_test.*")
class ApiContractTests {
    @LocalServerPort int port;
    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();
    private record Account(String email, String token, long id) {}
    private Account leader, member, outsider;
    private long project;

    private HttpResponse<String> call(String method, String path, String token, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        return http.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private JsonNode ok(int status, HttpResponse<String> response) {
        assertEquals(status, response.statusCode(), response.body());
        return response.body().isEmpty() ? json.nullNode() : json.readTree(response.body());
    }
    private Account account() throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        JsonNode registered = ok(200, call("POST", "/users", null, """
                {"firstName":"Test","lastName":"Person","email":"%s","password":"Test-only-42!"}
                """.formatted(email)));
        assertEquals("User created successfully", registered.get("message").asText());
        JsonNode login = ok(200, call("POST", "/auth", null, """
                {"email":"%s","password":"Test-only-42!"}
                """.formatted(email)));
        assertEquals("Login successful", login.get("message").asText());
        return new Account(email, login.get("token").asText(), login.get("userId").asLong());
    }
    private String path() { return "/projects/" + project; }
    private String role(String value) { return "{\"role\":\"" + value + "\"}"; }
    private void add(Account user, String role) throws Exception {
        ok(200, call("POST", path() + "/members", leader.token(),
                "{\"email\":\"" + user.email() + "\",\"role\":\"" + role + "\"}"));
    }

    @BeforeEach void setup() throws Exception {
        leader = account(); member = account(); outsider = account();
        project = ok(200, call("POST", "/projects", leader.token(), """
                {"name":"Contract test","description":"Local isolated fixture"}
                """)).get("id").asLong();
        add(member, "member");
    }

    @Test void authenticationAndValidation() throws Exception {
        ok(401, call("GET", "/users/me", null, null));
        for (String token : List.of("invalid", "", "a.b.c"))
            ok(401, call("GET", "/users/me", token, null));
        String expired = Jwts.builder().subject(leader.email())
                .expiration(Date.from(Instant.now().minusSeconds(30)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode("jjIOnIJqIfezqeucV6/WHiKLgH2AwugZ5QzbkVuR22M="))).compact();
        ok(401, call("GET", "/projects", expired, null));
        ok(401, call("POST", "/auth", null, "{\"email\":\"nobody@example.test\",\"password\":\"wrong\"}"));
        ok(400, call("POST", "/users", null, "{}"));
        ok(400, call("POST", "/projects", leader.token(), "{}"));
        assertEquals(leader.id(), ok(200, call("GET", "/users/me", leader.token(), null)).get("id").asLong());
        ok(204, call("DELETE", "/auth", leader.token(), null));
    }

    @Test void memberContractAndDuplicateChecks() throws Exception {
        JsonNode members = ok(200, call("GET", path() + "/members", member.token(), null));
        assertEquals(2, members.size());
        for (JsonNode item : members)
            for (String field : List.of("userId", "firstName", "lastName", "email", "role")) assertTrue(item.has(field), field);
        ok(409, call("POST", path() + "/members", leader.token(),
                "{\"email\":\"" + member.email().toUpperCase() + "\",\"role\":\"member\"}"));
        ok(404, call("POST", path() + "/members", leader.token(),
                "{\"email\":\"unregistered@example.test\",\"role\":\"member\"}"));
        ok(400, call("POST", path() + "/members", leader.token(),
                "{\"email\":\"" + outsider.email() + "\",\"role\":\"owner\"}"));
        ok(400, call("POST", path() + "/members", leader.token(),
                "{\"email\":\"" + outsider.email() + "\"}"));
    }

    @Test void membershipRequiresProjectLeadership() throws Exception {
        for (Account account : List.of(member, outsider)) {
            ok(403, call("POST", path() + "/members", account.token(),
                    "{\"email\":\"" + outsider.email() + "\",\"role\":\"leader\"}"));
            ok(403, call("PUT", path() + "/members/" + leader.id(), account.token(), role("member")));
            ok(403, call("DELETE", path() + "/members/" + leader.id(), account.token(), null));
        }
        ok(403, call("GET", path(), outsider.token(), null));
        ok(403, call("GET", path() + "/members", outsider.token(), null));
        ok(404, call("GET", "/projects/9223372036854775807", leader.token(), null));
    }

    @Test void lastLeaderProtectionAndAllowedRoleChanges() throws Exception {
        ok(409, call("DELETE", path() + "/members/" + leader.id(), leader.token(), null));
        ok(409, call("PUT", path() + "/members/" + leader.id(), leader.token(), role("member")));
        ok(200, call("PUT", path() + "/members/" + member.id(), leader.token(), role("leader")));
        ok(200, call("PUT", path() + "/members/" + leader.id(), leader.token(), role("member")));
        ok(204, call("DELETE", path() + "/members/" + leader.id(), member.token(), null));
        ok(403, call("GET", path(), leader.token(), null));
        ok(409, call("DELETE", path() + "/members/" + member.id(), member.token(), null));
    }

    @Test void concurrentDemotionsKeepOneLeader() throws Exception {
        ok(200, call("PUT", path() + "/members/" + member.id(), leader.token(), role("leader")));
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var a = executor.submit(() -> call("PUT", path() + "/members/" + leader.id(), leader.token(), role("member")));
            var b = executor.submit(() -> call("PUT", path() + "/members/" + member.id(), member.token(), role("member")));
            List<Integer> statuses = List.of(a.get().statusCode(), b.get().statusCode());
            assertTrue(statuses.contains(200), statuses.toString());
            assertTrue(statuses.contains(409), statuses.toString());
        }
        JsonNode members = ok(200, call("GET", path() + "/members", leader.token(), null));
        int count = 0;
        for (JsonNode item : members) if (item.get("role").asText().equals("PROJECT_LEADER")) count++;
        assertEquals(1, count);
    }

    @Test void concurrentAddsNeverChangeExistingRole() throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            String body = "{\"email\":\"" + outsider.email() + "\",\"role\":\"member\"}";
            var a = executor.submit(() -> call("POST", path() + "/members", leader.token(), body));
            var b = executor.submit(() -> call("POST", path() + "/members", leader.token(), body));
            assertEquals(List.of(200, 409), List.of(a.get().statusCode(), b.get().statusCode()).stream().sorted().toList());
        }
    }

    @Test void projectCrud() throws Exception {
        assertFalse(ok(200, call("GET", "/projects", leader.token(), null)).isEmpty());
        ok(403, call("PUT", path(), member.token(), "{\"name\":\"No\"}"));
        JsonNode updated = ok(200, call("PUT", path(), leader.token(),
                "{\"name\":\"Changed\",\"description\":\"Text\",\"status\":\"COMPLETED\"}"));
        assertEquals("Changed", updated.get("name").asText());
        assertEquals("COMPLETED", updated.get("status").asText());
        ok(403, call("DELETE", path(), member.token(), null));
        ok(204, call("DELETE", path(), leader.token(), null));
        ok(404, call("GET", path(), leader.token(), null));
    }

    private String task(String type, String title) {
        return """
                {"type":"%s","title":"%s","description":"Test","start":"2026-11-01T10:00",
                "end":"2026-11-01T11:00","priority":"HOCH","allParticipants":false,
                "participantIds":[%d,%d],"subtasks":%s}
                """.formatted(type, title, leader.id(), member.id(), type.equals("MEETING") ? "[]"
                : "[{\"title\":\"Subtask\",\"description\":\"Test\",\"assigneeId\":" + member.id() + ",\"done\":false}]");
    }

    @Test void unifiedTaskCrudAndCalendar() throws Exception {
        for (String type : List.of("MEETING", "LANGES_TODO")) {
            String body = task(type, "Original");
            ok(403, call("POST", path() + "/tasks", member.token(), body));
            JsonNode created = ok(201, call("POST", path() + "/tasks", leader.token(), body));
            String url = path() + "/tasks/" + created.get("id").asText();
            assertEquals("HIGH", created.get("priority").asText());
            assertEquals("2026-11-01T10:00", created.get("start").asText());
            assertEquals(type, ok(200, call("GET", url, member.token(), null)).get("type").asText());
            ok(403, call("GET", url, outsider.token(), null));
            ok(200, call("PUT", url, leader.token(), task(type, "Edited")));
            ok(403, call("PUT", url, member.token(), body));
            ok(200, call("DELETE", url + "/assignees/me", member.token(), null));
            ok(403, call("PUT", url + "/status", member.token(), "{\"status\":\"done\"}"));
            ok(200, call("PUT", url + "/assignees/me", member.token(), null));
            assertTrue(ok(200, call("PUT", url + "/status", member.token(), "{\"status\":\"done\"}")).get("done").asBoolean());
            assertFalse(ok(200, call("PUT", url + "/status", leader.token(), "{\"status\":\"open\"}")).get("done").asBoolean());
            ok(400, call("PUT", url + "/status", leader.token(), "{\"status\":\"invalid\"}"));
            assertFalse(ok(200, call("GET", "/users/me/calendar", member.token(), null)).isEmpty());
            ok(403, call("DELETE", url, member.token(), null));
            ok(204, call("DELETE", url, leader.token(), null));
            ok(404, call("GET", url, leader.token(), null));
        }
        assertEquals(0, ok(200, call("GET", path() + "/tasks", leader.token(), null)).size());
    }

    @Test void taskCannotEscapeProjectOrAcceptForeignParticipant() throws Exception {
        String id = ok(201, call("POST", path() + "/tasks", leader.token(), task("MEETING", "First"))).get("id").asText();
        long second = ok(200, call("POST", "/projects", leader.token(), "{\"name\":\"Second\"}")).get("id").asLong();
        ok(404, call("GET", "/projects/" + second + "/tasks/" + id, leader.token(), null));
        ok(400, call("POST", path() + "/tasks", leader.token(),
                task("MEETING", "Bad").replace("\"participantIds\":[" + leader.id() + "," + member.id() + "]",
                        "\"participantIds\":[" + outsider.id() + "]")));
        ok(400, call("POST", path() + "/tasks", leader.token(), task("MEETING", "Bad").replace("11:00", "09:00")));
        ok(400, call("GET", path() + "/tasks/1", leader.token(), null));
        ok(400, call("POST", path() + "/tasks", leader.token(), "{}"));
    }

    @Test void removingMemberClearsDependentAssignments() throws Exception {
        String id = ok(201, call("POST", path() + "/tasks", leader.token(), task("LANGES_TODO", "Todo"))).get("id").asText();
        ok(204, call("DELETE", path() + "/members/" + member.id(), leader.token(), null));
        JsonNode task = ok(200, call("GET", path() + "/tasks/" + id, leader.token(), null));
        assertEquals(1, task.get("participantIds").size());
        assertTrue(task.get("subtasks").get(0).get("assigneeId").isNull());
        ok(204, call("DELETE", path(), leader.token(), null));
    }

    @Test void subtaskOwnershipAndAutomaticCompletion() throws Exception {
        JsonNode created = ok(201, call("POST", path() + "/tasks", leader.token(), task("LANGES_TODO", "Subtasks")));
        String id = created.get("id").asText();
        long subId = created.get("subtasks").get(0).get("id").asLong();
        String url = "/tasks/" + id + "/subtasks/" + subId;
        ok(409, call("POST", url + "/claim", leader.token(), null));
        ok(200, call("POST", url + "/claim", member.token(), null));
        ok(403, call("PATCH", url, outsider.token(), "{\"done\":true}"));
        JsonNode done = ok(200, call("PATCH", url, member.token(), "{\"done\":true}"));
        assertTrue(done.get("task").get("done").asBoolean());
        assertFalse(ok(200, call("PATCH", url, leader.token(), "{\"done\":false}")).get("task").get("done").asBoolean());
        ok(400, call("PATCH", url, leader.token(), "{}"));
    }

    @Test void corsPreflightAndErrorResponses() throws Exception {
        String origin = "https://plan-b-frontend-1a40950601f6.herokuapp.com";
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path() + "/members"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type").build();
        var preflight = http.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, preflight.statusCode());
        assertEquals(origin, preflight.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        var patch = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/tasks/t1/subtasks/1"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).header("Origin", origin)
                .header("Access-Control-Request-Method", "PATCH")
                .header("Access-Control-Request-Headers", "authorization,content-type,accept").build();
        assertEquals(200, http.send(patch, HttpResponse.BodyHandlers.ofString()).statusCode());
        var invalid = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/users/me"))
                .header("Origin", origin).header("Authorization", "Bearer invalid").GET().build();
        var response = http.send(invalid, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode());
        assertEquals(origin, response.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
    }

    @Test void invitationRoutesRemainAbsent() throws Exception {
        ok(404, call("GET", path() + "/invitations?status=AUSSTEHEND", leader.token(), null));
        ok(404, call("POST", path() + "/invitations", leader.token(), "{}"));
        ok(404, call("DELETE", path() + "/invitations/1", leader.token(), null));
    }
}
