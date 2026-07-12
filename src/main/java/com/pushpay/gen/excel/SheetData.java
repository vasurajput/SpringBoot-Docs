package com.pushpay.gen.excel;

import java.util.List;

/** Raw contents of one Excel sheet: the header names found (order as in the file) and data rows. */
public record SheetData(String sheetName, List<String> headers, List<ExcelRow> rows) {
}
