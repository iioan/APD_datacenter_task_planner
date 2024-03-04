/* Implement this class. */

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class MyHost extends Host {
    PriorityBlockingQueue<Task> tasks = new PriorityBlockingQueue<>(15, new TaskPriorityComparator());
    volatile AtomicBoolean hostRunning = new AtomicBoolean(true);
    // folosit pentru a sti daca exista un task in executie
    // volatile pentru thread safety.
    AtomicBoolean taskRunning = new AtomicBoolean(false);
    Task currentTask = null; // task-ul care este executat
    static Semaphore semRunning = new Semaphore(1);
    // semRunning este utilizat in special pentru functia Least Work Left.
    // Actonand acquire() in leftWorkLeft(), temporar oprim executia task-ului
    // pentru a obtine o estimare cat mai precisa a timpului ramas pentru fiecare nod.

    @Override
    public void run() {
        while (hostRunning.get()) {
            currentTask = tasks.poll();
            if (currentTask != null) {
                // incepe executarea taskului
                while (currentTask.getLeft() > 0) {
                    taskRunning.set(true);
                    try {
                        semRunning.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // se verifica daca avem un task mai important in coada
                    // + cel curent poate fi oprit pentru a-l rula pe cel mai important
                    Task nextTask = tasks.peek();
                    if (currentTask.isPreemptible() && nextTask != null &&
                            nextTask.getPriority() > currentTask.getPriority()) {
                        tasks.add(currentTask);
                        semRunning.release();
                        break;
                    }
                    semRunning.release();

                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    currentTask.setLeft(currentTask.getLeft() - 30);
                }
                taskRunning.set(false);
                currentTask.finish();
                currentTask = null;
            }
        }
    }


    @Override
    public void addTask(Task task) {
        tasks.add(task);
        // acum se pot adauga alte task-uri din dispatcher in noduri
        MyDispatcher.semaphore.release();
    }

    @Override
    public int getQueueSize() {
        return (taskRunning.get()) ? tasks.size() + 1 : tasks.size();
    }

    @Override
    public long getWorkLeft() {
        AtomicLong workLeft = new AtomicLong(0);
        // se ia fiecare task si se aduna timpul ramas
        if (!tasks.isEmpty()) {
            for (Task task : tasks) {
                workLeft.addAndGet(task.getLeft());
            }
        }
        // daca avem un task care ruleaza -> ii luam timpul ramas
        AtomicLong runningThreadAtomic = (currentTask != null && currentTask.getLeft() > 0) ?
                new AtomicLong(currentTask.getLeft()) :
                new AtomicLong(0);
        workLeft.addAndGet(runningThreadAtomic.get());
        return workLeft.get();
    }

    @Override
    public void shutdown() {
        hostRunning.set(false);
    }

}


class TaskPriorityComparator implements Comparator<Task> {
    @Override
    public int compare(Task task1, Task task2) {
        if (task1.getPriority() > task2.getPriority()) {
            return -1;
        } else if (task1.getPriority() < task2.getPriority()) {
            return 1;
        } else if (task1.getStart() < task2.getStart()) {
            return -1;
        } else if (task1.getStart() > task2.getStart()) {
            return 1;
        }
        return 0;
    }
}
