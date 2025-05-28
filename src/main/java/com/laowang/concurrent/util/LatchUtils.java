package com.laowang.concurrent.util;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LatchUtils {

    @AllArgsConstructor
    private static final class TaskInfo {
        private Executor executor;
        private Runnable runnable;
    }

    private static final ThreadLocal<List<TaskInfo>> THREADLOCAL = ThreadLocal.withInitial(LinkedList::new);

    public static void submitTask(Executor executor, Runnable runnable) {
        THREADLOCAL.get().add(new TaskInfo(executor, runnable));
    }

    private static List<TaskInfo> popTask() {
        List<TaskInfo> taskInfos = THREADLOCAL.get();
        THREADLOCAL.remove();
        return taskInfos;
    }

    public static boolean waitFor(Long seconds) {
        List<TaskInfo> taskInfos = popTask();
        if (taskInfos.isEmpty()) {
            return true;
        }

        CountDownLatch latch = new CountDownLatch(taskInfos.size());
        for (TaskInfo taskInfo : taskInfos) {
            Executor executor = taskInfo.executor;
            Runnable runnable = taskInfo.runnable;
            executor.execute(() -> {
                try {
                    runnable.run();
                } finally {
                    latch.countDown();
                }
            });
        }
        boolean await = false;
        try {
            if (seconds != null) {
                await = latch.await(seconds, TimeUnit.SECONDS);
            } else {
                latch.await();
                await = true;
            }
        } catch (Exception e) {
            log.error("error", e);
        }

        return await;
    }

}
