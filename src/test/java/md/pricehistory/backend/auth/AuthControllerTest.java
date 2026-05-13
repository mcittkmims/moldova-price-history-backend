package md.pricehistory.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ActiveProfiles("local")
class AuthControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void registerSetsJwtCookieAndReturnsSessionPayload() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "demo_user_auth_test",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("demo_user_auth_test"))
                .andReturn();

        Cookie authCookie = result.getResponse().getCookie("pricehistory_access");
        assertThat(authCookie).isNotNull();
        assertThat(authCookie.isHttpOnly()).isTrue();
        assertThat(authCookie.getSecure()).isFalse();

        Jwt jwt = jwtDecoder.decode(authCookie.getValue());
        assertThat(jwt.getSubject()).isEqualTo("demo_user_auth_test");
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getClaimAsStringList("permissions"))
                .contains("tracked:create_own", "tracked:delete_own");
        assertThat(jwt.getExpiresAt()).isNotNull();
        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }

    @Test
    void loginReturnsSessionForExistingUser() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "demo_user_login_test",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "demo_user_login_test",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("demo_user_login_test"))
                .andExpect(jsonPath("$.user.permissions").isArray())
                .andReturn();

        Cookie authCookie = loginResult.getResponse().getCookie("pricehistory_access");
        assertThat(authCookie).isNotNull();

        mockMvc.perform(get("/api/auth/session").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("demo_user_login_test"))
                .andExpect(jsonPath("$.user.permissions").isArray());
    }

    @Test
    void logoutClearsAuthCookie() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "demo_user_logout_test",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        Cookie authCookie = registerResult.getResponse().getCookie("pricehistory_access");
        String csrfToken = fetchCsrfToken(authCookie);

        mockMvc.perform(post("/api/auth/logout")
                        .header("X-CSRF-Token", csrfToken)
                        .cookie(authCookie))
                .andExpect(status().isNoContent())
                .andExpect((result) -> {
                    Cookie clearedCookie = result.getResponse().getCookie("pricehistory_access");
                    assertThat(clearedCookie).isNotNull();
                    assertThat(clearedCookie.getMaxAge()).isZero();
                });
    }

    @Test
    void registerReturnsFriendlyValidationMessageForShortPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "demo_user_validation_test",
                                  "password": "adrian"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must be between 8 and 72 characters"));
    }

    @Test
    void csrfEndpointReturnsSignedTokenForAuthenticatedSession() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "demo_user_csrf_test",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        Cookie authCookie = registerResult.getResponse().getCookie("pricehistory_access");

        mockMvc.perform(get("/api/csrf").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-Token"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    private String fetchCsrfToken(Cookie authCookie) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/csrf").cookie(authCookie))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        return payload.get("token").asText();
    }
}
