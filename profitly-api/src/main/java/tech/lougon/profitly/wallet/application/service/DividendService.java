package tech.lougon.profitly.wallet.application.service;

import org.springframework.stereotype.Service;
import tech.lougon.profitly.wallet.domain.model.Dividend;
import tech.lougon.profitly.wallet.domain.repository.DividendRepository;
import tech.lougon.profitly.wallet.presentation.request.AddDividendRequest;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class DividendService {

    private final DividendRepository repository;

    public DividendService(DividendRepository repository) {
        this.repository = repository;
    }

    public List<Dividend> findByWallet(String walletId) {
        return repository.findByWalletId(walletId);
    }

    public Dividend add(String walletId, String userId, AddDividendRequest req) {
        var dividend = new Dividend(null, walletId, userId,
                req.ticker().toUpperCase(), req.totalAmount(), req.paymentDate(),
                req.type(), req.received(), Instant.now());
        return repository.save(dividend);
    }

    public Dividend toggleReceived(String id, String userId) {
        var d = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Dividend not found: " + id));
        if (!d.userId().equals(userId)) throw new IllegalArgumentException("Acesso negado");
        var updated = new Dividend(d.id(), d.walletId(), d.userId(), d.ticker(),
                d.totalAmount(), d.paymentDate(), d.type(), !d.received(), d.createdAt());
        return repository.save(updated);
    }

    public void delete(String id, String userId) {
        var d = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Dividend not found: " + id));
        if (!d.userId().equals(userId)) throw new IllegalArgumentException("Acesso negado");
        repository.deleteById(id);
    }
}
