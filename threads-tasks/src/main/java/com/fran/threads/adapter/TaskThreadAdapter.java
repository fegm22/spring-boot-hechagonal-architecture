package com.fran.threads.adapter;

import com.fran.task.domain.model.Task;
import com.fran.task.domain.port.TaskManager;
import com.fran.threads.exception.CounterTaskNotFoundException;
import com.fran.threads.model.TaskThread;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskThreadAdapter implements TaskManager {

    private final Map<String, TaskThread> taskRegister;
    private final ExecutorService executorService;

    @Override
    public void cancelTask(String taskId) {
        TaskThread taskThread = taskRegister.get(taskId);
        if (taskThread.thread() != null) {
            taskThread.thread().interrupt();
        }
    }

    @Override
    public Task executeTask(Task task) {
        if (task == null) {
            throw new CounterTaskNotFoundException("Failed to find counter with ID ");
        }
        executorService.execute(() -> {
            taskRegister.put(task.getId(), new TaskThread(task, Thread.currentThread()));
            for (int i = task.getBegin(); i <= task.getFinish(); i++) {
                task.setProgress(i);
                log.info("Counter progress is '{}' for '{}' running in '{}' ", task.getProgress(), task.getId(), Thread.currentThread().getName());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        return task;
    }

    @Override
    public List<Task> getAllRunningCounters() {
        return taskRegister.values().stream()
                .map(TaskThread::task)
                .filter(task -> task.getFinish() > task.getProgress())
                .toList();
    }

    @Override
    public Task getRunningCounter(String counterId) {
        if (taskRegister.isEmpty() || taskRegister.get(counterId) == null) {
            log.error("Failed to find counter with ID " + counterId);
            throw new CounterTaskNotFoundException("Failed to find counter with ID " + counterId);
        }
        TaskThread taskThread = taskRegister.get(counterId);
        log.info("Counter progress is '{}' for '{}' running in '{}' ", taskThread.task().getProgress(), taskThread.task().getId(), taskThread.thread().getName());
        return taskThread.task();
    }

    @Override
    public Flux<Task> startReceivingMessages() {
        return null;
    }

}
