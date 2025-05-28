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

    @org.junit.jupiter.api.Test
    void submitTask4() {
    }

    void yourSubmitTask(Object parm1, Object parm2) {
        //创建线程并行处理parm1和parm2，主线程等待两个线程处理完成再继续执行
        CountDownLatch countDownLatch = new CountDownLatch(2);
        executorService.execute(() -> {
            try {

                log.info("task1");
                //process parm1
            } finally {
                countDownLatch.countDown();
            }
        });

        executorService.execute(() -> {
            try {

                log.info("task2");
                //process parm2
            } finally {
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {

        }

        //go on do something
    }


    void mySubmitTask(Object parm1, Object parm2) {
        //创建线程并行处理parm1和parm2，主线程等待两个线程处理完成再继续执行
        LatchUtility.submitTask(executorService, () -> {
            log.info("task1");
            //process parm1
        });

        LatchUtility.submitTask(executorService, () -> {
            log.info("task2");
            //process parm2
        });

        LatchUtility.waitFor(10L);
        //go on do something
    }
}