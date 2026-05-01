package md.pricehistory.backend.product.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import md.pricehistory.backend.product.entity.ProductPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductPriceRepository extends JpaRepository<ProductPriceEntity, Long> {

    Optional<ProductPriceEntity> findByProduct_IdAndScrapedAt(Long productId, Instant scrapedAt);

    List<ProductPriceEntity> findByProduct_IdInOrderByScrapedAtAsc(Collection<Long> productIds);

    Optional<ProductPriceEntity> findTopByProduct_IdAndCurrentPriceIsNotNullOrderByScrapedAtDesc(Long productId);
}
