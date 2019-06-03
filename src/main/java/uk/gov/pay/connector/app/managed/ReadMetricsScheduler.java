package uk.gov.pay.connector.app.managed;

import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;

import javax.inject.Inject;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReadMetricsScheduler implements Managed {

    private static final String READ_METRICS_THREAD_NAME = "read-metrics-scheduler";

    private final ScheduledExecutorService scheduledExecutorService;
    private ChargesAwaitingCaptureMetric chargesAwaitingCaptureMetric;

    @Inject
    public ReadMetricsScheduler(Environment environment, ChargesAwaitingCaptureMetric chargesAwaitingCaptureMetric) {
        this.chargesAwaitingCaptureMetric = chargesAwaitingCaptureMetric;

        scheduledExecutorService = environment
                .lifecycle()
                .scheduledExecutorService(READ_METRICS_THREAD_NAME)
                .build();
    }

    @Override
    public void start() {
        //todo: values
        long initialDelay = 0;
        long delay = 0;

        scheduledExecutorService.scheduleWithFixedDelay(
                () -> chargesAwaitingCaptureMetric.measure(),
                initialDelay,
                delay,
                TimeUnit.SECONDS
        );
    }

    @Override
    public void stop() {
        scheduledExecutorService.shutdown();
    }
}
