package tech.lougon.profitly.wallet.application.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import tech.lougon.profitly.wallet.presentation.request.AddEntryRequest;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Parses the B3 "Movimentação" statement (.xlsx, downloaded from investidor.b3.com.br) and
 * imports trade/transfer rows into a wallet by reusing {@link WalletService#addEntry}, which
 * already computes weighted-average cost and realized P&L in date order.
 */
@Service
public class B3StatementImportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Compared against the accent-stripped, upper-cased value produced by normalize().
    private static final List<String> IMPORTABLE_MOVEMENT_TYPES = List.of(
            "COMPRA", "VENDA", "TRANSFERENCIA", "TRANSFERENCIA - LIQUIDACAO", "ATUALIZACAO"
    );

    /**
     * "Atualização" credits extra shares with no unit price (dividend-reinvestment programs like
     * BTG's PIC) — imported as a zero-cost BUY so quantity stays right, at the expense of slightly
     * understating average cost for the affected ticker (the real reinvestment cost isn't in the
     * statement). Fixed-income products also use "Atualização" (interest accrual) but never reach
     * here since isFixedIncomeProduct filters them out first.
     */
    private static final List<String> ZERO_COST_CREDIT_MOVEMENT_TYPES = List.of("ATUALIZACAO");

    private static final List<String> FIXED_INCOME_PREFIXES = List.of(
            "CDB", "LCI", "LCA", "CRI", "CRA", "TESOURO"
    );

    private final WalletService walletService;

    public B3StatementImportService(WalletService walletService) {
        this.walletService = walletService;
    }

    public B3ImportResult importStatement(String walletId, String userId, InputStream fileStream) {
        List<StatementRow> rows;
        try (Workbook workbook = WorkbookFactory.create(fileStream)) {
            rows = readRows(workbook);
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler o arquivo enviado: " + e.getMessage(), e);
        }

        // The B3 sheet lists rows newest-first. WalletPosition.costWalk() sorts by date when
        // computing average cost, but WalletService.addEntry's "can't sell more than you hold"
        // guard only sees entries already inserted — so a sell must be applied strictly after its
        // matching earlier buy, meaning we must insert in chronological (oldest-first) order here.
        rows = rows.stream()
                .sorted(java.util.Comparator.comparing(StatementRow::date))
                .toList();

        AtomicInteger imported = new AtomicInteger();
        Map<String, Integer> skippedByType = new LinkedHashMap<>();
        List<String> errors = new java.util.ArrayList<>();

        for (StatementRow row : rows) {
            if (isFixedIncomeProduct(row.product()) || !isImportableMovement(row.movementType())) {
                skippedByType.merge(row.movementType(), 1, Integer::sum);
                continue;
            }

            BigDecimal unitPrice = row.unitPrice();
            if (unitPrice == null && row.credit() && ZERO_COST_CREDIT_MOVEMENT_TYPES.contains(normalize(row.movementType()))) {
                unitPrice = BigDecimal.ZERO;
            }

            // Custody transfers between brokers for the same position carry no price ("-" in the
            // sheet) — nothing to reconstruct a cost basis from, so surface them as skipped rather
            // than importing at a fabricated price.
            if (row.quantity() == null || unitPrice == null) {
                skippedByType.merge(row.movementType() + " (sem preço)", 1, Integer::sum);
                continue;
            }

            try {
                String ticker = extractTicker(row.product());
                String type = row.credit() ? "BUY" : "SELL";
                AddEntryRequest request = new AddEntryRequest(row.date(), row.quantity(), unitPrice, type);
                walletService.addEntry(walletId, ticker, request, userId);
                imported.incrementAndGet();
            } catch (RuntimeException e) {
                errors.add(row.product() + " (" + row.date() + "): " + e.getMessage());
            }
        }

        int skipped = skippedByType.values().stream().mapToInt(Integer::intValue).sum();
        return new B3ImportResult(imported.get(), skipped, skippedByType, errors);
    }

    private List<StatementRow> readRows(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);
        List<StatementRow> rows = new java.util.ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String entrySide = cellText(row.getCell(0));
            String movementType = cellText(row.getCell(2));
            String product = cellText(row.getCell(3));
            String quantityRaw = cellText(row.getCell(5));
            String priceRaw = cellText(row.getCell(6));
            if (product == null || product.isBlank()) continue;

            LocalDate date = cellDate(row.getCell(1));
            if (date == null) continue;

            BigDecimal quantity = parseDecimal(quantityRaw);
            BigDecimal unitPrice = parseDecimal(priceRaw);

            boolean credit = "Credito".equalsIgnoreCase(entrySide == null ? "" : entrySide.trim());
            rows.add(new StatementRow(date, movementType == null ? "" : movementType.trim(), product.trim(),
                    quantity, unitPrice, credit));
        }
        return rows;
    }

    private boolean isImportableMovement(String movementType) {
        return IMPORTABLE_MOVEMENT_TYPES.contains(normalize(movementType));
    }

    /**
     * Fixed-income products are labelled either "CDB - CDB123..." (prefix immediately followed by
     * the separator) or "Tesouro IPCA+ 2050 - ..." (prefix followed by more words before the
     * separator) — matching on "prefix + space" covers both without false-matching a real ticker
     * (e.g. "CRIX11", which has no space after "CRI").
     */
    private boolean isFixedIncomeProduct(String product) {
        String upper = product.toUpperCase(Locale.ROOT);
        return FIXED_INCOME_PREFIXES.stream().anyMatch(prefix -> upper.startsWith(prefix + " ") || upper.startsWith(prefix + "-"));
    }

    private String extractTicker(String product) {
        int dashIndex = product.indexOf('-');
        String raw = dashIndex > 0 ? product.substring(0, dashIndex) : product;
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private String normalize(String value) {
        if (value == null) return "";
        String upper = value.trim().toUpperCase(Locale.ROOT);
        return java.text.Normalizer.normalize(upper, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private String cellText(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
        }
        return cell.getStringCellValue();
    }

    private LocalDate cellDate(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String raw = cellText(cell);
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDate.parse(raw.trim(), DATE_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return new BigDecimal(raw.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record StatementRow(LocalDate date, String movementType, String product,
                                 BigDecimal quantity, BigDecimal unitPrice, boolean credit) {}

    public record B3ImportResult(int imported, int skipped, Map<String, Integer> skippedByType, List<String> errors) {}
}
