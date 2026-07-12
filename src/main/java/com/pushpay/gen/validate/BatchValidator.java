package com.pushpay.gen.validate;

import com.pushpay.gen.config.ColumnsConfig;
import com.pushpay.gen.excel.ExcelReader;
import com.pushpay.gen.excel.ExcelRow;
import com.pushpay.gen.excel.SheetData;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * All business validation rules from design.md §5.3. If ANY error is found, the caller
 * must print all of them and generate nothing (exit code 2).
 *
 * Column names are never hardcoded here - refColumn/nameColumn/templateIdColumn/
 * emailColumns all come from ColumnsConfig, same as SqlGenerator, so renaming a header
 * in application.yml is a config-only change (design.md §4/§8).
 */
@Component
public class BatchValidator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    /**
     * @param defaultTemplateClientId fallback used for Clients rows that leave the
     *                                configured template-id column blank (from --template-client);
     *                                null if the CLI flag wasn't given.
     */
    public List<ValidationError> validate(ExcelReader.ParsedWorkbook workbook, ColumnsConfig config,
                                           Long defaultTemplateClientId) {
        List<ValidationError> errors = new ArrayList<>();

        SheetData clients = workbook.clients();
        SheetData benes = workbook.beneficiaries();

        String clientRefColumn = config.client().refColumn();
        String beneRefColumn = config.bene().refColumn();

        Set<String> clientAllowedHeaders = new HashSet<>(config.client().columns());
        clientAllowedHeaders.add(clientRefColumn);
        clientAllowedHeaders.add(config.client().nameColumn());
        clientAllowedHeaders.add(config.client().templateIdColumn());

        Set<String> beneAllowedHeaders = new HashSet<>(config.bene().columns());
        beneAllowedHeaders.add(beneRefColumn);

        validateHeaders(clients, clientAllowedHeaders, clientRefColumn, errors);
        validateHeaders(benes, beneAllowedHeaders, beneRefColumn, errors);

        Set<String> validClientRefs = validateClients(clients, config, defaultTemplateClientId, errors);
        validateBeneficiaries(benes, config, validClientRefs, errors);

        return errors;
    }

    private void validateHeaders(SheetData sheet, Set<String> allowedHeaders, String refColumn,
                                  List<ValidationError> errors) {
        for (String header : sheet.headers()) {
            if (!allowedHeaders.contains(header)) {
                errors.add(new ValidationError(sheet.sheetName(), null,
                        "column '" + header + "' is not a recognized column name for this sheet. "
                                + "Check for typos or an extra/renamed header; remove it, or ask the dev "
                                + "team to add support for it if this is meant to be a new column."));
            }
        }
        if (!sheet.headers().contains(refColumn)) {
            errors.add(new ValidationError(sheet.sheetName(), null,
                    "this sheet is missing the required '" + refColumn + "' column."));
        }
    }

    /** Returns the set of valid (non-blank, present) client_ref values, for orphan-bene checking. */
    private Set<String> validateClients(SheetData clients, ColumnsConfig config, Long defaultTemplateClientId,
                                         List<ValidationError> errors) {
        String refColumn = config.client().refColumn();
        String nameColumn = config.client().nameColumn();

        Set<String> validRefs = new HashSet<>();
        boolean hasClientRefHeader = clients.headers().contains(refColumn);
        Map<String, Integer> seenRefs = new HashMap<>();
        Map<String, Integer> seenNames = new HashMap<>();

        for (ExcelRow row : clients.rows()) {
            if (hasClientRefHeader) {
                String ref = row.get(refColumn);
                if (ref == null || ref.isBlank()) {
                    errors.add(new ValidationError(ExcelReader.CLIENTS_SHEET, row.rowNumber(),
                            "'" + refColumn + "' is blank - every client needs a unique reference code (e.g. C1, C2, ...) "
                                    + "to link its beneficiaries on the Beneficiaries sheet."));
                } else if (seenRefs.containsKey(ref)) {
                    errors.add(new ValidationError(ExcelReader.CLIENTS_SHEET, row.rowNumber(),
                            "client_ref '" + ref + "' is used more than once (also at row " + seenRefs.get(ref)
                                    + "). Each client_ref must be unique within this file."));
                } else {
                    seenRefs.put(ref, row.rowNumber());
                    validRefs.add(ref);
                }
            }

            if (clients.headers().contains(nameColumn)) {
                String name = row.get(nameColumn);
                if (name == null || name.isBlank()) {
                    errors.add(new ValidationError(ExcelReader.CLIENTS_SHEET, row.rowNumber(),
                            "'" + nameColumn + "' is blank - every client needs a name."));
                } else if (seenNames.containsKey(name)) {
                    errors.add(new ValidationError(ExcelReader.CLIENTS_SHEET, row.rowNumber(),
                            "client name '" + name + "' is used more than once (also at row " + seenNames.get(name)
                                    + "). Each new client in this file must have a unique name."));
                } else {
                    seenNames.put(name, row.rowNumber());
                }
            }

            validateMandatory(ExcelReader.CLIENTS_SHEET, row, clients.headers(), config.client().mandatory(), errors);
            validateEmails(ExcelReader.CLIENTS_SHEET, row, config.client().emailColumns(), errors);
            validateTemplateClientId(row, clients.headers(), config.client().templateIdColumn(),
                    defaultTemplateClientId, errors);
        }

        return validRefs;
    }

    private void validateTemplateClientId(ExcelRow row, List<String> headers, String templateIdColumn,
                                           Long defaultTemplateClientId, List<ValidationError> errors) {
        String cellValue = headers.contains(templateIdColumn) ? row.get(templateIdColumn) : null;
        if (cellValue == null || cellValue.isBlank()) {
            if (defaultTemplateClientId == null) {
                errors.add(new ValidationError(ExcelReader.CLIENTS_SHEET, row.rowNumber(),
                        "'" + templateIdColumn + "' is blank - enter the id of the existing client this new "
                                + "client should be copied from, or re-run the tool with a --template-client "
                                + "default for the whole file."));
            }
            return;
        }
        try {
            Long.parseLong(cellValue.trim());
        } catch (NumberFormatException e) {
            errors.add(new ValidationError(ExcelReader.CLIENTS_SHEET, row.rowNumber(),
                    "'" + templateIdColumn + "' value '" + cellValue + "' is not a valid number. "
                            + "It must be the numeric id of an existing client."));
        }
    }

    private void validateBeneficiaries(SheetData benes, ColumnsConfig config, Set<String> validClientRefs,
                                        List<ValidationError> errors) {
        String refColumn = config.bene().refColumn();
        boolean hasClientRefHeader = benes.headers().contains(refColumn);

        for (ExcelRow row : benes.rows()) {
            if (hasClientRefHeader) {
                String ref = row.get(refColumn);
                if (ref == null || ref.isBlank()) {
                    errors.add(new ValidationError(ExcelReader.BENEFICIARIES_SHEET, row.rowNumber(),
                            "'" + refColumn + "' is blank - it must match the client_ref of the client "
                                    + "this beneficiary belongs to, on the Clients sheet."));
                } else if (!validClientRefs.contains(ref)) {
                    errors.add(new ValidationError(ExcelReader.BENEFICIARIES_SHEET, row.rowNumber(),
                            "client_ref '" + ref + "' does not match any client_ref on the Clients sheet. "
                                    + "Check for a typo, or add the missing client row."));
                }
            }

            validateMandatory(ExcelReader.BENEFICIARIES_SHEET, row, benes.headers(), config.bene().mandatory(), errors);
        }
    }

    private void validateMandatory(String sheetName, ExcelRow row, List<String> headers,
                                    List<String> mandatoryColumns, List<ValidationError> errors) {
        for (String column : mandatoryColumns) {
            if (!headers.contains(column)) {
                continue; // already reported as a missing-header error
            }
            if (row.isBlank(column)) {
                errors.add(new ValidationError(sheetName, row.rowNumber(),
                        "'" + column + "' is required and cannot be left blank."));
            }
        }
    }

    private void validateEmails(String sheetName, ExcelRow row, List<String> emailFields,
                                 List<ValidationError> errors) {
        for (String field : emailFields) {
            String value = row.get(field);
            if (value != null && !value.isBlank() && !EMAIL_PATTERN.matcher(value).matches()) {
                errors.add(new ValidationError(sheetName, row.rowNumber(),
                        "'" + field + "' value '" + value + "' doesn't look like a valid email address "
                                + "(expected something like name@example.com)."));
            }
        }
    }
}
