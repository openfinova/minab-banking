package com.openfinova.banking.tan;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.openfinova.banking.tan.api.TanService;
import com.openfinova.banking.tan.service.TanAuthorizationService;

/**
 * Facade implementation of {@link TanService} for cross-module callers (e.g. transaction processing).
 *
 * Contains no business logic: each method delegates to the appropriate internal service so other
 * modules depend only on {@code tan-api} and never on {@code tan-service} internals.
 *
 * @see TanService
 * @see TanAuthorizationService
 */
@Service
public class TanServiceImpl implements TanService {

    private final TanAuthorizationService tanAuthorizationService;

    public TanServiceImpl(TanAuthorizationService tanAuthorizationService) {
        this.tanAuthorizationService = tanAuthorizationService;
    }

    @Override
    public boolean isScaVerified(UUID transactionId) {
        return tanAuthorizationService.isScaVerified(transactionId);
    }
}
