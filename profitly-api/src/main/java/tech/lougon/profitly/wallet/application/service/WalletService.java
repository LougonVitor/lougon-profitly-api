package tech.lougon.profitly.wallet.application.service;

import org.springframework.stereotype.Service;
import tech.lougon.profitly.wallet.application.dto.WalletSummaryDTO;
import tech.lougon.profitly.wallet.application.mapper.WalletMapper;
import tech.lougon.profitly.wallet.domain.model.Wallet;
import tech.lougon.profitly.wallet.domain.port.StockMarketData;
import tech.lougon.profitly.wallet.domain.port.StockPriceLookup;
import tech.lougon.profitly.wallet.domain.repository.WalletRepository;

import java.util.List;
import java.util.Map;
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

    public List<WalletSummaryDTO> findAll() {
        return walletRepository.findAll().stream()
                .map(wallet -> walletMapper.toSummaryDTO(wallet, resolveMarketData(wallet)))
                .toList();
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
