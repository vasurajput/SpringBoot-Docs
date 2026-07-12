package com.pushpay.gen.excel;

import java.util.Map;

/**
 * One data row from an Excel sheet: header name -> cell text (trimmed), plus the
 * 1-based row number as it appears in Excel (for error reporting).
 */
public record ExcelRow(int rowNumber, Map<String, String> values) {

    public String get(String header) {
        return values.get(header);
    }

    public boolean isBlank(String header) {
        String v = get(header);
        return v == null || v.isBlank();
    }
}
