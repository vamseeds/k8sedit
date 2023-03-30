package com.oracle.k8sedit;

import com.google.common.flogger.FluentLogger;

/**
 * Invoke system exit on informer failure
 */
public class DefaultInformerFailureAction implements Runnable {
    private static final FluentLogger log = FluentLogger.forEnclosingClass();

    @Override
    public void run() {
        log.atSevere().log("Kubernetes resource informer failed, invoking system exit.");
        System.exit(1);
    }
}
