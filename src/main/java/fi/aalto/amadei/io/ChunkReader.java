package fi.aalto.amadei.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.aalto.amadei.beans.UpdateRequest;
import fi.aalto.amadei.computing.ComputeController;

import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

/**
 * Reads and parse a chunk of the input file. Spawned by the ParallelFileReader
 */
public class ChunkReader implements Runnable {

    private final long startingBytePosition;
    private final long chunkLength;
    private final FileChannel fileChannel;

    private final Gson gson = new GsonBuilder().create();


    private final ComputeController computeController;

    public ChunkReader(long startingBytePosition, long chunkLength, FileChannel channel, ComputeController computeController) {
        this.startingBytePosition = startingBytePosition;
        this.chunkLength = chunkLength;
        this.fileChannel = channel;

        this.computeController = computeController;
    }

    public long getStartingBytePosition() {
        return startingBytePosition;
    }

    public long getChunkLength() {
        return chunkLength;
    }

    @Override
    public void run() {
        try {
            // Allocate memory
            ByteBuffer buffer = ByteBuffer.allocate(Math.toIntExact(chunkLength));

            // Read file chunk to RAM
            fileChannel.read(buffer, startingBytePosition);

            // Convert all bytes to a single String
            String stringChunk = new String(buffer.array(), StandardCharsets.UTF_8);

            // Read chunk line by line
            String line;
            try (BufferedReader reader = new BufferedReader(new StringReader(stringChunk))) {
                while ((line = reader.readLine()) != null) {
                    UpdateRequest request = gson.fromJson(line, UpdateRequest.class);

                    computeController.sendRequestToWorker(request);
                }
            }

            computeController.parsingDone();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
