package org.donorly.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.donorly.backend.service.OrgLogoService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** One-time migration of legacy base64 logo_data rows to GCS when bucket is configured. */
@Component
@RequiredArgsConstructor
@Slf4j
public class LogoDataMigrator implements ApplicationRunner {

    private final OrgLogoService orgLogoService;

    @Override
    public void run(ApplicationArguments args) {
        int migrated = orgLogoService.migrateLegacyLogoData();
        if (migrated > 0) {
            log.info("Logo migration complete: {} organization(s) moved to GCS", migrated);
        }
    }
}
