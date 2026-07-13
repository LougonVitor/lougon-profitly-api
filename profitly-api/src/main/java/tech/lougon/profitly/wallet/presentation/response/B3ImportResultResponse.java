package tech.lougon.profitly.wallet.presentation.response;

import tech.lougon.profitly.wallet.application.service.B3StatementImportService.B3ImportResult;

import java.util.List;
import java.util.Map;

public record B3ImportResultResponse(
        int imported,
        int duplicates,
        int skipped,
        Map<String, Integer> skippedByType,
        List<String> errors
) {
    public static B3ImportResultResponse from(B3ImportResult result) {
        return new B3ImportResultResponse(result.imported(), result.duplicates(), result.skipped(),
                result.skippedByType(), result.errors());
    }
}
