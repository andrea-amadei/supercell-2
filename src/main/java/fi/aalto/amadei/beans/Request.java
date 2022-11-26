package fi.aalto.amadei.beans;

import fi.aalto.amadei.model.StateStorage;

/**
 * Generic request
 */
public interface Request {

    /**
     * Executes the request. To be implemented by requests!
     * @param stateStorage the State Storage instance (where every user's state is stored)
     * @return true if execution should be terminated after, false otherwise
     */
    boolean execute(StateStorage stateStorage);
}
