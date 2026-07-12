package com.pushpay.gen.sql;

import com.pushpay.gen.config.ColumnsConfig;
import com.pushpay.gen.excel.ExcelReader;
import com.pushpay.gen.excel.ExcelRow;
import com.pushpay.gen.excel.SheetData;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds batch.sql (one flat SELECT per client, design.md §7) and batch_summary.txt.
 * Escaping rules (design.md §5.4):
 *  - JSON strings: escape backslash, double-quote, newlines/tabs.
 *  - The whole JSON blob is then wrapped in a SQL string literal ('...'::jsonb), so any
 *    single quote in the data (already valid inside JSON, e.g. an apostrophe in a name)
 *    must additionally be doubled for the SQL literal.
 *  - Blank cells: key omitted entirely. Clients -> template value; Beneficiaries -> NULL.
 *
 * Column names (refColumn/nameColumn/templateIdColumn) come from ColumnsConfig, not
 * literals in this class. nameColumn's configured text is also the JSON key sent to
 * Postgres as p_client->>'...' - see the note in application.yml.
 */
@Component
public class SqlGenerator {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public record Client(String ref, String name, long templateClientId, String clientJson, List<String> beneJsons) {
        public int beneCount() {
            return beneJsons.size();
        }
    }

    public record Batch(String sql, String summary, List<Client> clients, int totalBenes) {
    }

    /**
     * @param defaultTemplateClientId fallback for rows that leave 'template_client_id' blank;
     *                                BatchValidator guarantees every row resolves to a value
     *                                (row cell, or this default) before generate() is called.
     */
    public Batch generate(ExcelReader.ParsedWorkbook workbook, ColumnsConfig config,
                           Long defaultTemplateClientId, String inputFileName) {
        SheetData clientsSheet = workbook.clients();
        SheetData benesSheet = workbook.beneficiaries();

        String clientRef = config.client().refColumn();
        String name = config.client().nameColumn();
        String templateIdColumn = config.client().templateIdColumn();
        String beneRef = config.bene().refColumn();

        Map<String, List<ExcelRow>> benesByClientRef = new LinkedHashMap<>();
        for (ExcelRow bene : benesSheet.rows()) {
            String ref = bene.get(beneRef);
            benesByClientRef.computeIfAbsent(ref, k -> new ArrayList<>()).add(bene);
        }

        List<Client> clients = new ArrayList<>();
        int totalBenes = 0;
        for (ExcelRow clientRow : clientsSheet.rows()) {
            String ref = clientRow.get(clientRef);
            String nameValue = clientRow.get(name);
            long templateClientId = resolveTemplateClientId(clientRow, clientsSheet.headers(), templateIdColumn,
                    defaultTemplateClientId);
            String clientJson = buildClientJson(clientRow, clientsSheet.headers(), config);

            List<ExcelRow> benes = benesByClientRef.getOrDefault(ref, List.of());
            List<String> beneJsons = new ArrayList<>();
            for (ExcelRow bene : benes) {
                beneJsons.add(buildBeneJson(bene, benesSheet.headers(), config));
            }
            totalBenes += beneJsons.size();

            clients.add(new Client(ref, nameValue, templateClientId, clientJson, beneJsons));
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String sql = buildSql(clients, inputFileName, timestamp, templateIdColumn);
        String summary = buildSummary(clients, inputFileName, timestamp, totalBenes);

        return new Batch(sql, summary, clients, totalBenes);
    }

    private long resolveTemplateClientId(ExcelRow row, List<String> headers, String templateIdColumn,
                                          Long defaultTemplateClientId) {
        String cellValue = headers.contains(templateIdColumn) ? row.get(templateIdColumn) : null;
        if (cellValue != null && !cellValue.isBlank()) {
            return Long.parseLong(cellValue.trim());
        }
        return defaultTemplateClientId;
    }

    private String buildClientJson(ExcelRow row, List<String> headers, ColumnsConfig config) {
        Map<String, String> fields = new LinkedHashMap<>();
        String nameColumn = config.client().nameColumn();
        if (headers.contains(nameColumn) && !row.isBlank(nameColumn)) {
            fields.put(nameColumn, row.get(nameColumn));
        }
        for (String column : config.client().columns()) {
            if (headers.contains(column) && !row.isBlank(column)) {
                fields.put(column, row.get(column));
            }
        }
        return toJsonObject(fields);
    }

    private String buildBeneJson(ExcelRow row, List<String> headers, ColumnsConfig config) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String column : config.bene().columns()) {
            if (headers.contains(column) && !row.isBlank(column)) {
                fields.put(column, row.get(column));
            }
        }
        return toJsonObject(fields);
    }

    private String toJsonObject(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append('"').append(jsonEscape(entry.getKey())).append("\":\"")
                    .append(jsonEscape(entry.getValue())).append('"');
        }
        sb.append("}");
        return sb.toString();
    }

    private String jsonEscape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Escapes a JSON blob for embedding as a SQL '...' string literal. */
    private String sqlLiteral(String jsonText) {
        return jsonText.replace("'", "''");
    }

    private String buildSql(List<Client> clients, String inputFileName, String timestamp, String templateIdColumn) {
        StringBuilder sb = new StringBuilder();
        sb.append("-- Generated by pushpay-gen\n");
        sb.append("-- Input file: ").append(inputFileName).append("\n");
        sb.append("-- Template client id: per-client, from the '").append(templateIdColumn).append("' column\n");
        sb.append("-- Generated at: ").append(timestamp).append("\n");
        sb.append("--\n");
        sb.append("-- Run top-to-bottom in auto-commit; a failed statement affects only that client.\n");
        sb.append("-- Capture full console output (NOTICEs + errors) and paste on the change ticket.\n\n");

        for (Client client : clients) {
            sb.append("-- Client ").append(client.ref()).append(" : ").append(client.name())
                    .append(" (template client ").append(client.templateClientId())
                    .append(", ").append(client.beneCount()).append(" beneficiaries)\n");
            sb.append("SELECT onboard_pushpay_client_with_benes(\n");
            sb.append("    p_template_client_id => ").append(client.templateClientId()).append(",\n");
            sb.append("    p_client => '").append(sqlLiteral(client.clientJson())).append("'::jsonb,\n");

            if (client.beneJsons().isEmpty()) {
                sb.append("    p_benes  => '[]'::jsonb);\n\n");
            } else {
                sb.append("    p_benes  => '[\n");
                for (int i = 0; i < client.beneJsons().size(); i++) {
                    sb.append("      ").append(sqlLiteral(client.beneJsons().get(i)));
                    if (i < client.beneJsons().size() - 1) {
                        sb.append(",");
                    }
                    sb.append("\n");
                }
                sb.append("    ]'::jsonb);\n\n");
            }
        }
        return sb.toString();
    }

    private String buildSummary(List<Client> clients, String inputFileName, String timestamp, int totalBenes) {
        StringBuilder sb = new StringBuilder();
        sb.append("PushPay Onboarding Batch Summary\n");
        sb.append("=================================\n");
        sb.append("Generated:            ").append(timestamp).append("\n");
        sb.append("Input file:           ").append(inputFileName).append("\n");
        sb.append("Total clients:        ").append(clients.size()).append("\n");
        sb.append("Total beneficiaries:  ").append(totalBenes).append("\n\n");
        sb.append("Clients:\n");
        for (Client client : clients) {
            sb.append(String.format("  %-8s %-40s template=%-8d %d beneficiaries%n",
                    client.ref(), client.name(), client.templateClientId(), client.beneCount()));
        }
        return sb.toString();
    }
}
