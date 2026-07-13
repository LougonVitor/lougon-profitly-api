package tech.lougon.profitly.wallet.application.service;

import org.springframework.stereotype.Service;
import tech.lougon.profitly.wallet.application.dto.WalletSummaryDTO;
import tech.lougon.profitly.wallet.application.mapper.WalletMapper;
import tech.lougon.profitly.wallet.domain.model.EntryType;
import tech.lougon.profitly.wallet.domain.model.PositionEntry;
import tech.lougon.profitly.wallet.domain.model.Wallet;
import tech.lougon.profitly.wallet.domain.model.WalletPosition;
import tech.lougon.profitly.wallet.domain.port.StockMarketData;
import tech.lougon.profitly.wallet.domain.port.StockPriceLookup;
import tech.lougon.profitly.wallet.domain.repository.WalletRepository;
import tech.lougon.profitly.wallet.presentation.request.AddEntryRequest;
import tech.lougon.profitly.wallet.presentation.request.UpdateEntryRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final StockPriceLookup stockPriceLookup;
    private final WalletMapper walletMapper;
    private final tech.lougon.profitly.wallet.domain.repository.DividendRepository dividendRepository;
    private final FixedIncomeValuationService fixedIncomeValuationService;

    public WalletService(WalletRepository walletRepository,
                         StockPriceLookup stockPriceLookup,
                         WalletMapper walletMapper,
                         tech.lougon.profitly.wallet.domain.repository.DividendRepository dividendRepository,
                         FixedIncomeValuationService fixedIncomeValuationService) {
        this.walletRepository = walletRepository;
        this.stockPriceLookup = stockPriceLookup;
        this.walletMapper = walletMapper;
        this.dividendRepository = dividendRepository;
        this.fixedIncomeValuationService = fixedIncomeValuationService;
    }

    public List<WalletSummaryDTO> findAll(String userId) {
        return walletRepository.findByUserId(userId).stream()
                .map(wallet -> walletMapper.toSummaryDTO(wallet, resolveMarketData(wallet)))
                .toList();
    }

    public WalletSummaryDTO findById(String id, String userId) {
        Wallet wallet = requireOwned(id, userId);
        return walletMapper.toSummaryDTO(wallet, resolveMarketData(wallet));
    }

    /** Loads the wallet and enforces ownership; a wallet of another user behaves as not found. */
    Wallet requireOwned(String walletId, String userId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new NoSuchElementException("Wallet not found: " + walletId));
        if (!wallet.userId().equals(userId)) {
            throw new NoSuchElementException("Wallet not found: " + walletId);
        }
        return wallet;
    }

    public WalletSummaryDTO create(String name, String userId) {
        return create(name, userId, tech.lougon.profitly.wallet.domain.model.WalletSource.MANUAL);
    }

    public WalletSummaryDTO create(String name, String userId, tech.lougon.profitly.wallet.domain.model.WalletSource source) {
        Wallet wallet = new Wallet(null, name, userId, List.of(), Instant.now(), source);
        Wallet saved = walletRepository.save(wallet);
        return walletMapper.toSummaryDTO(saved, Map.of());
    }

    public WalletSummaryDTO rename(String walletId, String name, String userId) {
        Wallet wallet = requireOwned(walletId, userId);
        Wallet renamed = new Wallet(wallet.id(), name, wallet.userId(), wallet.positions(), wallet.createdAt(), wallet.source());
        Wallet saved = walletRepository.save(renamed);
        return walletMapper.toSummaryDTO(saved, resolveMarketData(saved));
    }

    public void deleteWallet(String walletId, String userId) {
        requireOwned(walletId, userId);
        // wallet_dividends has no FK cascade to wallets — clean up explicitly
        dividendRepository.deleteByWalletId(walletId);
        walletRepository.deleteById(walletId);
    }

    public WalletSummaryDTO addEntry(String walletId, String ticker, AddEntryRequest request, String userId) {
        Wallet wallet = requireOwned(walletId, userId);

        // Treasury symbols are lowercase in the tickers table; everything else uppercase.
        String upperTicker = ticker.toLowerCase().startsWith("tesouro")
                ? ticker.toLowerCase()
                : ticker.toUpperCase();
        EntryType type = parseType(request.type());
        Optional<WalletPosition> existing = wallet.positions().stream()
                .filter(p -> p.ticker().equalsIgnoreCase(upperTicker))
                .findFirst();

        if (existing.isPresent() && existing.get().isFixedIncome()) {
            throw new IllegalArgumentException(
                    "Posições de renda fixa aceitam aportes/resgates apenas pelos endpoints dedicados");
        }

        if (type == EntryType.SELL) {
            java.math.BigDecimal held = existing.map(WalletPosition::totalQuantity)
                    .orElse(java.math.BigDecimal.ZERO);
            if (request.quantity() == null || request.quantity().compareTo(held) > 0) {
                throw new IllegalArgumentException(
                        "Venda maior que a posição atual (" + held.stripTrailingZeros().toPlainString()
                                + " unidades de " + upperTicker + ")");
            }
        }

        List<WalletPosition> updatedPositions;
        if (existing.isPresent()) {
            WalletPosition position = existing.get();
            PositionEntry newEntry = new PositionEntry(
                    null, position.id(), request.date(), request.quantity(), request.paidPrice(), type, Instant.now()
            );
            List<PositionEntry> updatedEntries = new ArrayList<>(position.entries());
            updatedEntries.add(newEntry);
            WalletPosition updatedPosition = position.withEntries(updatedEntries);
            updatedPositions = wallet.positions().stream()
                    .map(p -> p.id().equals(position.id()) ? updatedPosition : p)
                    .toList();
        } else {
            PositionEntry newEntry = new PositionEntry(
                    null, null, request.date(), request.quantity(), request.paidPrice(), type, Instant.now()
            );
            WalletPosition newPosition = new WalletPosition(
                    null, walletId, upperTicker, List.of(newEntry), Instant.now(), null
            );
            updatedPositions = new ArrayList<>(wallet.positions());
            ((ArrayList<WalletPosition>) updatedPositions).add(newPosition);
        }

        Wallet updated = new Wallet(wallet.id(), wallet.name(), wallet.userId(), updatedPositions, wallet.createdAt(), wallet.source());
        Wallet saved = walletRepository.save(updated);
        return walletMapper.toSummaryDTO(saved, resolveMarketData(saved));
    }

    public WalletSummaryDTO updateEntry(String walletId, String entryId, UpdateEntryRequest request, String userId) {
        Wallet wallet = requireOwned(walletId, userId);
        WalletPosition owningPosition = requireEntry(wallet, entryId);

        if (owningPosition.isFixedIncome()) {
            PositionEntry current = owningPosition.entries().stream()
                    .filter(e -> e.id().equals(entryId)).findFirst().orElseThrow();
            boolean changesPaidPrice = request.paidPrice() != null && current.paidPrice().compareTo(request.paidPrice()) != 0;
            boolean changesType = request.type() != null && !parseType(request.type()).equals(current.typeOrBuy());
            if (changesPaidPrice || changesType) {
                throw new IllegalArgumentException(
                        "Lançamentos de renda fixa aceitam apenas edição de data/valor — use resgatar para encerrar a posição");
            }
        }

        List<WalletPosition> updatedPositions = wallet.positions().stream()
                .map(position -> {
                    List<PositionEntry> updatedEntries = position.entries().stream()
                            .map(e -> e.id().equals(entryId)
                                    ? new PositionEntry(e.id(), e.walletPositionId(), request.date(), request.quantity(), request.paidPrice(),
                                            request.type() != null ? parseType(request.type()) : e.typeOrBuy(), e.createdAt())
                                    : e)
                            .toList();
                    return position.withEntries(updatedEntries);
                })
                .toList();

        Wallet updated = new Wallet(wallet.id(), wallet.name(), wallet.userId(), updatedPositions, wallet.createdAt(), wallet.source());
        Wallet saved = walletRepository.save(updated);
        return walletMapper.toSummaryDTO(saved, resolveMarketData(saved));
    }

    public WalletSummaryDTO deleteEntry(String walletId, String entryId, String userId) {
        Wallet wallet = requireOwned(walletId, userId);
        requireEntry(wallet, entryId);

        List<WalletPosition> updatedPositions = wallet.positions().stream()
                .map(position -> {
                    List<PositionEntry> remaining = position.entries().stream()
                            .filter(e -> !e.id().equals(entryId))
                            .toList();
                    return position.withEntries(remaining);
                })
                .filter(position -> !position.entries().isEmpty())
                .toList();

        Wallet updated = new Wallet(wallet.id(), wallet.name(), wallet.userId(), updatedPositions, wallet.createdAt(), wallet.source());
        Wallet saved = walletRepository.save(updated);
        return walletMapper.toSummaryDTO(saved, resolveMarketData(saved));
    }

    public WalletSummaryDTO deletePosition(String walletId, String ticker, String userId) {
        Wallet wallet = requireOwned(walletId, userId);

        List<WalletPosition> updatedPositions = wallet.positions().stream()
                .filter(p -> !p.ticker().equalsIgnoreCase(ticker))
                .toList();

        Wallet updated = new Wallet(wallet.id(), wallet.name(), wallet.userId(), updatedPositions, wallet.createdAt(), wallet.source());
        Wallet saved = walletRepository.save(updated);
        return walletMapper.toSummaryDTO(saved, resolveMarketData(saved));
    }

    private EntryType parseType(String raw) {
        if (raw == null || raw.isBlank()) return EntryType.BUY;
        try {
            return EntryType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de lançamento inválido: " + raw);
        }
    }

    /** Returns the position owning {@code entryId}, or throws if no entry with that id exists in the wallet. */
    private WalletPosition requireEntry(Wallet wallet, String entryId) {
        return wallet.positions().stream()
                .filter(p -> p.entries().stream().anyMatch(e -> e.id().equals(entryId)))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Entry not found: " + entryId));
    }

    /**
     * Package-visible so {@link FixedIncomeService} can build the same response shape after its own saves.
     * Built with a plain loop (not {@link Collectors#toMap}) because a ticker with no market data yet
     * (e.g. just imported, not synced) resolves to {@code null}, which {@code Collectors.toMap}'s internal
     * {@code HashMap.merge} rejects with an NPE.
     */
    Map<String, StockMarketData> resolveMarketData(Wallet wallet) {
        Map<String, StockMarketData> result = new java.util.HashMap<>();
        for (WalletPosition position : wallet.positions()) {
            result.putIfAbsent(position.ticker(), resolvePositionMarketData(position));
        }
        return result;
    }

    private StockMarketData resolvePositionMarketData(WalletPosition position) {
        if (position.isFixedIncome()) return fixedIncomeMarketData(position);
        return stockPriceLookup.findMarketData(position.ticker()).orElse(null);
    }

    /** currentPrice here is the accrued factor (a "cota" born at 1.0) — see FixedIncomeValuationService. */
    private StockMarketData fixedIncomeMarketData(WalletPosition position) {
        var details = position.fixedIncomeDetails();
        LocalDate start = position.entries().stream()
                .map(PositionEntry::date)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
        java.math.BigDecimal factor = fixedIncomeValuationService.factorAt(
                details.indexer(), details.ratePercent(), start, LocalDate.now(), details.maturityDate());
        return new StockMarketData(factor, null, "fixed-income", details.displayName());
    }
}
