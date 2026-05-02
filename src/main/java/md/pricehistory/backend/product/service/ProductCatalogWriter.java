package md.pricehistory.backend.product.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import md.pricehistory.backend.product.entity.ProductEntity;
import md.pricehistory.backend.product.entity.ProductPriceEntity;
import md.pricehistory.backend.product.entity.StoreEntity;
import md.pricehistory.backend.product.model.ProductSlug;
import md.pricehistory.backend.product.model.ScrapeKind;
import md.pricehistory.backend.product.model.ScrapedProductRecord;
import md.pricehistory.backend.product.repository.ProductPriceRepository;
import md.pricehistory.backend.product.repository.ProductRepository;
import md.pricehistory.backend.product.repository.StoreRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductCatalogWriter {

    private final ProductRepository productRepository;
    private final ProductPriceRepository productPriceRepository;
    private final StoreRepository storeRepository;

    public ProductCatalogWriter(
            ProductRepository productRepository,
            ProductPriceRepository productPriceRepository,
            StoreRepository storeRepository
    ) {
        this.productRepository = productRepository;
        this.productPriceRepository = productPriceRepository;
        this.storeRepository = storeRepository;
    }

    @Async("catalogWriteExecutor")
    @Transactional
    public void persistAsync(List<ScrapedProductRecord> records) {
        if (records.isEmpty()) {
            return;
        }

        Map<String, ScrapedProductRecord> latestById = new LinkedHashMap<>();
        for (ScrapedProductRecord record : records) {
            latestById.merge(record.id(), record, this::latestRecord);
        }

        Map<String, ProductEntity> productsById = loadProducts(latestById.keySet());
        Map<String, StoreEntity> storesById = loadStores(latestById.values());
        List<ProductEntity> newProducts = new ArrayList<>();

        for (ScrapedProductRecord record : latestById.values()) {
            ProductEntity product = productsById.get(record.id());
            if (product == null) {
                product = new ProductEntity();
                product.setStore(storesById.get(record.storeId()));
                product.setItemId(record.itemId());
                productsById.put(record.id(), product);
                newProducts.add(product);
            }
            mergeProduct(product, record, storesById.get(record.storeId()));
        }

        if (!newProducts.isEmpty()) {
            productRepository.saveAll(newProducts);
        }

        for (ScrapedProductRecord record : latestById.values()) {
            upsertPrice(productsById.get(record.id()), record);
        }
    }

    private Map<String, ProductEntity> loadProducts(Collection<String> ids) {
        Map<String, ProductEntity> productsById = new LinkedHashMap<>();
        for (ProductEntity product : productRepository.findByNaturalKeyIn(ids)) {
            productsById.put(productKey(product), product);
        }
        return productsById;
    }

    private Map<String, StoreEntity> loadStores(Collection<ScrapedProductRecord> records) {
        Map<String, StoreEntity> storesById = new LinkedHashMap<>();
        for (ScrapedProductRecord record : records) {
            storesById.computeIfAbsent(record.storeId(), ignored -> ensureStore(record));
        }
        return storesById;
    }

    private StoreEntity ensureStore(ScrapedProductRecord record) {
        return storeRepository.findById(record.storeId())
                .map((store) -> mergeStore(store, record.store()))
                .orElseGet(() -> createStore(record.storeId(), record.store()));
    }

    private StoreEntity mergeStore(StoreEntity store, String storeName) {
        if (storeName.equals(store.getName())) {
            return store;
        }
        store.setName(storeName);
        store.setUpdatedAt(Instant.now());
        return storeRepository.save(store);
    }

    private StoreEntity createStore(String storeId, String storeName) {
        Instant now = Instant.now();
        StoreEntity store = new StoreEntity();
        store.setId(storeId);
        store.setName(storeName);
        store.setCreatedAt(now);
        store.setUpdatedAt(now);
        return storeRepository.save(store);
    }

    private void mergeProduct(ProductEntity product, ScrapedProductRecord record, StoreEntity store) {
        Instant now = Instant.now();
        if (product.getCreatedAt() == null) {
            product.setCreatedAt(now);
        }
        product.setStore(store);
        product.setItemId(record.itemId());
        if (product.getSlug() == null || product.getSlug().isBlank()) {
            product.setSlug(ProductSlug.stableSlug(record.title(), record.storeId(), record.itemId()));
        }

        if (record.scrapeKind() == ScrapeKind.DETAIL) {
            if (shouldApplySnapshot(product, record)) {
                applyFullSnapshot(product, record);
            }
            product.setDetailComplete(true);
            product.setUpdatedAt(now);
            return;
        }

        if (!product.isDetailComplete()) {
            if (shouldApplySnapshot(product, record)) {
                applyFullSnapshot(product, record);
                product.setUpdatedAt(now);
            }
            return;
        }

        if (applySearchRefresh(product, record)) {
            product.setUpdatedAt(now);
        }
    }

    private void applyFullSnapshot(ProductEntity product, ScrapedProductRecord record) {
        product.setTitle(record.title());
        product.setBrand(record.brand());
        product.setCategory(record.category());
        product.setCurrency(record.currency());
        product.setAvailability(record.availability());
        product.setUrl(record.url());
        product.setImageTone(record.imageTone());
        product.setImageUrl(record.imageUrl());
        product.setSpecs(record.specs());
        product.setLastScrapedAt(record.lastScrapedAt());
        product.setRawPayload(record.rawPayload());
    }

    private boolean applySearchRefresh(ProductEntity product, ScrapedProductRecord record) {
        boolean changed = false;

        if (shouldApplySnapshot(product, record)) {
            changed |= assignIfDifferent(product.getAvailability(), record.availability(), product::setAvailability);
            changed |= assignIfDifferent(product.getLastScrapedAt(), record.lastScrapedAt(), product::setLastScrapedAt);
        }

        changed |= fillMissingText(product.getTitle(), record.title(), product::setTitle);
        changed |= fillMissingText(product.getBrand(), record.brand(), product::setBrand);
        changed |= fillMissingText(product.getCategory(), record.category(), product::setCategory);
        changed |= fillMissingText(product.getCurrency(), record.currency(), product::setCurrency);
        changed |= fillMissingText(product.getUrl(), record.url(), product::setUrl);
        changed |= fillMissingText(product.getImageTone(), record.imageTone(), product::setImageTone);
        changed |= fillMissingText(product.getImageUrl(), record.imageUrl(), product::setImageUrl);
        changed |= fillMissingList(product.getSpecs(), record.specs(), product::setSpecs);
        changed |= fillMissingText(product.getRawPayload(), record.rawPayload(), product::setRawPayload);
        return changed;
    }

    private boolean fillMissingText(String currentValue, String incomingValue, Consumer<String> setter) {
        if (StringUtils.hasText(currentValue) || !StringUtils.hasText(incomingValue)) {
            return false;
        }
        setter.accept(incomingValue);
        return true;
    }

    private boolean fillMissingList(List<String> currentValue, List<String> incomingValue, Consumer<List<String>> setter) {
        if (currentValue != null && !currentValue.isEmpty()) {
            return false;
        }
        if (incomingValue == null || incomingValue.isEmpty()) {
            return false;
        }
        setter.accept(incomingValue);
        return true;
    }

    private <T> boolean assignIfDifferent(T currentValue, T incomingValue, Consumer<T> setter) {
        if (Objects.equals(currentValue, incomingValue)) {
            return false;
        }
        setter.accept(incomingValue);
        return true;
    }

    private void upsertPrice(ProductEntity product, ScrapedProductRecord record) {
        ProductPriceEntity price = productPriceRepository.findByProduct_IdAndScrapedAt(product.getId(), record.lastScrapedAt())
                .orElseGet(ProductPriceEntity::new);
        price.setProduct(product);
        price.setScrapedAt(record.lastScrapedAt());
        price.setCurrentPrice(record.currentPrice());
        price.setPreviousPrice(record.previousPrice());
        price.setCurrency(record.currency());
        productPriceRepository.save(price);
    }

    private ScrapedProductRecord latestRecord(ScrapedProductRecord left, ScrapedProductRecord right) {
        return right.lastScrapedAt().isAfter(left.lastScrapedAt()) ? right : left;
    }

    private boolean shouldApplySnapshot(ProductEntity product, ScrapedProductRecord record) {
        Instant currentLastScrapedAt = product.getLastScrapedAt();
        return currentLastScrapedAt == null || !record.lastScrapedAt().isBefore(currentLastScrapedAt);
    }

    private String productKey(ProductEntity product) {
        return product.getStoreId() + ":" + product.getItemId();
    }
}
