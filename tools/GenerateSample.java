import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;

public class GenerateSample {
    public static void main(String[] args) throws Exception {
        boolean bad = args.length > 0 && args[0].equals("bad");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet clients = wb.createSheet("Clients");
            String[] clientHeaders = {"client_ref", "name", "country", "transaction_manager", "contact_email"};
            Row ch = clients.createRow(0);
            for (int i = 0; i < clientHeaders.length; i++) ch.createCell(i).setCellValue(clientHeaders[i]);

            Object[][] clientRows = bad
                ? new Object[][] {
                    {"C1", "Acme Trading LLC", "India", "TM-NORTH", "not-an-email"},
                    {"C1", "Dup Ref Co", "India", "TM-NORTH", "ok@example.com"},
                    {"C2", "", "India", "", ""},
                  }
                : new Object[][] {
                    {"C1", "Acme Trading LLC", "India", "TM-NORTH", "acme@example.com"},
                    {"C2", "Globex Corp", "", "", ""},
                  };
            for (int r = 0; r < clientRows.length; r++) {
                Row row = clients.createRow(r + 1);
                for (int c = 0; c < clientRows[r].length; c++) row.createCell(c).setCellValue((String) clientRows[r][c]);
            }

            Sheet benes = wb.createSheet("Beneficiaries");
            String[] beneHeaders = {"client_ref", "bene_name", "account_number", "swift_code", "bene_bank_name", "bene_addr_country", "bene_addr_city"};
            Row bh = benes.createRow(0);
            for (int i = 0; i < beneHeaders.length; i++) bh.createCell(i).setCellValue(beneHeaders[i]);

            Object[][] beneRows = bad
                ? new Object[][] {
                    {"C1", "Supplier One", "004501234567", "HDFCINBB", "HDFC Bank", "India", "Pune"},
                    {"C3", "Orphan Bene", "0099", "", "ICICI Bank", "", ""},
                    {"C2", "", "", "", "", "", ""},
                  }
                : new Object[][] {
                    {"C1", "Supplier One", "004501234567", "HDFCINBB", "HDFC Bank", "India", "Pune"},
                    {"C1", "Supplier Two", "009988776655", "", "ICICI Bank", "", ""},
                    {"C2", "Only Bene", "112233445566", "", "Wells Fargo", "USA", "NYC"},
                  };
            for (int r = 0; r < beneRows.length; r++) {
                Row row = benes.createRow(r + 1);
                for (int c = 0; c < beneRows[r].length; c++) row.createCell(c).setCellValue((String) beneRows[r][c]);
            }

            wb.createSheet("READ_ME");

            String outName = bad ? "sample_bad.xlsx" : "sample.xlsx";
            try (FileOutputStream fos = new FileOutputStream(outName)) {
                wb.write(fos);
            }
            System.out.println("wrote " + outName);
        }
    }
}
