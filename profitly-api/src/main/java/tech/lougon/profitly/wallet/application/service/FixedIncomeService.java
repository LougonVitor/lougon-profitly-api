package tech.lougon.profitly.wallet.application.service;

import org.springframework.stereotype.Service;
import tech.lougon.profitly.wallet.application.dto.WalletSummaryDTO;
import tech.lougon.profitly.wallet.application.mapper.WalletMapper;
import tech.lougon.profitly.wallet.domain.model.EntryType;
import tech.lougon.profitly.wallet.domain.model.FixedIncomeDetails;
import tech.lougon.profitly.wallet.domain.model.FixedIncomeInstrumentType;
import tech.lougon.profitly.wallet.domain.model.Indexer;
import tech.lougon.profitly.wallet.domain.model.PositionEntry;
import tech.lougon.profitly.wallet.domain.model.Wallet;
import tech.lougon.profitly.wallet.domain.model.WalletPosition;
import tech.lougon.profitly.wallet.domain.repository.WalletRepository;
import tech.lougon.profitly.wallet.presentation.request.AddFixedIncomeEntryRequest;
import tech.lougon.profitly.wallet.presentation.request.RedeemFixedIncomeRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Creates and redeems renda-fixa positions (CDB/LCI/LCA/LC/LF/RDB). Each application is
 * its own independent {@link WalletPosition} — a synthetic ticker ({@code rf-<uuid>}) with
 * a single BUY entry where {@code quantity} is the principal and {@code paidPrice} is
 * always 1.0 (a "cota" born at 1.0 that accrues via {@link FixedIncomeValuationService}).
 * Redemption in V1 is always total: it closes the position with a SELL entry whose price
 * is the accrued factor computed server-side, never supplied by the client.
 */
@Service
public class FixedIncomeService {

    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final WalletMapper walletMapper;
    private final FixedIncomeValuationService valuationService;

    public FixedIncomeService(WalletService walletService,
                              WalletRepository walletRepository,
                              WalletMapper walletMapper,
                              FixedIncomeValuationService valuationService) {
        this.walletService = walletService;
        this.walletRepository = walletRepository;
        this.walletMapper = walletMapper;
        this.valuationService = valuationService;
    }

    public WalletSummaryDTO createPosition(String walletId, AddFixedIncomeEntryRequest request, String userId) {
        Wallet wallet = walletService.requireOwned(walletId, userId);

        if (request.existingTicker() != null && !request.existingTicker().isBlank()) {
            return addToExistingPosition(wallet, request);
        }

        // Daily-liquidity products can be redeemed anytime, so a maturity date isn't
        // required to track them; everything else needs one to cap accrual against.
        if (request.maturityDate() == null && !request.dailyLiquidity()) {
            throw new IllegalArgumentException("Informe a data de vencimento ou marque liquidez diária");
        }
        if (request.maturityDate() != null && !request.maturityDate().isAfter(request.transactionDate())) {
            throw new IllegalArgumentException("Data de vencimento deve ser posterior à data da transação");
        }

        FixedIncomeDetails details = new FixedIncomeDetails(
                request.issuer(),
                parseInstrumentType(request.instrumentType()),
                parseIndexer(request.indexer()),
                request.ratePercent(),
                request.dailyLiquidity(),
                request.maturityDate()
        );

        PositionEntry entry = new PositionEntry(
                null, null, request.transactionDate(), request.principal(), BigDecimal.ONE, EntryType.BUY, Instant.now()
        );
        WalletPosition position = new WalletPosition(
                null, walletId, syntheticTicker(), List.of(entry), Instant.now(), details
        );

        List<WalletPosition> updatedPositions = new ArrayList<>(wallet.positions());
        updatedPositions.add(position);

        return save(wallet, updatedPositions);
    }

    /**
     * Adds another aporte to an already-existing renda-fixa position instead of creating a
     * new one — e.g. topping up the same CDB Inter 100% CDI over time. The position's own
     * stored terms (issuer/instrumentType/indexer/ratePercent/dailyLiquidity/maturityDate)
     * are authoritative; whatever the client sent for those fields is ignored.
     */
    private WalletSummaryDTO addToExistingPosition(Wallet wallet, AddFixedIncomeEntryRequest request) {
        WalletPosition position = wallet.positions().stream()
                .filter(p -> p.ticker().equalsIgnoreCase(request.existingTicker()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Position not found: " + request.existingTicker()));

        if (!position.isFixedIncome()) {
            throw new IllegalArgumentException("Lançamento selecionado não é de renda fixa");
        }

        PositionEntry entry = new PositionEntry(
                null, position.id(), request.transactionDate(), request.principal(), BigDecimal.ONE, EntryType.BUY, Instant.now()
        );
        List<PositionEntry> updatedEntries = new ArrayList<>(position.entries());
        updatedEntries.add(entry);
        WalletPosition updatedPosition = position.withEntries(updatedEntries);

        List<WalletPosition> updatedPositions = wallet.positions().stream()
                .map(p -> p.id().equals(position.id()) ? updatedPosition : p)
                .toList();

        return save(wallet, updatedPositions);
    }

    public WalletSummaryDTO redeem(String walletId, String ticker, RedeemFixedIncomeRequest request, String userId) {
        Wallet wallet = walletService.requireOwned(walletId, userId);
        WalletPosition position = wallet.positions().stream()
                .filter(p -> p.ticker().equalsIgnoreCase(ticker))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Position not found: " + ticker));

        if (!position.isFixedIncome()) {
            throw new IllegalArgumentException("Posição não é de renda fixa");
        }

        BigDecimal quantity = position.totalQuantity();
        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException("Posição já foi resgatada");
        }

        LocalDate start = position.entries().stream()
                .map(PositionEntry::date)
                .min(LocalDate::compareTo)
                .orElseThrow();
        if (request.date().isBefore(start)) {
            throw new IllegalArgumentException("Data de resgate anterior à data de aplicação");
        }

        FixedIncomeDetails details = position.fixedIncomeDetails();
        BigDecimal factor = valuationService.factorAt(
                details.indexer(), details.ratePercent(), start, request.date(), details.maturityDate());

        PositionEntry redeemEntry = new PositionEntry(
                null, position.id(), request.date(), quantity, factor, EntryType.SELL, Instant.now()
        );
        List<PositionEntry> updatedEntries = new ArrayList<>(position.entries());
        updatedEntries.add(redeemEntry);
        WalletPosition updatedPosition = position.withEntries(updatedEntries);

        List<WalletPosition> updatedPositions = wallet.positions().stream()
                .map(p -> p.id().equals(position.id()) ? updatedPosition : p)
                .toList();

        return save(wallet, updatedPositions);
    }

    private WalletSummaryDTO save(Wallet wallet, List<WalletPosition> updatedPositions) {
        Wallet updated = new Wallet(wallet.id(), wallet.name(), wallet.userId(), updatedPositions, wallet.createdAt(), wallet.source());
        Wallet saved = walletRepository.save(updated);
        return walletMapper.toSummaryDTO(saved, walletService.resolveMarketData(saved));
    }

    private static String syntheticTicker() {
        return "rf-" + UUID.randomUUID();
    }

    private static FixedIncomeInstrumentType parseInstrumentType(String raw) {
        try {
            return FixedIncomeInstrumentType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de título inválido: " + raw);
        }
    }

    private static Indexer parseIndexer(String raw) {
        try {
            return Indexer.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Indexador inválido: " + raw);
        }
    }
}
