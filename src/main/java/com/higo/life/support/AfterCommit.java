package com.higo.life.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Best-effort Redis projections. Durable business writes remain in the database. */
public final class AfterCommit {
    private static final Logger LOG = LoggerFactory.getLogger(AfterCommit.class);
    private AfterCommit() {}

    public static void run(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try { action.run(); }
                catch (RuntimeException ex) {
                    // A committed write must not be reported as rolled back. Reconcile stock after recovery.
                    LOG.warn("Database committed; Redis projection failed: {}", ex.getClass().getSimpleName());
                }
            }
        });
    }
}
