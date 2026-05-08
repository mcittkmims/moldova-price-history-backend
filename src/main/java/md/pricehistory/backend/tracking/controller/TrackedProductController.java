package md.pricehistory.backend.tracking.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import md.pricehistory.backend.tracking.dto.TrackedProductStatusResponse;
import md.pricehistory.backend.tracking.dto.TrackedProductsPageResponse;
import md.pricehistory.backend.tracking.service.TrackedProductService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/tracked")
public class TrackedProductController {

    private final TrackedProductService trackedProductService;

    public TrackedProductController(TrackedProductService trackedProductService) {
        this.trackedProductService = trackedProductService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('tracked:read_own')")
    public TrackedProductsPageResponse listTracked(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(name = "page_size", defaultValue = "12") @Min(1) @Max(48) int pageSize
    ) {
        return trackedProductService.listTracked(authentication.getName(), page, pageSize);
    }

    @GetMapping("/{slug}")
    @PreAuthorize("hasAuthority('tracked:read_own')")
    public TrackedProductStatusResponse trackedStatus(
            @PathVariable String slug,
            Authentication authentication
    ) {
        return new TrackedProductStatusResponse(
                trackedProductService.trackedStatus(authentication.getName(), slug)
        );
    }

    @PutMapping("/{slug}")
    @PreAuthorize("hasAuthority('tracked:create_own')")
    public TrackedProductStatusResponse track(
            @PathVariable String slug,
            Authentication authentication
    ) {
        trackedProductService.track(authentication.getName(), slug);
        return new TrackedProductStatusResponse(true);
    }

    @DeleteMapping("/{slug}")
    @PreAuthorize("hasAuthority('tracked:delete_own')")
    public TrackedProductStatusResponse untrack(
            @PathVariable String slug,
            Authentication authentication
    ) {
        trackedProductService.untrack(authentication.getName(), slug);
        return new TrackedProductStatusResponse(false);
    }
}
