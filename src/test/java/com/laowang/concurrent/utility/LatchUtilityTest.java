package com.laowang.concurrent.utility;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class LatchUtilityTest {

    private ExecutorService executorService = Executors.newFixedThreadPool(2);

    private void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
        }
    }

    @org.junit.jupiter.api.Test
    void submitTask1() {
        LatchUtility.submitTask(executorService, () -> {
            log.info("task1");
            sleep(3);
        });
        LatchUtility.submitTask(executorService, () -> {
            log.info("task2");

            sleep(2);

        });
        LatchUtility.submitTask(executorService, () -> {
            log.info("task3");
        });
        assertTrue(LatchUtility.waitFor(6L));
    }

    @org.junit.jupiter.api.Test
    void submitTask2() {
        LatchUtility.submitTask(executorService, () -> {
            log.info("task1");
            sleep(3);
            log.info("task1 done");
        });
        LatchUtility.submitTask(executorService, () -> {
            log.info("task2");

            sleep(2);

            log.info("task2 done");

        });
        LatchUtility.submitTask(executorService, () -> {
            log.info("task3");
        });
        assertTrue(LatchUtility.waitFor(4L));
    }

}