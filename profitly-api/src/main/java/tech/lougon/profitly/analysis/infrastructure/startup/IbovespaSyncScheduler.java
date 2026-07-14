package tech.lougon.profitly.analysis.infrastructure.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.analysis.application.service.IbovespaService;

@Component
public class IbovespaSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(IbovespaSyncScheduler.class);

    private final IbovespaService ibovespaService;

    public IbovespaSyncScheduler(IbovespaService ibovespaService) {
        this.ibovespaService = ibovespaService;
    }

    // Dev-only flag to repopulate the range cache on demand — turn off once done
    @Value("${profitly.sync.ibov-on-startup:false}")
    private boolean ibovSyncOnStartup;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void syncOnStartup() {
        if (!ibovSyncOnStartup) return;
        log.info("Running Ibovespa startup sync (all ranges)");
        syncAllRanges();
    }

    // During market hours: refresh intraday (1d/5m) every 15 minutes Mon-Fri 10h-17h55 BRT
    public void syncIntraday() {
        log.info("Ibovespa intraday sync triggered");
        ibovespaService.syncIntraday();
    }

    // After market close: refresh all ranges once (alongside analysis sync at 19h05)
    public void syncAllRanges() {
        log.info("Ibovespa full sync triggered (all ranges)");
        ibovespaService.syncAll();
    }
}
