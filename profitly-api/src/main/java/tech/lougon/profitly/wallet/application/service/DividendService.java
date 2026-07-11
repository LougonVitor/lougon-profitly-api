package tech.lougon.profitly.wallet.application.service;

import org.springframework.stereotype.Service;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaDividendEventRepository;
import tech.lougon.profitly.wallet.domain.model.Dividend;
import tech.lougon.profitly.wallet.domain.repository.DividendRepository;
import tech.lougon.profitly.wallet.domain.repository.WalletRepository;
import tech.lougon.profitly.wallet.presentation.request.AddDividendRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class DividendService {

    private final DividendRepository repository;
    private final WalletRepository walletRepository;
    private final JpaDividendEventRepository dividendEventRepository;

    public DividendService(DividendRepository repository,
                           WalletRepository walletRepository,
                           JpaDividendEventRepository dividendEventRepository) {
        this.repository = repository;
        this.walletRepository = walletRepository;
        this.dividendEventRepository = dividendEventRepository;
    }

    public List<Dividend> findByWallet(String walletId, String userId) {
        requireOwnedWallet(walletId, userId);
        return repository.findByWalletId(walletId);
    }

    private tech.lougon.profitly.wallet.domain.model.Wallet requireOwnedWallet(String walletId, String userId) {
        var wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new NoSuchElementException("Wallet not found: " + walletId));
        if (!wallet.userId().equals(userId)) {
            throw new NoSuchElementException("Wallet not found: " + walletId);
        }
        return wallet;
    }

    public Dividend add(String walletId, String userId, AddDividendRequest req) {
        requireOwnedWallet(walletId, userId);
        var dividend = new Dividend(null, walletId, userId,
                req.ticker().toUpperCase(), req.totalAmount(), req.paymentDate(),
                null, req.type(), req.received(), Instant.now());
        return repository.save(dividend);
    }

    public List<Dividend> syncFromMarket(String walletId, String userId) {
        var wallet = requireOwnedWallet(walletId, userId);

        List<Dividend> created = new ArrayList<>();

        for (var position : wallet.positions()) {
            var events = dividendEventRepository.findBySymbolOrderByLastDatePriorDesc(position.ticker());

            for (var event : events) {
                if (event.getLastDatePrior() == null || event.getRate() == null || event.getPaymentDate() == null) {
                    continue;
                }

                LocalDate exDate;
                LocalDate paymentDate;
                try {
                    exDate = LocalDate.parse(event.getLastDatePrior());
                    paymentDate = LocalDate.parse(event.getPaymentDate());
                } catch (DateTimeParseException e) {
                    continue;
                }

                if (repository.existsByWalletIdAndTickerAndPaymentDate(walletId, position.ticker(), paymentDate)) {
                    continue;
                }

                int quantityAtExDate = position.entries().stream()
                        .filter(entry -> !entry.date().isAfter(exDate))
                        .mapToInt(entry -> entry.signedQuantity())
                        .sum();

                if (quantityAtExDate <= 0) {
                    continue;
                }

                BigDecimal totalAmount = BigDecimal.valueOf(event.getRate())
                        .multiply(BigDecimal.valueOf(quantityAtExDate));

                String type = event.getLabel() != null ? event.getLabel() : "Dividendo";

                var dividend = new Dividend(null, walletId, userId,
                        position.ticker(), totalAmount, paymentDate,
                        exDate, type, paymentDate.isBefore(LocalDate.now()), Instant.now());

                created.add(repository.save(dividend));
            }
        }

        return created;
    }

    public Dividend toggleReceived(String id, String userId) {
        var d = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Dividend not found: " + id));
        if (!d.userId().equals(userId)) throw new IllegalArgumentException("Acesso negado");
        var updated = new Dividend(d.id(), d.walletId(), d.userId(), d.ticker(),
                d.totalAmount(), d.paymentDate(), d.exDate(), d.type(), !d.received(), d.createdAt());
        return repository.save(updated);
    }

    public void delete(String id, String userId) {
        var d = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Dividend not found: " + id));
        if (!d.userId().equals(userId)) throw new IllegalArgumentException("Acesso negado");
        repository.deleteById(id);
    }
}
