import java.io.*;
import java.util.*;
import java.util.concurrent.*;

class Fork {
    final Semaphore semaphore = new Semaphore(1);
    private final int id;
    private int processTime;

    public Fork(int id, int processTime) {
        this.id = id;
        this.processTime = processTime;
    }

    public int getId() {
        return id;
    }
    
    public synchronized int getProcessTime() {
        return processTime;
    }

    public synchronized int setProcessTime(ProcessThread process) {
        this.processTime -= process.burstTime;
        if (processTime < 0) processTime = 0;
        return processTime;
    }

    public void pickUp() throws InterruptedException {
        semaphore.acquire();
    }

    public void putDown() {
        semaphore.release();
    }
}

class ProcessThread extends Thread {
    int id;
    int burstTime;
    ArrayList<Fork> forks;

    public ProcessThread(int id, int burstTime, ArrayList<Fork> forks) {
        this.id = id;
        this.burstTime = burstTime;
        this.forks = forks;
    }
    /*
    * Each thread tries to acquire two forks (semaphores) in a circular manner. 
    * If it successfully acquires both, it simulates eating by sleeping for the burst time, then releases the forks.
    * If it fails to acquire the second fork, it releases the first one and tries again with the next pair of forks.
    * This process continues until the thread has successfully eaten or all forks have been tried.
    */
    @Override
    public void run() {
        boolean ateLast = true;
        while (ateLast) {
            ateLast = false;
            for (int i = 0; i < forks.size(); i++) {
                Fork fork = forks.get(i);
                Fork nextFork = forks.get((i + 1) % forks.size());

                if (fork.getProcessTime() <= 0 && nextFork.getProcessTime() <= 0) continue;

                if (fork.semaphore.tryAcquire()) {
                    if (nextFork.semaphore.tryAcquire()) {
                        System.out.println("[Thread " + id + "] Picked up fork " + fork.getId() + " and " + nextFork.getId());
                        System.out.println("[Thread " + id + "] Eating...");
                        try {
                            Thread.sleep(1000L * burstTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        fork.setProcessTime(this);
                        nextFork.setProcessTime(this);
                        fork.putDown();
                        nextFork.putDown();
                        System.out.println("[Thread " + id + "] Released forks");
                        ateLast = true; // ate this round, keep going
                        break;
                    } else {
                        fork.putDown();
                    }
                }
            }
        }
        System.out.println("[Thread " + id + "] Finished.");
    }
}

public class App {
    public static void main(String[] args) throws Exception {
        ArrayList<ProcessThread> threads = new ArrayList<>();
        ArrayList<Fork> forks = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            forks.add(new Fork(i, (int)(Math.random() * 20 + 1)));
        }
        try( Scanner sc = new Scanner(new File("processes.txt")) ) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+");
                int id = Integer.parseInt(parts[0]);
                int burstTime = Integer.parseInt(parts[1]);
                threads.add(new ProcessThread(id, burstTime, forks));
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + e.getMessage());
        }
        

        for (ProcessThread thread : threads){
            thread.start();
        }
        for (ProcessThread thread : threads){
            thread.join();
        }

        for (Fork fork : forks) {
            System.out.println("Fork " + fork.getId() + " remaining process time: " + fork.getProcessTime());
        }
    }
}
