package fi.aalto.amadei.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Static class that provides global instances
 */
public class Instances {

    // Creates a new GSON instance and register a type adapter for requests
    private static final Gson gson = new GsonBuilder().create();

    private Instances() {}

    public static Gson gson() {
        return gson;
    }
}
