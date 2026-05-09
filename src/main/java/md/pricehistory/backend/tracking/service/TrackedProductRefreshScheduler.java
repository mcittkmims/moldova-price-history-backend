package md.pricehistory.backend.tracking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!swagger")
public class TrackedProductRefreshScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TrackedProductRefreshScheduler.class);

    private final TrackedProductService trackedProductService;

    public TrackedProductRefreshScheduler(TrackedProductService trackedProductService) {
        this.trackedProductService = trackedProductService;
    }

    @Scheduled(
            fixedDelayString = "${price-history.tracking.scheduler-interval:PT1H}",
            initialDelayString = "${price-history.tracking.scheduler-interval:PT1H}"
    )
    public void refreshStaleTrackedProducts() {
        int refreshedCount = trackedProductService.refreshStaleTrackedProducts();
        if (refreshedCount > 0) {
            logger.info("Refreshed {} stale tracked products", refreshedCount);
        }
    }
}
