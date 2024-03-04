/* Implement this class. */

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class MyDispatcher extends Dispatcher {

    public static Semaphore semaphore;
    int i; // folosit pentru Round-Robin

    public MyDispatcher(SchedulingAlgorithm algorithm, List<Host> hosts) {
        super(algorithm, hosts);
        this.i = hosts.size() - 1;
        semaphore = new Semaphore(1);
    }

    @Override
    public void addTask(Task task) {
        // semaphore pentru a nu se suprapuna task-uri in acelasi timp
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        switch (algorithm) {
            case ROUND_ROBIN:
                roundRobin(task);
                break;
            case SHORTEST_QUEUE:
                shortestQueue(task);
                break;
            case SIZE_INTERVAL_TASK_ASSIGNMENT:
                sizeIntervalTaskAssigment(task);
                break;
            case LEAST_WORK_LEFT:
                leastWorkLeft(task);
                break;
        }
    }

    private void roundRobin(Task task) {
        i = (i + 1) % hosts.size();
        hosts.get(i).addTask(task);
    }

    private void shortestQueue(Task task) {
        AtomicIntegerArray queueSize = new AtomicIntegerArray(hosts.size());
        // se retine dimensiunea cozii de task-uri pentru fiecare host
        for (int i = 0; i < hosts.size(); i++) {
            queueSize.set(i, hosts.get(i).getQueueSize());
        }
        int min = queueSize.get(0);
        int index = 0;
        // se cauta minimum -> cel in care va fi adaugat task-ul
        for (int i = 1; i < hosts.size(); i++) {
            if (queueSize.get(i) < min) {
                min = queueSize.get(i);
                index = i;
            }
        }
        hosts.get(index).addTask(task);
    }

    public void sizeIntervalTaskAssigment(Task task) {
        int currentNode = 0;
        // in functie de tipul task-ului, il atribuim nodului destinat
        switch (task.getType()) {
            case SHORT:
                currentNode = 0;
                break;
            case MEDIUM:
                currentNode = 1;
                break;
            case LONG:
                currentNode = 2;
                break;
        }
        this.hosts.get(currentNode).addTask(task);
    }

    private void leastWorkLeft(Task task) {
        int currentNode = 0;
        long min = Long.MAX_VALUE;
        long workLeft = 0;
        long difference = 0;
        boolean almostEqual = false;
        MyHost.semRunning.acquireUninterruptibly();

        for (int i = 0; i < hosts.size(); i++) {
            workLeft = hosts.get(i).getWorkLeft();
            difference = workLeft - min;
            // almostEqual face o aproximare a timpurilor de executie
            almostEqual = (Math.abs(difference) < 30);
            if (difference < 0 && !almostEqual) {
                min = workLeft;
                currentNode = i;
            } else if (almostEqual) {
                if (i < currentNode) {
                    min = workLeft;
                    currentNode = i;
                }
            }
        }
        MyHost.semRunning.release();

        this.hosts.get(currentNode).addTask(task);
    }
}




