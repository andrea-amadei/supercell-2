package fi.aalto.amadei.computing;

import fi.aalto.amadei.beans.UpdateRequest;
import fi.aalto.amadei.model.StateStorage;
import fi.aalto.amadei.utils.Instances;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The heart of application.
 * Assign requests to the correct worker according to the user performing them and compute the final result once
 * available.
 */
public class ComputeController {

    private final int nParsingThreads;
    private final int nComputingThreads;

    private final ExecutorService executor;
    private final List<ComputeWorker> workers;

    private int parsingThreadsDone;
    private int computingThreadsDone;

    private StateStorage finalStateStorage;

    public ComputeController(int nParsingThreads, int nComputingThreads) {
        this.nParsingThreads = nParsingThreads;
        this.nComputingThreads = nComputingThreads;

        this.executor = Executors.newFixedThreadPool(nComputingThreads);
        this.workers = new ArrayList<>(nComputingThreads);

        this.parsingThreadsDone = 0;

        this.finalStateStorage = new StateStorage();

        for(int i = 0; i < nComputingThreads; i++) {
            // Add worker to list
            this.workers.add(new ComputeWorker(this));

            // Start it
            executor.execute(this.workers.get(i));
        }

        executor.shutdown();
    }

    /**
     * Submit a new request to be assigned to a worker
     * @param request the request to execute
     */
    public synchronized void sendRequestToWorker(UpdateRequest request) {
        // Compute hash of user
        int hash = request.getUser().hashCode();

        // Assign always the same user to the same worker
        workers.get(hash % nComputingThreads).addToQueue(request);
    }

    /**
     * Signals that parsing is done. Once every parsing thread has signaled it, the information is propagated to every
     * computing thread to let them know to stop the computation when their queue is empty since no more requests will
     * come.
     */
    public synchronized void parsingDone() {
        // Increase number of parsing threads done by one
        parsingThreadsDone++;

        // If every parsing thread is done, notify all computing workers they should stop when the queue is empty
        if(parsingThreadsDone >= nParsingThreads) {
            for(int i = 0; i < nComputingThreads; i++)
                workers.get(i).parsingDone();
        }
    }

    /**
     * Sends a partial state storage to the controller in order to be merged with all others.
     * This shall be used by computing workers once they are done executing requests.
     * @param partialStateStorage the state storage of a worker
     */
    public synchronized void sendResult(StateStorage partialStateStorage) {
        computingThreadsDone++;

        // Merge the partial storage with the final one
        finalStateStorage.mergeWith(partialStateStorage);

        // If every computing thread is done prints the result
        if(computingThreadsDone >= nComputingThreads) {
            String finalResult = Instances.gson().toJson(finalStateStorage.getStateOfAllUsers());

            System.out.println(finalResult);
        }
    }

    /**
     * Wait for all computing workers to stop. If after 60 seconds computation is not done, force them to shut down.
     */
    public void waitForTermination() {
        // Wait for shutdown or force interrupt after 60 seconds
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS))
                executor.shutdownNow();

        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
