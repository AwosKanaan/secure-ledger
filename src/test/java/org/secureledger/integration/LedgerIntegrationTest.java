package org.secureledger.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.secureledger.config.JwtProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LedgerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    private static final String GILBERT = "gilbert@gmail.com";
    private static final String BOB = "bob@ledger.test";
    private static final Map<String, String> PASSWORDS = Map.of(GILBERT, "gilbert123", BOB, "Ledger-Test-2026!");
    private static final String INVALID_TOKEN = "The access token is invalid or has expired";

    private static final String INVOICE = """
            {"amount":"99.00","currency":"EUR","description":"Invoice 7","counterpartyIban":"DE89370400440532013000"}""";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtProperties jwt;

    @Test
    void issuesToken() throws Exception {
        login(GILBERT, PASSWORDS.get(GILBERT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void rejectsBadCredentials() throws Exception {
        login(GILBERT, "wrong-password").andExpect(status().isUnauthorized());
        login("nobody@ledger.test", "gilbert123").andExpect(status().isUnauthorized());
    }

    @Test
    void ignoresEmailCase() throws Exception {
        login("Gilbert@Gmail.COM", PASSWORDS.get(GILBERT)).andExpect(status().isOk());
    }

    @Test
    void requiresToken() throws Exception {
        mvc.perform(get("/ledger/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
    }

    @Test
    void rejectsTamperedToken() throws Exception {
        String token = token(GILBERT);
        String tampered = token.substring(0, token.length() - 4) + (token.endsWith("AAAA") ? "BBBB" : "AAAA");

        history(tampered).andExpect(jsonPath("$.message").value(INVALID_TOKEN));
    }

    @Test
    void rejectsUnsignedToken() throws Exception {
        String header = base64Url("{\"alg\":\"none\"}");
        String payload = base64Url("{\"sub\":\"a11ce000-0000-4000-8000-000000000001\"}");

        history(header + "." + payload + ".").andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenWithoutSubject() throws Exception {
        String token = Jwts.builder()
                .issuer(jwt.issuer())
                .audience().add(jwt.audience()).and()
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwt.secret())), Jwts.SIG.HS256)
                .compact();

        history(token).andExpect(jsonPath("$.message").value(INVALID_TOKEN));
    }

    @Test
    void showsOnlyOwnTransactions() throws Exception {
        // only seed dates, other tests add more transactions
        history(token(GILBERT), "endDate", "2026-03-31")
                .andExpect(jsonPath("$.content[*].id", everyItem(startsWith("aaaaaaaa"))));
        history(token(BOB), "endDate", "2026-03-31")
                .andExpect(jsonPath("$.content[*].id", everyItem(startsWith("bbbbbbbb"))));
    }

    @Test
    void rejectsUnknownFields() throws Exception {
        create(token(GILBERT), null, INVOICE.replace("}", ",\"userId\":\"b0b00000-0000-4000-8000-000000000002\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown field 'userId'"));
    }

    @Test
    void filtersByDateRange() throws Exception {
        history(token(GILBERT), "startDate", "2026-02-10", "endDate", "2026-03-15")
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void sortsAndPages() throws Exception {
        // gilbert seed amounts ascending: 42.99, 300, 1250
        history(token(GILBERT), "endDate", "2026-03-31", "sort", "amount,asc", "size", "1", "page", "2")
                .andExpect(jsonPath("$.content[0].description").value("Transfer to savings"))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void capsPageSize() throws Exception {
        history(token(GILBERT), "size", "100000").andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void rejectsUnsupportedSort() throws Exception {
        history(token(GILBERT), "sort", "user.email").andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidDates() throws Exception {
        String token = token(GILBERT);
        history(token, "startDate", "2026-03-01", "endDate", "2026-02-01").andExpect(status().isBadRequest());
        history(token, "startDate", "not-a-date").andExpect(status().isBadRequest());
    }

    @Test
    void returnsJsonForUnknownRoutes() throws Exception {
        String token = token(GILBERT);
        mvc.perform(auth(get("/ledger/unknown"), token)).andExpect(jsonPath("$.status").value(404));
        mvc.perform(auth(delete("/ledger/transactions"), token)).andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void createsNormalisedTransaction() throws Exception {
        create(token(GILBERT), null, """
                {"amount":"125.5","currency":"eur","description":"  Invoice 42  ",
                 "counterpartyIban":"de89 3704 0044 0532 0130 00"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value("125.5000"))
                .andExpect(jsonPath("$.counterpartyIban").value("DE89370400440532013000"));
    }

    @Test
    void reportsFieldErrors() throws Exception {
        create(token(GILBERT), null, """
                {"amount":"-5.00","currency":"ABC","counterpartyIban":"DE89370400440532013001"}""")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("amount", "currency", "counterpartyIban")));
    }

    @Test
    void rejectsExcessPrecision() throws Exception {
        String token = token(GILBERT);
        create(token, null, INVOICE.replace("99.00", "10.12345")).andExpect(status().isUnprocessableEntity());
        create(token, null, INVOICE.replace("99.00", "100.5").replace("EUR", "JPY"))
                .andExpect(status().isUnprocessableEntity());
    }

    // Idempotency

    @Test
    void booksEveryRequestWithoutKey() throws Exception {
        String token = token(GILBERT);
        long before = count(token);

        create(token, null, INVOICE).andExpect(status().isCreated());
        create(token, null, INVOICE).andExpect(status().isCreated());

        assertThat(count(token)).isEqualTo(before + 2);
    }

    @Test
    void rejectsMalformedKey() throws Exception {
        mvc.perform(auth(post("/ledger/transactions"), token(GILBERT))
                        .header("Idempotency-Key", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON).content(INVOICE))
                .andExpect(status().isBadRequest());
    }

    @Test
    void replaysRetry() throws Exception {
        String token = token(GILBERT);
        UUID key = UUID.randomUUID();

        JsonNode first = body(create(token, key, INVOICE));
        JsonNode retry = body(create(token, key, INVOICE));

        assertThat(retry.get("id")).isEqualTo(first.get("id"));
    }

    @Test
    void rejectsReusedKey() throws Exception {
        String token = token(GILBERT);
        UUID key = UUID.randomUUID();
        create(token, key, INVOICE).andExpect(status().isCreated());

        create(token, key, INVOICE.replace("99.00", "999.00"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
    }

    @Test
    void booksConcurrentRetryOnce() throws Exception {
        String token = token(GILBERT);
        long before = count(token);
        UUID key = UUID.randomUUID();
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> send = () -> {
            start.await();
            return create(token, key, INVOICE).andReturn().getResponse().getStatus();
        };

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(send);
            var b = pool.submit(send);
            start.countDown();
            assertThat(List.of(a.get(), b.get())).containsAnyOf(201).allMatch(s -> s == 201 || s == 409);
        }
        assertThat(count(token)).isEqualTo(before + 1);
    }

    @Test
    void allowsListedOriginsOnly() throws Exception {
        mvc.perform(preflight("http://localhost:5173")).andExpect(status().isOk());
        mvc.perform(preflight("https://evil.example")).andExpect(status().isForbidden());
    }

    @Test
    void exposesHealth() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void documentsBearerAuth() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/ledger/auth/login'].post.security").isEmpty());
    }

    private ResultActions login(String email, String password) throws Exception {
        String body = json.createObjectNode().put("email", email).put("password", password).toString();
        return mvc.perform(post("/ledger/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String token(String email) throws Exception {
        return body(login(email, PASSWORDS.get(email)).andExpect(status().isOk())).get("accessToken").asText();
    }

    private ResultActions history(String token, String... params) throws Exception {
        MockHttpServletRequestBuilder request = get("/ledger/transactions");
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        return mvc.perform(auth(request, token));
    }

    private ResultActions create(String token, UUID key, String body) throws Exception {
        MockHttpServletRequestBuilder request = auth(post("/ledger/transactions"), token)
                .contentType(MediaType.APPLICATION_JSON).content(body);
        if (key != null) {
            request.header("Idempotency-Key", key.toString());
        }
        return mvc.perform(request);
    }

    private long count(String token) throws Exception {
        return body(history(token)).get("totalElements").asLong();
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private static MockHttpServletRequestBuilder preflight(String origin) {
        return options("/ledger/transactions")
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
    }

    private static String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes());
    }
}
