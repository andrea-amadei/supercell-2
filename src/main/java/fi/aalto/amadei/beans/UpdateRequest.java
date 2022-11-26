package fi.aalto.amadei.beans;

import fi.aalto.amadei.model.StateStorage;

import java.util.Map;

/**
 * Update a user's state
 */
public class UpdateRequest implements Request {

    private String user;
    private long timestamp;
    private Map<String, String> values;

    public String getUser() {
        return user;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getValues() {
        return values;
    }

    @Override
    public boolean execute(StateStorage stateStorage) {
        stateStorage.updateState(user, timestamp, values);

        return false;
    }
}
