package com.pushpay.gen.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the "Clients" and "Beneficiaries" sheets with Apache POI. Header matching is by
 * name (order-insensitive); fully blank rows are skipped; Excel row numbers (1-based, as
 * seen in the file) are preserved for error reporting. Column semantics (which headers
 * are known/mandatory) are NOT decided here - that's BatchValidator's job (design.md §5).
 */
@Component
public class ExcelReader {

    public static final String CLIENTS_SHEET = "Clients";
    public static final String BENEFICIARIES_SHEET = "Beneficiaries";

    public record ParsedWorkbook(SheetData clients, SheetData beneficiaries) {
    }

    public ParsedWorkbook read(Path inputFile) throws IOException {
        try (InputStream in = Files.newInputStream(inputFile);
             Workbook workbook = WorkbookFactory.create(in)) {

            SheetData clients = readSheet(workbook, CLIENTS_SHEET);
            SheetData beneficiaries = readSheet(workbook, BENEFICIARIES_SHEET);
            return new ParsedWorkbook(clients, beneficiaries);
        }
    }

    private SheetData readSheet(Workbook workbook, String sheetName) throws IOException {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new IOException("Excel file is missing required sheet '" + sheetName + "'");
        }

        DataFormatter formatter = new DataFormatter();
        int firstRowNum = sheet.getFirstRowNum();
        Row headerRow = sheet.getRow(firstRowNum);
        if (headerRow == null) {
            throw new IOException("Sheet '" + sheetName + "' has no header row");
        }

        // column index -> header name (blank header cells are ignored, not counted as columns)
        Map<Integer, String> columnHeaders = new LinkedHashMap<>();
        List<String> headers = new ArrayList<>();
        for (Cell cell : headerRow) {
            String header = formatter.formatCellValue(cell).trim();
            if (!header.isEmpty()) {
                columnHeaders.put(cell.getColumnIndex(), header);
                headers.add(header);
            }
        }

        List<ExcelRow> rows = new ArrayList<>();
        for (int r = firstRowNum + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            Map<String, String> values = new LinkedHashMap<>();
            boolean allBlank = true;
            for (Map.Entry<Integer, String> entry : columnHeaders.entrySet()) {
                Cell cell = row.getCell(entry.getKey());
                String text = cell == null ? "" : formatter.formatCellValue(cell).trim();
                values.put(entry.getValue(), text);
                if (!text.isEmpty()) {
                    allBlank = false;
                }
            }
            if (allBlank) {
                continue; // skip fully blank rows
            }
            // Excel row numbers are 1-based in the UI; POI row index is 0-based.
            rows.add(new ExcelRow(r + 1, values));
        }

        return new SheetData(sheetName, headers, rows);
    }
}
