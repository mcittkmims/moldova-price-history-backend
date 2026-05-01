package md.pricehistory.backend.product.repository;

import md.pricehistory.backend.product.entity.StoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<StoreEntity, String> {
}
