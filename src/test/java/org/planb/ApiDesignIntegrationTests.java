package org.planb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.planb.security.JwtService;
import org.planb.model.User;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiDesignIntegrationTests {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtService jwt;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private String token, memberToken, otherToken, memberEmail;
    private long projectId, leaderId, memberId;
    record Reply(int status, JsonNode body) {}

    private Reply call(String method, String path, Object body, String bearer) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        if (bearer != null) builder.header("Authorization", "Bearer " + bearer);
        if (body != null) builder.header("Content-Type", "application/json");
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), json.readTree(response.body()));
    }

    private String register(String email) throws Exception {
        assertEquals(200, call("POST", "/users", Map.of("firstName", "API", "lastName", "Test",
                "email", email, "password", "LocalTest2026!"), null).status);
        return call("POST", "/auth", Map.of("email", email, "password", "LocalTest2026!"), null).body.path("token").asText();
    }

    @BeforeEach void setup() throws Exception {
        String prefix = UUID.randomUUID().toString();
        String email = prefix + "-leader@example.test";
        memberEmail = prefix + "-member@example.test";
        token = register(email);
        memberToken = register(memberEmail);
        otherToken = register(prefix + "-other@example.test");
        leaderId = call("GET", "/users/me", null, token).body.path("id").asLong();
        memberId = call("GET", "/users/me", null, memberToken).body.path("id").asLong();
        var project = call("POST", "/projects", Map.of("name", "API test", "description", "isolated fixture"), token);
        assertEquals(200, project.status);
        projectId = project.body.path("projectId").asLong();
        assertTrue(projectId > 0);
        assertEquals(200, call("POST", "/projects/" + projectId + "/members",
                Map.of("email", memberEmail, "role", "MEMBER"), token).status);
    }

    @Test void jwtFailuresAre401() throws Exception {
        for (String invalid : new String[]{"bad-token", ""}) {
            var result = call("GET", "/projects", null, invalid);
            assertEquals(401, result.status);
            assertEquals("token wrong", result.body.path("message").asText());
        }
        assertEquals(401, call("GET", "/projects", null, null).status);
        User user = new User(leaderId, "Expired", "Test", "expired@example.test", "unused");
        var expired = new JwtService("jjIOnIJqIfezqeucV6/WHiKLgH2AwugZ5QzbkVuR22M=", -10000).generateToken(user);
        assertEquals(401, call("GET", "/projects", null, expired).status);
    }

    @Test void memberResponseAndErrorsMatchDesign() throws Exception {
        String path = "/projects/" + projectId + "/members";
        var members = call("GET", path, null, token);
        assertEquals(200, members.status);
        assertTrue(members.body.get(0).has("email"));
        assertEquals(400, call("POST", path, Map.of("email", memberEmail, "role", "MEMBER"), token).status);
        assertEquals(404, call("POST", path, Map.of("email", "missing@example.test", "role", "MEMBER"), token).status);
        assertEquals(403, call("POST", path, Map.of("email", "missing@example.test", "role", "MEMBER"), memberToken).status);
        assertEquals(404, call("GET", path, null, otherToken).status);
        assertEquals(400, call("POST", path, Map.of("email", memberEmail, "role", "member"), token).status);
    }

    @Test void meetingCreateReadAndPermissions() throws Exception {
        var body = Map.of("title", "Review", "startDateTime", "2026-11-01T10:00",
                "endDateTime", "2026-11-01T11:00", "participantIds", new long[]{leaderId, memberId});
        String path = "/projects/" + projectId + "/meetings";
        var created = call("POST", path, body, token);
        assertEquals(200, created.status);
        long id = created.body.path("meetingId").asLong();
        var details = call("GET", "/meetings/" + id, null, memberToken);
        assertEquals(200, details.status);
        assertEquals(2, details.body.path("participantIds").size());
        assertEquals("2026-11-01T10:00", details.body.path("startDateTime").asText());
        assertEquals(403, call("POST", path, body, memberToken).status);
        assertEquals(404, call("GET", "/meetings/" + id, null, otherToken).status);
        assertEquals(0, call("GET", path + "?from=2027-01-01&sort=startDateTime", null, token).body.size());
        assertEquals(400, call("GET", path + "?sort=garbage", null, token).status);
    }

    @Test void longTodoAndSubtaskCreateArePersisted() throws Exception {
        var body = Map.of("title", "Long todo", "startDateTime", "2026-11-01T10:00",
                "endDateTime", "2026-11-02T10:00", "priority", "HIGH",
                "assignedMemberIds", new long[]{leaderId, memberId},
                "subtasks", new Object[]{Map.of("title", "Default priority subtask")});
        String path = "/projects/" + projectId + "/long-todos";
        var created = call("POST", path, body, token);
        assertEquals(200, created.status);
        long id = created.body.path("longToDoId").asLong();
        var details = call("GET", "/long-todos/" + id, null, token);
        assertEquals(200, details.status);
        assertEquals(2, details.body.path("assignedMemberIds").size());
        assertEquals("MEDIUM", details.body.path("subtasks").get(0).path("priority").asText());
        assertEquals(200, call("POST", "/long-todos/" + id + "/subtasks", Map.of("title", "Later"), token).status);
        assertEquals(403, call("POST", "/long-todos/" + id + "/subtasks", Map.of("title", "Not allowed"), memberToken).status);
        assertEquals(2, call("GET", "/long-todos/" + id, null, token).body.path("subtasks").size());
        assertEquals(1, call("GET", path + "?priority=HIGH&done=false&sort=endDateTime", null, token).body.size());
        assertEquals(0, call("GET", path + "?priority=LOW", null, token).body.size());
        assertEquals(403, call("POST", path, body, memberToken).status);
        assertEquals(404, call("GET", "/long-todos/" + id, null, otherToken).status);
    }

    @Test void invalidCreateRollsBackAndUses400() throws Exception {
        String path = "/projects/" + projectId + "/meetings";
        var body = Map.of("title", "Invalid", "startDateTime", "2026-11-01T11:00",
                "endDateTime", "2026-11-01T10:00", "participantIds", new long[]{leaderId});
        assertEquals(400, call("POST", path, body, token).status);
        assertEquals(0, call("GET", path, null, token).body.size());
        assertEquals(400, call("POST", path, Map.of(), token).status);
        assertEquals(404, call("GET", "/projects/" + projectId + "/invitations", null, token).status);
    }

    @Test void parallelAddsCannotOverwriteRole() throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        register(email);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var gate = new java.util.concurrent.CountDownLatch(1);
            var a = pool.submit(() -> { gate.await(); return call("POST", "/projects/" + projectId + "/members", Map.of("email", email, "role", "MEMBER"), token); });
            var b = pool.submit(() -> { gate.await(); return call("POST", "/projects/" + projectId + "/members", Map.of("email", email, "role", "PROJECT_LEADER"), token); });
            gate.countDown();
            assertEquals(java.util.Set.of(200, 400), java.util.Set.of(a.get().status, b.get().status));
        } finally { pool.shutdownNow(); }
    }
}
