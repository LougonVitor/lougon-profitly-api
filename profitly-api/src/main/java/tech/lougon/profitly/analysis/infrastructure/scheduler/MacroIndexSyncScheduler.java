package tech.lougon.profitly.analysis.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.analysis.infrastructure.client.BrapiMacroClient;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiMacroResponse;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaMacroIndexValueRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.MacroIndexValueJpaEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Syncs the CDI/Selic/IPCA daily-index series that the wallet module uses to compute
 * fixed-income (renda fixa) accrual. A slug with no rows yet gets a one-shot backfill
 * (no startDate — brapi returns the most recent {@code limit} observations, which easily
 * covers the last decade for a daily series); everyone else gets a small incremental
 * window. Values are read back from the local table by {@code FixedIncomeValuationService} —
 * never call brapi during a user request.
 */
@Component
public class MacroIndexSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(MacroIndexSyncScheduler.class);
    private static final List<String> SLUGS = List.of("cdi", "selic", "ipca");
    private static final int BACKFILL_LIMIT = 10000;
    private static final int INCREMENTAL_LIMIT = 60;

    private final BrapiMacroClient client;
    private final JpaMacroIndexValueRepository repository;

    public MacroIndexSyncScheduler(BrapiMacroClient client, JpaMacroIndexValueRepository repository) {
        this.client = client;
        this.repository = repository;
    }

    // Dev-only flag while the renda-fixa screen is under construction — turn off when done
    @Value("${profitly.sync.macro-on-startup:false}")
    private boolean macroSyncOnStartup;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void syncOnStartup() {
        if (!macroSyncOnStartup) return;
        log.info("Running macro index startup sync");
        syncAll();
    }

    public void syncAll() {
        List<String> backfill = new ArrayList<>();
        List<String> incremental = new ArrayList<>();
        for (String slug : SLUGS) {
            if (repository.countBySlug(slug) == 0) backfill.add(slug);
            else incremental.add(slug);
        }

        int saved = 0;
        if (!backfill.isEmpty()) {
            log.info("Macro index backfill: {}", backfill);
            saved += syncBatch(String.join(",", backfill), null, BACKFILL_LIMIT);
        }
        if (!incremental.isEmpty()) {
            String startDate = LocalDate.now().minusDays(30).toString();
            saved += syncBatch(String.join(",", incremental), startDate, INCREMENTAL_LIMIT);
        }
        log.info("Macro index sync complete: {} new observations", saved);
    }

    private int syncBatch(String symbols, String startDate, int limit) {
        List<BrapiMacroResponse.SeriesResult> results = client.fetchSeries(symbols, startDate, limit);
        int saved = 0;
        for (var result : results) {
            saved += saveSeries(result);
        }
        return saved;
    }

    private int saveSeries(BrapiMacroResponse.SeriesResult result) {
        if (result.series() == null || result.series().slug() == null || result.observations() == null) return 0;
        String slug = result.series().slug();

        Set<LocalDate> existingDates = new HashSet<>();
        for (var obs : result.observations()) {
            LocalDate date = parseDate(obs.date());
            if (date == null) continue;
            existingDates.add(date);
        }
        if (existingDates.isEmpty()) return 0;

        LocalDate min = existingDates.stream().min(LocalDate::compareTo).orElseThrow();
        LocalDate max = existingDates.stream().max(LocalDate::compareTo).orElseThrow();
        Set<LocalDate> already = repository.findBySlugAndDateBetweenOrderByDateAsc(slug, min, max)
                .stream().map(MacroIndexValueJpaEntity::getDate)
                .collect(Collectors.toSet());

        Instant now = Instant.now();
        List<MacroIndexValueJpaEntity> toSave = new ArrayList<>();
        for (var obs : result.observations()) {
            if (obs.value() == null) continue;
            LocalDate date = parseDate(obs.date());
            if (date == null || !already.add(date)) continue;

            var entity = new MacroIndexValueJpaEntity();
            entity.setSlug(slug);
            entity.setDate(date);
            entity.setValue(BigDecimal.valueOf(obs.value()));
            entity.setSyncedAt(now);
            toSave.add(entity);
        }
        repository.saveAll(toSave);
        return toSave.size();
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null) return null;
        try {
            return LocalDate.parse(raw.length() > 10 ? raw.substring(0, 10) : raw);
        } catch (Exception e) {
            return null;
        }
    }
}
