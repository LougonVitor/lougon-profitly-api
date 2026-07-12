package tech.lougon.profitly.finance.infrastructure.wallet;

import org.springframework.stereotype.Component;
import tech.lougon.profitly.finance.domain.port.InvestedLookup;
import tech.lougon.profitly.wallet.domain.model.PositionEntry;
import tech.lougon.profitly.wallet.domain.repository.WalletRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Sums the user's wallet purchases from {@code since} onwards, reading the wallet module
 * through its domain repository. A "buy" is a {@link PositionEntry}; invested = qty × paidPrice.
 */
@Component
public class WalletInvestedLookup implements InvestedLookup {

    private final WalletRepository walletRepository;

    public WalletInvestedLookup(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public BigDecimal investedSince(String userId, LocalDate since) {
        return walletRepository.findByUserId(userId).stream()
                .flatMap(wallet -> wallet.positions().stream())
                .flatMap(position -> position.entries().stream())
                .filter(entry -> entry.date() != null && !entry.date().isBefore(since))
                .filter(entry -> entry.quantity() != null && entry.paidPrice() != null)
                .filter(entry -> entry.typeOrBuy() == tech.lougon.profitly.wallet.domain.model.EntryType.BUY)
                .map(entry -> entry.paidPrice().multiply(entry.quantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
