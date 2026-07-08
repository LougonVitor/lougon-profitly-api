package tech.lougon.profitly.finance.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.finance.application.service.FinanceService;
import tech.lougon.profitly.finance.domain.repository.FinanceSettingsRepository;

import java.util.List;

/**
 * Closes each user's budget period automatically once their configured reset day
 * arrives, archiving the current expenses into history. Runs daily at 00:05 BRT and
 * defers the actual date check to {@link FinanceService#checkAndResetIfDue(String)}.
 * This is pure database work (no brapi), so it is enabled by default.
 */
@Component
public class FinancePeriodResetScheduler {

    private static final Logger log = LoggerFactory.getLogger(FinancePeriodResetScheduler.class);

    private final FinanceService financeService;
    private final FinanceSettingsRepository settingsRepository;

    @Value("${profitly.finance.auto-close.enabled:true}")
    private boolean enabled;

    public FinancePeriodResetScheduler(FinanceService financeService,
                                       FinanceSettingsRepository settingsRepository) {
        this.financeService = financeService;
        this.settingsRepository = settingsRepository;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "America/Sao_Paulo")
    public void autoCloseDuePeriods() {
        if (!enabled) return;
        List<String> userIds = settingsRepository.findAllUserIds();
        int closed = 0;
        for (String userId : userIds) {
            try {
                boolean wasDue = financeService.checkAndResetIfDue(userId);
                if (wasDue) closed++;
            } catch (Exception e) {
                log.warn("Auto-close failed for user {}: {}", userId, e.getMessage());
            }
        }
        if (closed > 0) log.info("Finance auto-close: {} period(s) archived", closed);
    }
}
