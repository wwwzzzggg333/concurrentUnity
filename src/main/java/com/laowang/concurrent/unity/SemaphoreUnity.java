package com.laowang.concurrent.unity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

@Slf4j
public class SemaphoreUnity {

    @AllArgsConstructor
    @Data
    private static class TaskInfo {
        private Executor executor;
        private Runnable[] runnables;
    }

    private static final ThreadLocal<List<TaskInfo>> THREADLOCAL = ThreadLocal.withInitial(LinkedList::new);

    public static void submitTask(Executor executor, Runnable after) {
        THREADLOCAL.get().add(new TaskInfo(executor, new Runnable[]{null, after}));
    }

    public static void submitTask(Executor executor, Runnable before, Runnable after) {
        THREADLOCAL.get().add(new TaskInfo(executor, new Runnable[]{before, after}));
    }

    private static List<TaskInfo> popTask() {
        List<TaskInfo> taskInfos = THREADLOCAL.get();
        THREADLOCAL.remove();
        return taskInfos;
    }

    public static void start(int maxSemaphore) {
        List<TaskInfo> taskInfos = popTask();
        if (taskInfos.isEmpty()) {
            return;
        }

        Semaphore semaphore = new Semaphore(maxSemaphore);
        for (TaskInfo taskInfo : taskInfos) {
            Executor executor = taskInfo.executor;
            Runnable before = taskInfo.runnables[0];
            Runnable after = taskInfo.runnables[1];

            executor.execute(() -> {
                try {
                    if (before != null) {
                        before.run();
                    }
                    semaphore.acquire();
                    if (after != null) {
                        after.run();
                    }
                } catch (InterruptedException e) {
                    log.info(" ", e);
                } finally {
                    semaphore.release();
                }
            });
        }

    }

}
