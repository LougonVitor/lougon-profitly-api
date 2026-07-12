package tech.lougon.profitly.analysis.infrastructure.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.analysis.application.service.IbovespaService;

@Component
public class IbovespaSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(IbovespaSyncScheduler.class);

    private final IbovespaService ibovespaService;

    public IbovespaSyncScheduler(IbovespaService ibovespaService) {
        this.ibovespaService = ibovespaService;
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
