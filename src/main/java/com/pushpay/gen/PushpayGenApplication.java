package com.pushpay.gen;

import com.pushpay.gen.config.ColumnsConfig;
import com.pushpay.gen.excel.ExcelReader;
import com.pushpay.gen.sql.SqlGenerator;
import com.pushpay.gen.validate.BatchValidator;
import com.pushpay.gen.validate.ValidationError;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * CLI entry point (design.md §5). NOT a web app - spring.main.web-application-type=none.
 *
 * Usage: java -jar pushpay-gen.jar <input.xlsx> [--template-client <id>] [--out batch.sql]
 *
 * Each client row picks its own reference client via the 'template_client_id' column on
 * the Clients sheet. --template-client is only a fallback default used for rows that
 * leave that column blank. If a row has neither a filled-in column value nor a CLI
 * default, validation fails and nothing is generated (exit code 2).
 *
 * Exit codes: 0 ok, 1 usage/read error, 2 validation failed (nothing generated).
 */
@SpringBootApplication
@EnableConfigurationProperties(ColumnsConfig.class)
public class PushpayGenApplication implements CommandLineRunner, ExitCodeGenerator {

    private static final String USAGE =
            "Usage: java -jar pushpay-gen.jar <input.xlsx> [--template-client <id>] [--out batch.sql]";

    private final ColumnsConfig columnsConfig;
    private final ExcelReader excelReader;
    private final BatchValidator batchValidator;
    private final SqlGenerator sqlGenerator;

    private int exitCode = 0;

    public PushpayGenApplication(ColumnsConfig columnsConfig, ExcelReader excelReader,
                                  BatchValidator batchValidator, SqlGenerator sqlGenerator) {
        this.columnsConfig = columnsConfig;
        this.excelReader = excelReader;
        this.batchValidator = batchValidator;
        this.sqlGenerator = sqlGenerator;
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context = new org.springframework.boot.builder.SpringApplicationBuilder(
                PushpayGenApplication.class).run(args);
        System.exit(SpringApplication.exit(context));
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    @Override
    public void run(String... args) {
        exitCode = execute(args);
    }

    private int execute(String[] args) {
        CliArgs parsed;
        try {
            parsed = CliArgs.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println(USAGE);
            return 1;
        }

        ExcelReader.ParsedWorkbook workbook;
        try {
            workbook = excelReader.read(parsed.inputFile());
        } catch (IOException e) {
            System.err.println("Failed to read '" + parsed.inputFile() + "': " + e.getMessage());
            return 1;
        }

        List<ValidationError> errors = batchValidator.validate(workbook, columnsConfig, parsed.templateClientId());
        if (!errors.isEmpty()) {
            System.err.println("Validation failed with " + errors.size() + " error(s). Nothing generated.");
            for (ValidationError error : errors) {
                System.err.println("  " + error);
            }
            return 2;
        }

        SqlGenerator.Batch batch = sqlGenerator.generate(workbook, columnsConfig,
                parsed.templateClientId(), parsed.inputFile().getFileName().toString());

        try {
            Path summaryPath = summaryPathFor(parsed.outFile());
            if (parsed.outFile().getParent() != null) {
                Files.createDirectories(parsed.outFile().getParent());
            }
            Files.writeString(parsed.outFile(), batch.sql());
            Files.writeString(summaryPath, batch.summary());
            System.out.println("Generated " + parsed.outFile() + " and " + summaryPath + " - "
                    + batch.clients().size() + " client(s), " + batch.totalBenes() + " beneficiary(ies).");
        } catch (IOException e) {
            System.err.println("Failed to write output: " + e.getMessage());
            return 1;
        }

        return 0;
    }

    private Path summaryPathFor(Path outFile) {
        String fileName = outFile.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String base = dot >= 0 ? fileName.substring(0, dot) : fileName;
        Path parent = outFile.getParent();
        String summaryName = base + "_summary.txt";
        return parent == null ? Path.of(summaryName) : parent.resolve(summaryName);
    }

    private record CliArgs(Path inputFile, Long templateClientId, Path outFile) {
        static CliArgs parse(String[] args) {
            if (args.length == 0 || args[0].startsWith("--")) {
                throw new IllegalArgumentException("Missing required <input.xlsx> argument.");
            }
            Path inputFile = Path.of(args[0]);

            Long templateClientId = null;
            String out = "batch.sql";

            int i = 1;
            while (i < args.length) {
                String token = args[i];
                switch (token) {
                    case "--template-client" -> {
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("--template-client requires a value.");
                        }
                        try {
                            templateClientId = Long.parseLong(args[i + 1]);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException(
                                    "--template-client must be numeric, got '" + args[i + 1] + "'.");
                        }
                        i += 2;
                    }
                    case "--out" -> {
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("--out requires a value.");
                        }
                        out = args[i + 1];
                        i += 2;
                    }
                    default -> throw new IllegalArgumentException("Unrecognized argument '" + token + "'.");
                }
            }

            if (!Files.exists(inputFile)) {
                throw new IllegalArgumentException("Input file not found: " + inputFile);
            }

            return new CliArgs(inputFile, templateClientId, Path.of(out));
        }
    }
}
