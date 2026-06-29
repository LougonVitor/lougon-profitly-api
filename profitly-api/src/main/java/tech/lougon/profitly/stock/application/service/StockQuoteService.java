package tech.lougon.profitly.stock.application.service;

import org.springframework.stereotype.Service;
import tech.lougon.profitly.stock.application.dto.StockQuoteDTO;
import tech.lougon.profitly.stock.application.mapper.ApiStockQuoteMapper;
import tech.lougon.profitly.stock.domain.model.StockQuote;
import tech.lougon.profitly.stock.domain.repository.StockRepository;
import tech.lougon.profitly.stock.infrastructure.client.BrapiStockClient;
import tech.lougon.profitly.stock.infrastructure.client.dto.BrapiQuoteResponse;

import java.util.List;
import java.util.Optional;

@Service
public class StockQuoteService {

    private final StockRepository stockRepository;
    private final BrapiStockClient brapiStockClient;
    private final ApiStockQuoteMapper stockMapper;

    public StockQuoteService(
            StockRepository stockRepository,
            BrapiStockClient brapiStockClient,
            ApiStockQuoteMapper stockMapper
    ) {
        this.stockRepository = stockRepository;
        this.brapiStockClient = brapiStockClient;
        this.stockMapper = stockMapper;
    }

    public StockQuoteDTO syncFromBrapi(String ticker) {
        BrapiQuoteResponse response = brapiStockClient.fetchQuote(ticker);

        BrapiQuoteResponse.BrapiQuoteResult result = response.results().getFirst();

        StockQuote stockQuote = stockMapper.toDomain(result, response);
        StockQuote saved = stockRepository.save(stockQuote);

        return stockMapper.toDTO(saved);
    }

    public Optional<StockQuoteDTO> findByTicker(String ticker) {
        return stockRepository.findByTicker(ticker)
                .map(stockMapper::toDTO);
    }

    public List<StockQuoteDTO> syncManyFromBrapi(List<String> tickers) {
        if (tickers.isEmpty()) return List.of();

        BrapiQuoteResponse response = brapiStockClient.fetchQuotes(tickers);

        return response.results().stream()
                .map(result -> {
                    StockQuote stockQuote = stockMapper.toDomain(result, response);
                    StockQuote saved = stockRepository.save(stockQuote);
                    return stockMapper.toDTO(saved);
                })
                .toList();
    }

    public List<StockQuoteDTO> findAll() {
        return stockRepository.findAll()
                .stream()
                .map(stockMapper::toDTO)
                .toList();
    }
}