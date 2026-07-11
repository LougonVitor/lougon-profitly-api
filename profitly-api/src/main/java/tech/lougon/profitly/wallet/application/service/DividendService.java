package tech.lougon.profitly.wallet.application.service;

import org.springframework.stereotype.Service;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaDividendEventRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiDividendEventRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFundDividendEventRepository;
import tech.lougon.profitly.wallet.domain.model.Dividend;
import tech.lougon.profitly.wallet.domain.repository.DividendRepository;
import tech.lougon.profitly.wallet.domain.repository.WalletRepository;
import tech.lougon.profitly.wallet.presentation.request.AddDividendRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class DividendService {

    private final DividendRepository repository;
    private final WalletRepository walletRepository;
    private final JpaDividendEventRepository dividendEventRepository;
    private final JpaFiiDividendEventRepository fiiDividendEventRepository;
    private final JpaFundDividendEventRepository fundDividendEventRepository;

    public DividendService(DividendRepository repository,
                           WalletRepository walletRepository,
                           JpaDividendEventRepository dividendEventRepository,
                           JpaFiiDividendEventRepository fiiDividendEventRepository,
                           JpaFundDividendEventRepository fundDividendEventRepository) {
        this.repository = repository;
        this.walletRepository = walletRepository;
        this.dividendEventRepository = dividendEventRepository;
        this.fiiDividendEventRepository = fiiDividendEventRepository;
        this.fundDividendEventRepository = fundDividendEventRepository;
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

    /** A market dividend event, whatever table it came from. */
    private record MarketEvent(String exDate, String paymentDate, Double rate, String label) {}

    public List<Dividend> syncFromMarket(String walletId, String userId) {
        var wallet = requireOwnedWallet(walletId, userId);

        // Dedup key: ticker|paymentDate|type — lets a dividend and a JCP paid on the
        // same date coexist while still skipping events already imported or typed by hand.
        Set<String> seen = new HashSet<>();
        for (var existing : repository.findByWalletId(walletId)) {
            seen.add(dedupKey(existing.ticker(), existing.paymentDate(), existing.type()));
        }

        List<Dividend> created = new ArrayList<>();

        for (var position : wallet.positions()) {
            for (var event : eventsFor(position.ticker())) {
                LocalDate exDate = parseDate(event.exDate());
                LocalDate paymentDate = parseDate(event.paymentDate());
                if (exDate == null || paymentDate == null || event.rate() == null) continue;

                String type = event.label() != null ? event.label() : "Dividendo";
                String key = dedupKey(position.ticker(), paymentDate, type);
                if (seen.contains(key)) continue;

                int quantityAtExDate = position.entries().stream()
                        .filter(entry -> !entry.date().isAfter(exDate))
                        .mapToInt(entry -> entry.signedQuantity())
                        .sum();
                if (quantityAtExDate <= 0) continue;

                BigDecimal totalAmount = BigDecimal.valueOf(event.rate())
                        .multiply(BigDecimal.valueOf(quantityAtExDate));

                var dividend = new Dividend(null, walletId, userId,
                        position.ticker(), totalAmount, paymentDate,
                        exDate, type, paymentDate.isBefore(LocalDate.now()), Instant.now());

                created.add(repository.save(dividend));
                seen.add(key);
            }
        }

        return created;
    }

    /** Merges dividend events from the stock, FII and listed-fund tables for one ticker. */
    private List<MarketEvent> eventsFor(String ticker) {
        List<MarketEvent> events = new ArrayList<>();
        for (var e : dividendEventRepository.findBySymbolOrderByLastDatePriorDesc(ticker)) {
            events.add(new MarketEvent(e.getLastDatePrior(), e.getPaymentDate(), e.getRate(), e.getLabel()));
        }
        for (var e : fiiDividendEventRepository.findBySymbolOrderByPaymentDateDesc(ticker)) {
            events.add(new MarketEvent(e.getLastDatePrior(), e.getPaymentDate(), e.getRate(),
                    e.getLabel() != null ? e.getLabel() : "Rendimento"));
        }
        for (var e : fundDividendEventRepository.findBySymbolOrderByPaymentDateDesc(ticker)) {
            events.add(new MarketEvent(e.getLastDatePrior(), e.getPaymentDate(), e.getRate(),
                    e.getLabel() != null ? e.getLabel() : "Rendimento"));
        }
        return events;
    }

    private String dedupKey(String ticker, LocalDate paymentDate, String type) {
        return ticker + "|" + paymentDate + "|" + (type != null ? type.trim().toUpperCase() : "");
    }

    /** Event dates come as strings, sometimes with time/tz — take the first 10 chars. */
    private LocalDate parseDate(String raw) {
        if (raw == null || raw.length() < 10) return null;
        try {
            return LocalDate.parse(raw.substring(0, 10));
        } catch (DateTimeParseException e) {
            return null;
        }
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
