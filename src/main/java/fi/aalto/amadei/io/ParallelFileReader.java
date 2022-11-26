package fi.aalto.amadei.io;

import fi.aalto.amadei.computing.ComputeController;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Splits the file in -mostly- equal-sized chunks and assign them to a ChunkReader to be read and parsed.
 * Chunk sizes are not always equal since it's fundamental to not split lines into multiple chunks, otherwise parsing
 * cannot be performed. To achieve this, file is first split into equal-size chunk candidates. Each chunk is then
 * extended until the closest newline. Works especially well for big files (> 100MB)
 */
public class ParallelFileReader {

    private final int nThreads;
    private final File file;
    private final FileChannel channel;
    private final List<ChunkReader> chunkReaders;

    private final ExecutorService executor;

    private final ComputeController computeController;

    private void computeChunks() throws IOException {
        long lastAssignedByte = -1;
        long chunkStart;
        long chunkSize;

        long fileSize;
        long expectedChunkSize;

        char readChar;

        // Open the file as a Random Access File
        // This way we can read random bytes from it
        RandomAccessFile randomAccessFile;
        try {
            randomAccessFile = new RandomAccessFile(file, "r");
        } catch (Exception e) {
            throw new FileNotFoundException();
        }

        // Compute the file size and the expected chunk size
        fileSize = randomAccessFile.length();
        expectedChunkSize = (long) Math.ceil((double) fileSize / nThreads);

        // For every chunk (except the last one)
        for(int i = 0; i < nThreads - 1; i++) {
            // Compute initial chunk start and length
            chunkStart = lastAssignedByte + 1;
            chunkSize = expectedChunkSize - 1;

            // Search for first newline
            do {
                // Increase chunk size
                chunkSize++;

                // Go to last byte of chunk
                randomAccessFile.seek(chunkStart + chunkSize - 1);

                // Read its value
                readChar = (char) randomAccessFile.read();

                // Continue until last char it's a \n
            } while (readChar != '\n');

            // Compute the new last assigned byte
            lastAssignedByte = chunkStart + chunkSize - 1;

            // Add new Chunk to list
            chunkReaders.add(new ChunkReader(chunkStart, chunkSize, channel, computeController));
        }

        // Last chunk
        chunkReaders.add(new ChunkReader(
                lastAssignedByte + 1, fileSize - lastAssignedByte - 1,
                channel, computeController));
    }

    public ParallelFileReader(File file, int nThreads, ComputeController computeController) throws IOException {
        Objects.requireNonNull(file);

        this.file = file;
        this.nThreads = nThreads;
        this.channel = new FileInputStream(file).getChannel();
        this.chunkReaders = new ArrayList<>(nThreads);

        this.executor = Executors.newFixedThreadPool(nThreads);

        this.computeController = computeController;

        computeChunks();
    }

    /**
     * Signals every Chunk Reader to start reading, parsing requests and sending them to the Compute Controller.
     * Wait for all parsing workers to stop. If after 60 seconds parsing is not done, force them to shut down.
     */
    public void readAll() {
        // Start all threads
        for(ChunkReader i : chunkReaders) {
            executor.execute(i);
        }

        // Ask for shutdown
        executor.shutdown();

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
