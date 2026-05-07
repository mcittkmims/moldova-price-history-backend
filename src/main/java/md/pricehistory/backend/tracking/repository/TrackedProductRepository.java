package md.pricehistory.backend.tracking.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import md.pricehistory.backend.product.entity.ProductEntity;
import md.pricehistory.backend.tracking.entity.TrackedProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrackedProductRepository extends JpaRepository<TrackedProductEntity, Long> {

    Page<TrackedProductEntity> findAllByUserAccount_UsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    Optional<TrackedProductEntity> findByUserAccount_UsernameAndProduct_Slug(String username, String slug);

    boolean existsByUserAccount_UsernameAndProduct_Slug(String username, String slug);

    long deleteByUserAccount_UsernameAndProduct_Slug(String username, String slug);

    @Query("""
            select distinct tp.product
            from TrackedProductEntity tp
            where tp.product.lastScrapedAt < :cutoff
            """)
    List<ProductEntity> findDistinctProductsLastScrapedBefore(@Param("cutoff") Instant cutoff);
}
