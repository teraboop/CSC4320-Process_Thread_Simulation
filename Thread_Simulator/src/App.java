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
    
    public int getProcessTime() {
        return processTime;
    }

    public int setProcessTime(ProcessThread process) {
        this.processTime -= process.burstTime;
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

    @Override
    public void run() {
        for (int i = 0; i < forks.size(); i++) {
            Fork fork = forks.get(i);
            Fork nextFork = forks.get((i + 1) % forks.size());

            if (fork.getProcessTime() <= 0) continue; // skip exhausted forks

            System.out.println("[Thread " + id + "] Waiting for forks " + fork.getId() + " and " + nextFork.getId());

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
                    break;
                } else {
                    fork.putDown(); // couldn't get both, release first
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
    }
}
