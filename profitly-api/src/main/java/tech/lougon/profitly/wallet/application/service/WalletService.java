package tech.lougon.profitly.wallet.application.service;

import org.springframework.stereotype.Service;
import tech.lougon.profitly.wallet.application.dto.WalletSummaryDTO;
import tech.lougon.profitly.wallet.application.mapper.WalletMapper;
import tech.lougon.profitly.wallet.domain.model.Wallet;
import tech.lougon.profitly.wallet.domain.model.WalletPosition;
import tech.lougon.profitly.wallet.domain.port.StockMarketData;
import tech.lougon.profitly.wallet.domain.port.StockPriceLookup;
import tech.lougon.profitly.wallet.domain.repository.WalletRepository;
import tech.lougon.profitly.wallet.presentation.request.AddPositionRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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

    public WalletService(
            WalletRepository walletRepository,
            StockPriceLookup stockPriceLookup,
            WalletMapper walletMapper
    ) {
        this.walletRepository = walletRepository;
        this.stockPriceLookup = stockPriceLookup;
        this.walletMapper = walletMapper;
    }

    public Optional<WalletSummaryDTO> findById(String id) {
        return walletRepository.findById(id)
                .map(wallet -> walletMapper.toSummaryDTO(wallet, resolveMarketData(wallet)));
    }

    public WalletSummaryDTO addPosition(String walletId, AddPositionRequest request) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new NoSuchElementException("Wallet not found: " + walletId));

        List<WalletPosition> updatedPositions = upsertPosition(wallet, walletId, request);

        Wallet updated = new Wallet(wallet.id(), wallet.name(), wallet.userId(), updatedPositions, wallet.createdAt());
        Wallet saved = walletRepository.save(updated);

        return walletMapper.toSummaryDTO(saved, resolveMarketData(saved));
    }

    public List<WalletSummaryDTO> findAll() {
        return walletRepository.findAll().stream()
                .map(wallet -> walletMapper.toSummaryDTO(wallet, resolveMarketData(wallet)))
                .toList();
    }

    private List<WalletPosition> upsertPosition(Wallet wallet, String walletId, AddPositionRequest request) {
        Optional<WalletPosition> existing = wallet.positions().stream()
                .filter(p -> p.ticker().equalsIgnoreCase(request.ticker()))
                .findFirst();

        List<WalletPosition> positions = new ArrayList<>(wallet.positions());

        if (existing.isPresent()) {
            WalletPosition p = existing.get();
            int newQty = p.quantity() + request.quantity();
            BigDecimal newAvg = p.averagePrice().multiply(BigDecimal.valueOf(p.quantity()))
                    .add(request.averagePrice().multiply(BigDecimal.valueOf(request.quantity())))
                    .divide(BigDecimal.valueOf(newQty), 4, RoundingMode.HALF_UP);

            WalletPosition merged = new WalletPosition(p.id(), p.walletId(), p.ticker(), newQty, newAvg, p.createdAt());
            positions.replaceAll(pos -> pos.id().equals(p.id()) ? merged : pos);
        } else {
            positions.add(new WalletPosition(null, walletId, request.ticker().toUpperCase(), request.quantity(), request.averagePrice(), Instant.now()));
        }

        return positions;
    }

    private Map<String, StockMarketData> resolveMarketData(Wallet wallet) {
        return wallet.positions().stream()
                .collect(Collectors.toMap(
                        p -> p.ticker(),
                        p -> stockPriceLookup.findMarketData(p.ticker()).orElse(null),
                        (a, b) -> a
                ));
    }
}
