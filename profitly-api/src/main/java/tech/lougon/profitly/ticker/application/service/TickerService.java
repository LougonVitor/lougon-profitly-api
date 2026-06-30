package tech.lougon.profitly.ticker.application.service;

import org.springframework.stereotype.Service;
import tech.lougon.profitly.ticker.application.dto.TickerDTO;
import tech.lougon.profitly.ticker.domain.model.Ticker;
import tech.lougon.profitly.ticker.domain.repository.TickerRepository;
import tech.lougon.profitly.ticker.infrastructure.client.BrapiClient;
import tech.lougon.profitly.ticker.infrastructure.client.dto.BrapiTickerResponse;

import java.time.Instant;
import java.util.List;

@Service
public class TickerService {

    private final TickerRepository tickerRepository;
    private final BrapiClient brapiClient;

    public TickerService(TickerRepository tickerRepository, BrapiClient brapiClient) {
        this.tickerRepository = tickerRepository;
        this.brapiClient = brapiClient;
    }

    public List<TickerDTO> findAll() {
        return tickerRepository.findAll().stream().map(this::toDTO).toList();
    }

    public void syncAll() {
        List<BrapiTickerResponse.TickerItem> items = brapiClient.fetchAllTickers();

        for (BrapiTickerResponse.TickerItem item : items) {
            if (item.symbol() == null || item.symbol().isBlank()) continue;

            BrapiTickerResponse.QuoteSummary q = item.quote();
            Ticker ticker = new Ticker(
                    null, item.symbol(), item.name(), item.longName(),
                    item.assetType(), item.subType(), item.exchange(), item.currency(),
                    item.sector(), item.isActive(), item.logoUrl(),
                    q != null ? q.lastPrice() : null,
                    q != null ? q.changePercent() : null,
                    q != null ? q.volume() : null,
                    q != null ? q.marketCap() : null,
                    Instant.now()
            );
            tickerRepository.save(ticker);
        }
    }

    private TickerDTO toDTO(Ticker t) {
        return new TickerDTO(
                t.symbol(), t.name(), t.longName(),
                t.assetType(), t.subType(), t.sector(), t.isActive(), t.logoUrl(),
                t.lastPrice(), t.changePercent(), t.volume(), t.marketCap()
        );
    }
}
