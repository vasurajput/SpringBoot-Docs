package com.pushpay.gen.validate;

/**
 * One validation failure. rowNumber is null for file/header-level errors that aren't
 * tied to a specific data row (e.g. an unknown column header).
 */
public record ValidationError(String sheet, Integer rowNumber, String message) {

    @Override
    public String toString() {
        String location = rowNumber == null
                ? sheet + " sheet, header row"
                : sheet + " sheet, row " + rowNumber;
        return location + ": " + message;
    }
}
