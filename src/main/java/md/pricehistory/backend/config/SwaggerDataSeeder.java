package md.pricehistory.backend.config;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import md.pricehistory.backend.user.AppPermission;
import md.pricehistory.backend.user.entity.UserAccountEntity;
import md.pricehistory.backend.user.service.UserService;
import md.pricehistory.backend.product.entity.ProductEntity;
import md.pricehistory.backend.product.entity.StoreEntity;
import md.pricehistory.backend.product.repository.ProductRepository;
import md.pricehistory.backend.product.repository.StoreRepository;
import md.pricehistory.backend.tracking.entity.TrackedProductEntity;
import md.pricehistory.backend.tracking.repository.TrackedProductRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("swagger")
public class SwaggerDataSeeder implements ApplicationRunner {

    private static final String DEMO_USER = "demo";
    private static final String DEMO_PASSWORD = "demodemo";

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final TrackedProductRepository trackedProductRepository;
    private final PasswordEncoder passwordEncoder;

    public SwaggerDataSeeder(
            StoreRepository storeRepository,
            ProductRepository productRepository,
            UserService userService,
            TrackedProductRepository trackedProductRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.userService = userService;
        this.trackedProductRepository = trackedProductRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Instant now = Instant.now();

        StoreEntity darwin = store("darwin", "Darwin", now);
        StoreEntity enter = store("enter", "Enter", now);
        StoreEntity maximum = store("maximum", "Maximum", now);
        storeRepository.saveAll(List.of(darwin, enter, maximum));

        List<ProductEntity> products = List.of(
                product(darwin, "iphone-15-pro", "iphone-15-pro-darwin", "iPhone 15 Pro", "Apple", "Smartphones", "https://darwin.md/iphone15pro", now),
                product(darwin, "samsung-s24", "samsung-s24-darwin", "Samsung Galaxy S24", "Samsung", "Smartphones", "https://darwin.md/s24", now),
                product(darwin, "macbook-air-m3", "macbook-air-m3-darwin", "MacBook Air M3", "Apple", "Laptops", "https://darwin.md/macbookairm3", now),
                product(darwin, "sony-wh1000xm5", "sony-wh1000xm5-darwin", "Sony WH-1000XM5", "Sony", "Headphones", "https://darwin.md/wh1000xm5", now),
                product(darwin, "ipad-pro-12", "ipad-pro-12-darwin", "iPad Pro 12.9\"", "Apple", "Tablets", "https://darwin.md/ipadpro12", now),
                product(enter, "lg-oled-55", "lg-oled-55-enter", "LG OLED 55\" C3", "LG", "TVs", "https://enter.online/lg-oled-55", now),
                product(enter, "dyson-v15", "dyson-v15-enter", "Dyson V15 Detect", "Dyson", "Vacuum Cleaners", "https://enter.online/dysonv15", now),
                product(enter, "bose-qc45", "bose-qc45-enter", "Bose QuietComfort 45", "Bose", "Headphones", "https://enter.online/boseqc45", now),
                product(enter, "dell-xps-15", "dell-xps-15-enter", "Dell XPS 15", "Dell", "Laptops", "https://enter.online/dellxps15", now),
                product(enter, "gopro-hero12", "gopro-hero12-enter", "GoPro HERO12 Black", "GoPro", "Cameras", "https://enter.online/gopro12", now),
                product(maximum, "ps5-slim", "ps5-slim-maximum", "PlayStation 5 Slim", "Sony", "Gaming", "https://maximum.md/ps5slim", now),
                product(maximum, "xbox-series-x", "xbox-series-x-maximum", "Xbox Series X", "Microsoft", "Gaming", "https://maximum.md/xboxseriesx", now),
                product(maximum, "asus-rog-laptop", "asus-rog-laptop-maximum", "ASUS ROG Strix G16", "ASUS", "Laptops", "https://maximum.md/rogstrix", now),
                product(maximum, "samsung-qled-65", "samsung-qled-65-maximum", "Samsung QLED 65\" Q80C", "Samsung", "TVs", "https://maximum.md/qled65", now),
                product(maximum, "nikon-z50", "nikon-z50-maximum", "Nikon Z50 II", "Nikon", "Cameras", "https://maximum.md/nikonz50", now)
        );
        productRepository.saveAll(products);

        UserAccountEntity user = new UserAccountEntity();
        user.setUsername(DEMO_USER);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        user.setPermissions(new LinkedHashSet<>(AppPermission.defaultUserPermissions()));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userService.save(user);

        List<TrackedProductEntity> tracked = products.stream().map(p -> {
            TrackedProductEntity t = new TrackedProductEntity();
            t.setUserAccount(user);
            t.setProduct(p);
            t.setCreatedAt(now);
            return t;
        }).toList();
        trackedProductRepository.saveAll(tracked);
    }

    private StoreEntity store(String id, String name, Instant now) {
        StoreEntity s = new StoreEntity();
        s.setId(id);
        s.setName(name);
        s.setCreatedAt(now);
        s.setUpdatedAt(now);
        return s;
    }

    private ProductEntity product(StoreEntity store, String itemId, String slug, String title,
                                   String brand, String category, String url, Instant now) {
        ProductEntity p = new ProductEntity();
        p.setStore(store);
        p.setItemId(itemId);
        p.setSlug(slug);
        p.setTitle(title);
        p.setBrand(brand);
        p.setCategory(category);
        p.setCurrency("MDL");
        p.setAvailability("in_stock");
        p.setUrl(url);
        p.setImageTone("light");
        p.setRawPayload("{}");
        p.setDetailComplete(true);
        p.setLastScrapedAt(now);
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        return p;
    }
}
