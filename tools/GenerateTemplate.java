import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;

/**
 * Builds the blank onboarding template (design.md §3) that the product team fills with
 * real client/beneficiary data: Clients + Beneficiaries sheets with the exact DB column
 * headers from application.yml, plus a READ_ME instructions sheet. One example row is
 * included in each data sheet, clearly marked for deletion, to show the expected format.
 */
public class GenerateTemplate {

    static final String[] CLIENT_HEADERS = {
        "client_ref", "name", "template_client_id", "client_address", "country", "client_city",
        "deal_review_notification", "country_segment", "website_url", "contact_email",
        "cct_account_number", "transaction_manager", "legal_entity", "primary_email",
        "industry_segment", "seller_type", "debtor_portal_default_lang", "seller_payment_type"
    };

    static final String[] CLIENT_EXAMPLE = {
        "C1", "Acme Trading LLC", "1001", "", "India", "", "", "", "", "acme@example.com",
        "", "TM-NORTH", "", "", "", "", "", ""
    };

    static final String[] BENE_HEADERS = {
        "client_ref", "bene_name", "account_number", "bene_bank_name", "routing_code",
        "swift_code", "bank_code", "bene_addr_address", "bene_addr_country", "bene_addr_city",
        "bene_addr_province", "bene_addr_post_code", "bank_addr_address", "bank_addr_country",
        "bank_addr_city", "bank_addr_province", "bank_addr_post_code"
    };

    static final String[] BENE_EXAMPLE = {
        "C1", "Supplier One", "004501234567", "HDFC Bank", "", "HDFCINBB", "",
        "", "India", "Pune", "", "", "", "", "", "", ""
    };

    static final String[][] READ_ME = {
        {"PushPay Client Onboarding - Instructions"},
        {""},
        {"1. One file = one batch = ONLY new clients (never cumulative). Each client's"},
        {"   beneficiaries must be in this same file, on the Beneficiaries sheet."},
        {"2. Name the file PushPay_Onboarding_YYYY-MM-DD.xlsx before sending it back."},
        {""},
        {"Clients sheet:"},
        {" - client_ref: a short code you make up (C1, C2, ...) to link a client's rows"},
        {"   to its beneficiaries below. Must be unique within the file. Never stored in the DB."},
        {" - name: mandatory."},
        {" - template_client_id: the id of the EXISTING PushPay-enabled client this new"},
        {"   client should be cloned from. Every row needs a value here UNLESS the tool is"},
        {"   run with a --template-client default for the whole batch - if you're not sure,"},
        {"   fill it in on every row to be safe. Different rows can point at different"},
        {"   template clients in the same file."},
        {" - All other columns are OPTIONAL. Leave a cell BLANK to copy that value from the"},
        {"   template (reference) client instead of overriding it. Only fill in what's"},
        {"   different for this new client."},
        {""},
        {"Beneficiaries sheet:"},
        {" - client_ref: mandatory, must match a client_ref on the Clients sheet."},
        {" - There is NO template for beneficiaries - fill in the complete, real data for"},
        {"   every beneficiary. A blank cell here means NULL in the database, not"},
        {"   \"copy from somewhere\"."},
        {" - bene_name and account_number are mandatory for every beneficiary row."},
        {""},
        {"Before sending back:"},
        {" - Delete the yellow EXAMPLE row on both sheets - it's only there to show the"},
        {"   expected format."},
        {" - Do not rename, remove, or reorder any column headers."},
        {" - Do not add extra columns - talk to the team first if you need a new field."},
    };

    public static void main(String[] args) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(wb);
            CellStyle exampleStyle = exampleStyle(wb);

            buildDataSheet(wb, "Clients", CLIENT_HEADERS, CLIENT_EXAMPLE, headerStyle, exampleStyle);
            buildDataSheet(wb, "Beneficiaries", BENE_HEADERS, BENE_EXAMPLE, headerStyle, exampleStyle);
            buildReadMe(wb);

            wb.setActiveSheet(wb.getSheetIndex("READ_ME"));

            try (FileOutputStream fos = new FileOutputStream("templates/PushPay_Onboarding_Template.xlsx")) {
                wb.write(fos);
            }
            System.out.println("wrote templates/PushPay_Onboarding_Template.xlsx");
        }
    }

    private static void buildDataSheet(Workbook wb, String sheetName, String[] headers, String[] example,
                                        CellStyle headerStyle, CellStyle exampleStyle) {
        Sheet sheet = wb.createSheet(sheetName);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        Row exampleRow = sheet.createRow(1);
        Cell tag = exampleRow.createCell(headers.length + 1);
        tag.setCellValue("<- EXAMPLE ROW, delete before sending");
        for (int i = 0; i < example.length; i++) {
            Cell cell = exampleRow.createCell(i);
            cell.setCellValue(example[i]);
            cell.setCellStyle(exampleStyle);
        }

        sheet.createFreezePane(0, 1);
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static void buildReadMe(Workbook wb) {
        Sheet sheet = wb.createSheet("READ_ME");
        CellStyle titleStyle = wb.createCellStyle();
        Font titleFont = wb.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        for (int r = 0; r < READ_ME.length; r++) {
            Row row = sheet.createRow(r);
            Cell cell = row.createCell(0);
            cell.setCellValue(READ_ME[r][0]);
            if (r == 0) {
                cell.setCellStyle(titleStyle);
            }
        }
        sheet.setColumnWidth(0, 100 * 256);
    }

    private static CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static CellStyle exampleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = wb.createFont();
        font.setItalic(true);
        style.setFont(font);
        return style;
    }
}
