package md.pricehistory.backend.tracking.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import md.pricehistory.backend.user.entity.UserAccountEntity;
import md.pricehistory.backend.user.service.UserService;
import md.pricehistory.backend.product.entity.ProductEntity;
import md.pricehistory.backend.product.service.ProductService;
import md.pricehistory.backend.common.ApiException;
import md.pricehistory.backend.config.PriceHistoryProperties;
import md.pricehistory.backend.product.dto.ProductResponse;
import md.pricehistory.backend.tracking.dto.TrackedProductsPageResponse;
import md.pricehistory.backend.tracking.entity.TrackedProductEntity;
import md.pricehistory.backend.tracking.repository.TrackedProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrackedProductService {

    private static final Logger logger = LoggerFactory.getLogger(TrackedProductService.class);

    private final TrackedProductRepository trackedProductRepository;
    private final UserService userService;
    private final ProductService productService;
    private final PriceHistoryProperties properties;
    private final Clock clock;

    @Autowired
    public TrackedProductService(
            TrackedProductRepository trackedProductRepository,
            UserService userService,
            ProductService productService,
            PriceHistoryProperties properties
    ) {
        this(trackedProductRepository, userService, productService, properties, Clock.systemUTC());
    }

    TrackedProductService(
            TrackedProductRepository trackedProductRepository,
            UserService userService,
            ProductService productService,
            PriceHistoryProperties properties,
            Clock clock
    ) {
        this.trackedProductRepository = trackedProductRepository;
        this.userService = userService;
        this.productService = productService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TrackedProductsPageResponse listTracked(String username, int page, int pageSize) {
        List<ProductResponse> trackedProducts = new ArrayList<>();
        Page<TrackedProductEntity> trackedPage = trackedProductRepository
                .findAllByUserAccount_UsernameOrderByCreatedAtDesc(
                        username,
                        PageRequest.of(page - 1, pageSize)
                );

        for (TrackedProductEntity trackedProduct : trackedPage.getContent()) {
            productService.findBySlug(trackedProduct.getProduct().getSlug())
                    .ifPresent(trackedProducts::add);
        }

        return new TrackedProductsPageResponse(
                trackedPage.getNumber() + 1,
                trackedPage.getSize(),
                trackedPage.getTotalElements(),
                trackedPage.getTotalPages(),
                trackedProducts
        );
    }

    @Transactional(readOnly = true)
    public boolean trackedStatus(String username, String slug) {
        if (!productService.findBySlug(slug).isPresent()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Product was not found");
        }
        return trackedProductRepository.existsByUserAccount_UsernameAndProduct_Slug(username, slug);
    }

    @Transactional
    public ProductResponse track(String username, String slug) {
        UserAccountEntity user = userService.findByUsername(username);
        ProductEntity product = productService.findEntityBySlug(slug)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product was not found"));

        trackedProductRepository.findByUserAccount_UsernameAndProduct_Slug(username, slug)
                .orElseGet(() -> {
                    TrackedProductEntity trackedProduct = new TrackedProductEntity();
                    trackedProduct.setUserAccount(user);
                    trackedProduct.setProduct(product);
                    trackedProduct.setCreatedAt(clock.instant());
                    return trackedProductRepository.save(trackedProduct);
                });

        return productService.findBySlug(slug)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tracked product view is unavailable"));
    }

    @Transactional
    public ProductResponse untrack(String username, String slug) {
        ProductResponse product = productService.findBySlug(slug)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product was not found"));
        trackedProductRepository.deleteByUserAccount_UsernameAndProduct_Slug(username, slug);
        return product;
    }

    @Transactional
    public int refreshStaleTrackedProducts() {
        Instant cutoff = clock.instant().minus(properties.tracking().refreshInterval());
        List<ProductEntity> staleProducts = trackedProductRepository.findDistinctProductsLastScrapedBefore(cutoff);
        int refreshedCount = 0;

        for (ProductEntity product : staleProducts) {
            try {
                productService.productByUrl(product.getUrl());
                refreshedCount++;
            } catch (RuntimeException exception) {
                logger.warn("Could not refresh tracked product {} from {}", product.getSlug(), product.getUrl(), exception);
            }
        }

        return refreshedCount;
    }
}
