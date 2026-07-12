package com.pushpay.gen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Set;

/**
 * Single source of truth for which Excel headers the utility accepts (design.md §4).
 * Bound from application.yml, overridable at runtime with
 * --spring.config.additional-location=file:./pushpay-columns.yml (no rebuild needed).
 *
 * refColumn/nameColumn/templateIdColumn/emailColumns are fixed-ROLE columns (join key,
 * mandatory identity field, template-client selector, email-format checks) - the code
 * always treats one column as "the" join key etc, but WHICH header text plays that role
 * is fully configurable here, not hardcoded in BatchValidator/SqlGenerator (design.md §3).
 */
@ConfigurationProperties(prefix = "pushpay")
public record ColumnsConfig(ClientSheet client, BeneSheet bene) {

    public ColumnsConfig {
        if (client == null) {
            throw new IllegalStateException("Invalid configuration: pushpay.client is not configured.");
        }
        if (bene == null) {
            throw new IllegalStateException("Invalid configuration: pushpay.bene is not configured.");
        }
        client.validate("pushpay.client");
        bene.validate("pushpay.bene");
    }

    public record ClientSheet(String refColumn, String nameColumn, String templateIdColumn,
                               List<String> columns, List<String> mandatory, List<String> emailColumns) {
        void validate(String prefix) {
            requireText(prefix, "refColumn", refColumn);
            requireText(prefix, "nameColumn", nameColumn);
            requireText(prefix, "templateIdColumn", templateIdColumn);
            Set<String> fixedColumns = Set.of(refColumn, nameColumn, templateIdColumn);
            List<String> cols = safe(columns);
            validateNoOverlap(prefix, "columns", fixedColumns, cols);
            validateSubset(prefix, "mandatory", fixedColumns, cols, safe(mandatory));
            validateSubset(prefix, "emailColumns", fixedColumns, cols, safe(emailColumns));
        }
    }

    public record BeneSheet(String refColumn, List<String> columns, List<String> mandatory) {
        void validate(String prefix) {
            requireText(prefix, "refColumn", refColumn);
            Set<String> fixedColumns = Set.of(refColumn);
            List<String> cols = safe(columns);
            validateNoOverlap(prefix, "columns", fixedColumns, cols);
            validateSubset(prefix, "mandatory", fixedColumns, cols, safe(mandatory));
        }
    }

    private static List<String> safe(List<String> list) {
        return list == null ? List.of() : list;
    }

    private static void requireText(String prefix, String key, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Invalid configuration: " + prefix + "." + key + " must be set.");
        }
    }

    /** The fixed-role columns (refColumn etc.) must never also appear in 'columns' - see SqlGenerator. */
    private static void validateNoOverlap(String prefix, String columnsKey, Set<String> fixedColumns,
                                           List<String> columns) {
        for (String fixed : fixedColumns) {
            if (columns.contains(fixed)) {
                throw new IllegalStateException("Invalid configuration: " + prefix + "." + columnsKey
                        + " lists '" + fixed + "' but it is already one of this sheet's fixed columns "
                        + "(refColumn/nameColumn/templateIdColumn) - remove it from " + prefix + "." + columnsKey
                        + ", it is handled separately and must never be duplicated there.");
            }
        }
    }

    /** Every entry of listKey must be either a fixed column or a member of 'columns'. */
    private static void validateSubset(String prefix, String listKey, Set<String> fixedColumns,
                                        List<String> columns, List<String> list) {
        for (String field : list) {
            if (columns.contains(field)) {
                continue;
            }
            String hint = fixedColumns.contains(field)
                    ? " '" + field + "' is already always required by default (it's one of this sheet's fixed "
                            + "columns) - remove it from " + prefix + "." + listKey + ", it doesn't belong there."
                    : " Add it to " + prefix + ".columns too, or remove it from " + prefix + "." + listKey + ".";
            throw new IllegalStateException("Invalid configuration: " + prefix + "." + listKey + " lists '" + field
                    + "' but it is not present in " + prefix + ".columns." + hint);
        }
    }
}
