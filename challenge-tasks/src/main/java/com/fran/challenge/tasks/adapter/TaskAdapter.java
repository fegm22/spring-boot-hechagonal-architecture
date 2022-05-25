package com.fran.challenge.tasks.adapter;

import com.fran.challenge.domain.model.Task;
import com.fran.challenge.domain.port.*;
import com.fran.challenge.persistence.adapter.PersistenceAdapter;
import com.fran.challenge.tasks.mapper.CounterMapper;
import com.fran.challenge.tasks.model.Counter;
import com.fran.challenge.tasks.service.CounterService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskAdapter
        implements CreateTaskPort, ReadTaskPort, UpdateTaskPort, DeleteTaskPort, ExecuteTaskPort, CancelTaskPort {

    private final PersistenceAdapter persistenceAdapter;
    private final CounterService counterService;
    private final CounterMapper mapper;

    @Override
    public Task createTask(Task task) {
        return persistenceAdapter.createTask(task);
    }

    @Override
    public List<Task> getTasks() {
        return persistenceAdapter.getTasks();
    }

    @Override
    public Optional<Task> getTask(String taskId) {
        return persistenceAdapter.getTask(taskId);
    }

    @Override
    public Task updateTask(String taskId, Task taskUpdate) {
        return persistenceAdapter.updateTask(taskId, taskUpdate);
    }

    @Override
    public void deleteTask(String taskId) {
        persistenceAdapter.deleteTask(taskId);
    }

    @Override
    public void cancelTask(String taskId) {
        counterService.cancelCounter(taskId);
    }

    public List<Task> getAllRunningCounters() {
        return mapper.toDomain(counterService.getAllRunningCounters());
    }

    @SneakyThrows
    public Task getRunningCounter(final String counterId) {
        return mapper.toDomain(counterService.getRunningCounter(counterId));
    }

    @SneakyThrows
    @Override
    public void executeTask(Task task) {
        executeCounterTask(task);
    }

    public void executeCounterTask(Task task) throws SchedulerException {
        Counter counter = mapper.toCounter(task);
        counterService.runCounterJob(counter);
        persistenceAdapter.updateExecution(task);
    }

}
