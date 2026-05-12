package md.pricehistory.backend.tracking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.Instant;
import md.pricehistory.backend.product.entity.ProductEntity;
import md.pricehistory.backend.product.repository.ProductPriceRepository;
import md.pricehistory.backend.product.repository.ProductRepository;
import md.pricehistory.backend.product.entity.StoreEntity;
import md.pricehistory.backend.product.repository.StoreRepository;
import md.pricehistory.backend.tracking.repository.TrackedProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("local")
class TrackedProductControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TrackedProductRepository trackedProductRepository;

    @Autowired
    private ProductPriceRepository productPriceRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        trackedProductRepository.deleteAllInBatch();
        productPriceRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
    }

    @Test
    void authenticatedUserCanTrackListAndUntrackProduct() throws Exception {
        ProductEntity product = saveProduct("iphone-15-enter-123");
        Cookie authCookie = register("tracked_user_test");
        String csrfToken = fetchCsrfToken(authCookie);

        mockMvc.perform(put("/api/me/tracked/iphone-15-enter-123")
                        .header("X-CSRF-Token", csrfToken)
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracked").value(true));

        mockMvc.perform(get("/api/me/tracked").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].slug").value(product.getSlug()))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(12));

        mockMvc.perform(get("/api/me/tracked/iphone-15-enter-123").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracked").value(true));

        mockMvc.perform(delete("/api/me/tracked/iphone-15-enter-123")
                        .header("X-CSRF-Token", csrfToken)
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracked").value(false));

        mockMvc.perform(get("/api/me/tracked/iphone-15-enter-123").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracked").value(false));
    }

    @Test
    void trackedListSupportsPagination() throws Exception {
        Cookie authCookie = register("tracked_pagination_test");
        String csrfToken = fetchCsrfToken(authCookie);

        for (int index = 1; index <= 13; index += 1) {
            String slug = "iphone-15-enter-12" + index;
            saveProduct(slug, "12" + index);

            mockMvc.perform(put("/api/me/tracked/" + slug)
                            .header("X-CSRF-Token", csrfToken)
                            .cookie(authCookie))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/me/tracked?page=1&page_size=12").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(12))
                .andExpect(jsonPath("$.totalItems").value(13))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items.length()").value(12));

        mockMvc.perform(get("/api/me/tracked?page=2&page_size=12").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    private Cookie register(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "password123"
                                }
                                """.formatted(username)))
                .andExpect(status().isCreated())
                .andReturn();

        return result.getResponse().getCookie("pricehistory_access");
    }

    private String fetchCsrfToken(Cookie authCookie) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/csrf").cookie(authCookie))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        return payload.get("token").asText();
    }

    private ProductEntity saveProduct(String slug) {
        return saveProduct(slug, "123");
    }

    private ProductEntity saveProduct(String slug, String itemId) {
        StoreEntity store = storeRepository.findById("enter").orElseGet(() -> {
            StoreEntity createdStore = new StoreEntity();
            createdStore.setId("enter");
            createdStore.setName("Enter");
            createdStore.setCreatedAt(Instant.parse("2026-05-09T10:00:00Z"));
            createdStore.setUpdatedAt(Instant.parse("2026-05-09T10:00:00Z"));
            return storeRepository.save(createdStore);
        });
        ProductEntity product = new ProductEntity();
        product.setStore(store);
        product.setItemId(itemId);
        product.setSlug(slug);
        product.setTitle("Apple iPhone 15");
        product.setCategory("Phones");
        product.setCurrency("MDL");
        product.setAvailability("In stock");
        product.setUrl("https://enter.online/product/" + itemId);
        product.setImageTone("#2f5d50");
        product.setImageUrl("https://cdn.example.com/iphone.jpg");
        product.setSpecs(java.util.List.of("Brand: Apple", "SKU: " + itemId));
        product.setLastScrapedAt(Instant.parse("2026-05-09T10:00:00Z"));
        product.setRawPayload("{\"source_id\":\"" + itemId + "\"}");
        product.setDetailComplete(true);
        product.setCreatedAt(Instant.parse("2026-05-09T10:00:00Z"));
        product.setUpdatedAt(Instant.parse("2026-05-09T10:00:00Z"));
        return productRepository.save(product);
    }

}
