package org.planb;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MembersIntegrationTests {
    @LocalServerPort int port;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private String leader, member, outsider, memberEmail, outsiderEmail, path;
    record Reply(int status, JsonNode body) {}

    private Reply call(String method, String path, Object body, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Authorization", "Bearer " + token);
        if (body != null) request.header("Content-Type", "application/json");
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        var response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), json.readTree(response.body()));
    }

    private String register(String email) throws Exception {
        assertEquals(200, call("POST", "/users", Map.of("firstName", "Mia", "lastName", "Muster",
                "email", email, "password", "LocalTest2026!"), "").status());
        var login = call("POST", "/auth", Map.of("email", email, "password", "LocalTest2026!"), "");
        assertEquals(200, login.status());
        return login.body().path("token").asText();
    }

    @BeforeEach void setup() throws Exception {
        String prefix = UUID.randomUUID().toString();
        leader = register(prefix + "-leader@example.test");
        memberEmail = prefix + "-member@example.test";
        outsiderEmail = prefix + "-other@example.test";
        member = register(memberEmail);
        outsider = register(outsiderEmail);
        var project = call("POST", "/projects", Map.of("name", "Members-only test"), leader);
        assertEquals(200, project.status());
        long id = project.body().path("id").asLong();
        assertTrue(id > 0);
        path = "/projects/" + id + "/members";
        assertEquals(200, call("POST", path, Map.of("email", memberEmail, "role", "MEMBER"), leader).status());
    }

    @Test void memberListContainsAllRequiredFields() throws Exception {
        var reply = call("GET", path, null, member);
        assertEquals(200, reply.status());
        assertEquals(2, reply.body().size());
        for (JsonNode user : reply.body()) {
            for (String field : List.of("userId", "firstName", "lastName", "email", "role")) {
                assertTrue(user.hasNonNull(field), field);
            }
            assertFalse(user.has("password"));
        }
    }

    @Test void duplicateAndUnknownEmailHaveDomainErrors() throws Exception {
        var duplicate = call("POST", path, Map.of("email", memberEmail, "role", "PROJECT_LEADER"), leader);
        assertEquals(400, duplicate.status());
        assertEquals("user is already member", duplicate.body().path("message").asText());
        var missing = call("POST", path, Map.of("email", UUID.randomUUID() + "@example.test", "role", "MEMBER"), leader);
        assertEquals(404, missing.status());
        assertEquals("user not found", missing.body().path("message").asText());
        for (JsonNode user : call("GET", path, null, leader).body()) {
            if (memberEmail.equals(user.path("email").asText())) {
                assertEquals("MEMBER", user.path("role").asText());
            }
        }
    }

    @Test void onlyProjectLeaderCanAddAndOutsiderCannotRead() throws Exception {
        var denied = call("POST", path, Map.of("email", outsiderEmail, "role", "MEMBER"), member);
        assertEquals(403, denied.status());
        assertEquals("no permission", denied.body().path("message").asText());
        assertEquals(404, call("GET", path, null, outsider).status());
    }

    @Test void leaderCanDirectlyAddAnotherLeader() throws Exception {
        assertEquals(200, call("POST", path, Map.of("email", outsiderEmail, "role", "PROJECT_LEADER"), leader).status());
        assertEquals(3, call("GET", path, null, outsider).body().size());
    }

    @Test void concurrentAddsDoNotOverwriteMembership() throws Exception {
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        var gate = new java.util.concurrent.CountDownLatch(1);
        try {
            var a = pool.submit(() -> { gate.await(); return call("POST", path,
                    Map.of("email", outsiderEmail, "role", "MEMBER"), leader); });
            var b = pool.submit(() -> { gate.await(); return call("POST", path,
                    Map.of("email", outsiderEmail, "role", "PROJECT_LEADER"), leader); });
            gate.countDown();
            var statuses = List.of(a.get().status(), b.get().status()).stream().sorted().toList();
            assertEquals(List.of(200, 400), statuses);
            assertEquals(3, call("GET", path, null, leader).body().size());
        } finally {
            pool.shutdownNow();
        }
    }
}
