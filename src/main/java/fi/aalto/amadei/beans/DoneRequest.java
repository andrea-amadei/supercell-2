package fi.aalto.amadei.beans;

import fi.aalto.amadei.computing.ComputeController;
import fi.aalto.amadei.model.StateStorage;

/**
 * Signals computation is done and therefore should terminate when possible
 */
public class DoneRequest implements Request {

    private final ComputeController computeController;

    public DoneRequest(ComputeController computeController) {
        this.computeController = computeController;
    }

    @Override
    public boolean execute(StateStorage stateStorage) {
        computeController.sendResult(stateStorage);

        return true;
    }
}
