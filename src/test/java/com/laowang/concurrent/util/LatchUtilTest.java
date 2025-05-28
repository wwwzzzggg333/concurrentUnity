package com.laowang.concurrent.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class LatchUtilTest {

    private ExecutorService executorService = Executors.newFixedThreadPool(2);

    private void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
        }
    }

    @org.junit.jupiter.api.Test
    void submitTask1() {
        LatchUtils.submitTask(executorService, () -> {
            log.info("task1");
            sleep(3);
        });
        LatchUtils.submitTask(executorService, () -> {
            log.info("task2");

            sleep(2);

        });
        LatchUtils.submitTask(executorService, () -> {
            log.info("task3");
        });
        assertTrue(LatchUtils.waitFor(6L));
    }

    @org.junit.jupiter.api.Test
    void submitTask2() {
        LatchUtils.submitTask(executorService, () -> {
            log.info("task1");
            sleep(3);
            log.info("task1 done");
        });
        LatchUtils.submitTask(executorService, () -> {
            log.info("task2");

            sleep(2);

            log.info("task2 done");

        });
        LatchUtils.submitTask(executorService, () -> {
            log.info("task3");
        });
        assertTrue(LatchUtils.waitFor(4L));
    }

}