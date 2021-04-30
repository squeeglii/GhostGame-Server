package net.cg360.spookums.server.core.scheduler;

import net.cg360.spookums.server.Server;

import java.util.ArrayList;
import java.util.UUID;

public class Scheduler {

    private static Scheduler primaryInstance;

    private UUID schedulerID;

    protected long syncedTick;    // The server's tick whilst this has been hooked.
    protected long schedulerTick; // The scheduler's actual tick. This depends on the tickrate

    protected int tickDelay; // The amount of server ticks between each scheduler tick.

    protected final ArrayList<Thread> activeThreads;

    protected ArrayList<SchedulerTaskEntry> schedulerTasks;
    protected boolean isRunning;


    public Scheduler(int tickDelay) {
        this.schedulerID = UUID.randomUUID();

        this.syncedTick = 0;
        this.schedulerTick = 0;
        this.tickDelay = Math.max(1, tickDelay);

        this.schedulerTasks = new ArrayList<>();
        this.activeThreads = new ArrayList<>();
        this.isRunning = false;
    }

    /**
     * Sets the scheduler the result provided from Scheduler#get() and
     * finalizes the instance to an extent.
     *
     * Cannot be changed once initially called.
     */
    public boolean setAsPrimaryInstance() {
        if(primaryInstance == null) {
            primaryInstance = this;
            return true;
        }
        return false;
    }



    // -- Control --

    /** Enables ticking on the scheduler*/
    public synchronized boolean startScheduler() {
        if(!isRunning) {
            this.isRunning = true;
            return true;
        }
        return false;
    }

    /**
     * Removes scheduler's hook to the server tick whilst clearing the queue.
     * @return true if the scheduler was initially running and then stopped.
     */
    public synchronized boolean stopScheduler() {
        if(isRunning) {
            pauseScheduler();
            clearQueuedSchedulerTasks();
            return true;
        }
        return false;
    }

    /** Removes scheduler's hook to the server tick whilst clearing the queue */
    public synchronized void pauseScheduler() {
        this.isRunning = false;
    }

    /** Cleares all the tasks queued in the scheduler. */
    public synchronized void clearQueuedSchedulerTasks() {
       for(SchedulerTaskEntry entry: new ArrayList<>(schedulerTasks)) {
           entry.getTask().cancel(); // For the runnable to use? idk
           this.schedulerTasks.remove(entry);
       }
    }


    // -- Ticking --
    // Methods used to tick a scheduler should only be triggered by the
    // Scheduler thread.

    /**
     * Ran to indicate a server tick has occurred, potentially triggering a server tick.
     * @return true is a scheduler tick is triggered as a result.
     */
    public synchronized boolean serverTick() { // Should only be done on the main thread
        syncedTick++;

        // Check if synced is a multiple of the delay
        if((syncedTick % tickDelay) == 0) {
            schedulerTick();
            return true;
        }
        return false;
    }

    /** Executes a scheduler tick, running any tasks due to run on this tick. */
    public synchronized void schedulerTick() {
        if(isRunning) {

            // To avoid stopping the scheduler from inside a task making it scream, use ArrayList wrapping
            for(SchedulerTaskEntry task: new ArrayList<>(schedulerTasks)) {
                long taskTick = task.getNextTick();

                if(taskTick == schedulerTick) {

                    // Cancelled tasks shouldn't be in the scheduler queue anyway.
                    if(!task.getTask().isCancelled()) {

                        if(task.isAsynchronous()) {
                            new Thread() {

                                @Override
                                public void run() {
                                    synchronized (activeThreads) { activeThreads.add(this); }

                                    try {
                                        task.getTask().run();
                                    } catch (Exception err) {
                                        Server.getMainLogger().error("Error thrown in asynchronous task! :(");
                                        err.printStackTrace();
                                    }

                                    synchronized (activeThreads) { activeThreads.remove(this); }
                                }

                                @Override
                                public void interrupt() {
                                    synchronized (activeThreads) {
                                        activeThreads.remove(this);
                                    }
                                    super.interrupt();
                                }

                            }.start(); // Start async thread and move on.

                        } else {
                            // Run as sync. This task must complete before the next one
                            // is ran.
                            task.getTask().run();
                        }


                        // Not cancelled by the call of #run() + it's a repeat task.
                        if(task.isRepeating() && (!task.getTask().isCancelled())) {
                            long targetTick = taskTick + task.getRepeatInterval();

                            SchedulerTaskEntry newTask = new SchedulerTaskEntry(task.getTask(), task.getRepeatInterval(), targetTick, task.isAsynchronous());
                            queueNSTaskEntry(newTask);
                        }
                    }

                } else if(taskTick > schedulerTick) {
                    // Upcoming task, do not remove from queue! :)
                    break;
                }

                schedulerTasks.remove(0); // Operate like a queue.
                // Remove from the start as long as it isn't an upcoming task.
                // If a task is somehow scheduled *before* the current tick, it should
                // be removed anyway.
            }
            schedulerTick++; // Tick after so tasks can be ran without a delay.
        }
    }


    // -- Task Control --

    protected void queueNSTaskEntry(SchedulerTaskEntry entry){
        if(entry.getNextTick() >= schedulerTick) throw new IllegalStateException("Task cannot be scheduled before the current tick.");

        int size = schedulerTasks.size();
        for(int i = 0; i < size; i++) {
            // Entry belongs before task? Insert into it's position
            if(schedulerTasks.get(i).getNextTick() < entry.getNextTick()) {
                this.schedulerTasks.add(i, entry);
                return;
            }
        }

        // Not added in loop. Append to the end.
        this.schedulerTasks.add(entry);
    }

    // -- Task Registering --



    // -- Getters --

    /** The unique ID of this scheduler. */
    public UUID getSchedulerID() { return schedulerID; }

    /** @return the amount of server ticks this scheduler has been running for. */
    public long getSyncedTick() { return syncedTick; }
    /** @return the amount of ticks this scheduler has executed. */
    public long getSchedulerTick() { return schedulerTick; }
    /** @return the amount of server ticks between each scheduler tick. */
    public int getTickDelay() { return tickDelay; }
    /** @return a list of active async task threads */
    public ArrayList<Thread> getActiveThreads() { return new ArrayList<>(activeThreads); }

    /** @return the primary instance of the scheduler. */
    public static Scheduler get() { return primaryInstance; }

    // -- Setters --

    /** Sets the amount of server ticks that should pass between each scheduler tick. */
    public void setTickDelay(int tickDelay) {
        this.tickDelay = tickDelay;
    }
}