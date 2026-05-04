package md.pricehistory.backend.user;

import java.util.LinkedHashSet;
import java.util.Set;

public enum AppPermission {
    CATALOG_READ("catalog:read"),
    ACCOUNT_READ_SELF("account:read_self"),
    TRACKED_READ_OWN("tracked:read_own"),
    TRACKED_CREATE_OWN("tracked:create_own"),
    TRACKED_UPDATE_OWN("tracked:update_own"),
    TRACKED_DELETE_OWN("tracked:delete_own");

    private final String value;

    AppPermission(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Set<String> defaultUserPermissions() {
        LinkedHashSet<String> permissions = new LinkedHashSet<>();
        for (AppPermission permission : values()) {
            permissions.add(permission.value());
        }
        return permissions;
    }
}
