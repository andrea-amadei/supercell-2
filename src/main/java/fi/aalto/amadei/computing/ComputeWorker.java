package fi.aalto.amadei.computing;

import fi.aalto.amadei.beans.DoneRequest;
import fi.aalto.amadei.beans.Request;
import fi.aalto.amadei.model.StateStorage;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Processes a single request at a time. Has a queue to store all pending requests. Spawned by the ComputeController.
 */
public class ComputeWorker implements Runnable {

    private final BlockingQueue<Request> requestQueue;
    private final StateStorage stateStorage;

    private final ComputeController computeController;

    public ComputeWorker(ComputeController computeController) {
        this.requestQueue = new ArrayBlockingQueue<>(1_000_000);
        this.stateStorage = new StateStorage();

        this.computeController = computeController;
    }

    /**
     * Adds a new request to the queue, to be eventually processed.
     * @param request the request to add to the queue
     */
    public synchronized void addToQueue(Request request) {
        // Add new request to the worker's queue
        requestQueue.add(request);
    }

    /**
     * Notifies the worker that all parsing is done, therefore they should finish working and then terminate.
     */
    public synchronized void parsingDone() {
        this.requestQueue.add(new DoneRequest(computeController));
    }

    @Override
    public void run() {
        try {

            while(true) {
                Request request = requestQueue.take();

                if(request.execute(stateStorage))
                    break;
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
