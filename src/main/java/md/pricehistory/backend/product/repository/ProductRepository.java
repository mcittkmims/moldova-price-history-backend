package md.pricehistory.backend.product.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import md.pricehistory.backend.product.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    @Query("""
            select p
            from ProductEntity p
            join fetch p.store s
            where concat(s.id, ':', p.itemId) in :keys
            """)
    List<ProductEntity> findByNaturalKeyIn(@Param("keys") Collection<String> keys);

    Optional<ProductEntity> findByStore_IdAndItemId(String storeId, String itemId);

    @Query("""
            select p
            from ProductEntity p
            join fetch p.store s
            where p.slug = :slug
            """)
    Optional<ProductEntity> findBySlug(@Param("slug") String slug);

    @Query("""
            select p
            from ProductEntity p
            order by p.slug asc
            """)
    Page<ProductEntity> findAllForSitemap(Pageable pageable);
}
