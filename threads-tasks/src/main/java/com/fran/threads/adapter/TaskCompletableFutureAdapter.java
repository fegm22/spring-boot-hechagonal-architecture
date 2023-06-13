
package com.fran.threads.adapter;

import com.fran.task.domain.model.Task;
import com.fran.task.domain.port.TaskManager;
import com.fran.threads.exception.CounterTaskNotFoundException;
import com.fran.threads.model.TaskCompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskCompletableFutureAdapter implements TaskManager {

    private final Map<String, TaskCompletableFuture> taskRegister;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;

    @SneakyThrows
    public void cancelTask(String taskId) {
        TaskCompletableFuture tasktaskCompletableFuture = taskRegister.get(taskId);
        if (tasktaskCompletableFuture.completableFuture() != null) {
            tasktaskCompletableFuture.completableFuture().cancel(true);
        }
        taskRegister.remove(taskId);
    }

    public Task executeTask(Task task) {
        if (task == null) {
            throw new CounterTaskNotFoundException("Failed to find counter with ID ");
        }

        CompletableFuture<Task> completableFuture = CompletableFuture.supplyAsync(() -> {
            for (int i = task.getBegin(); i <= task.getFinish(); i++) {
                task.setProgress(i);
                log.info("Counter progress from CompletableFuture is '{}' for '{}' running in '{}'", task.getProgress(), task.getId(), Thread.currentThread().getName());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return task;
        }, executorService);

        completableFuture.thenRun(() -> taskRegister.remove(task.getId()));

        taskRegister.put(task.getId(), new TaskCompletableFuture(task, completableFuture));

        return task;
    }

    public List<Task> getAllRunningCounters() {
        return taskRegister.values().stream()
                .map(TaskCompletableFuture::task)
                .filter(task -> task.getFinish() > task.getProgress())
                .toList();
    }

    public Task getRunningCounter(String counterId) {
        if (taskRegister.isEmpty() || taskRegister.get(counterId) == null) {
            log.error("Failed to find counter with ID " + counterId);
            throw new CounterTaskNotFoundException("Failed to find counter with ID " + counterId);
        }
        TaskCompletableFuture taskThread = taskRegister.get(counterId);
        log.info("Progress from from CompletableFuture is '{}' for '{}' running in '{}' ", taskThread.task().getProgress(), taskThread.task().getId(), taskThread.completableFuture());
        return taskThread.task();
    }

    public Flux<Task> startReceivingMessages() {
        return null;
    }

    @PostConstruct
    private void createScheduleRemoveTask() {
        Runnable deleteTasksSchedule = () -> {
            if (taskRegister != null && !taskRegister.isEmpty()) {
                log.info("Schedule executing every 5 minutes will remove {} tasks from the Running CompletableFuture Register.", taskRegister.size());
                taskRegister.clear();
            } else {
                log.info("Not running tasks will be deleted from Running CompletableFuture Register");
            }
        };
        scheduledExecutorService.scheduleAtFixedRate(deleteTasksSchedule, 0, 5, TimeUnit.MINUTES);
    }

}
