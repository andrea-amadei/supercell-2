package fi.aalto.amadei;

import fi.aalto.amadei.computing.ComputeController;
import fi.aalto.amadei.io.ParallelFileReader;
import fi.aalto.amadei.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

@Command(name = "run", version = "1.0", description = "Run the algorithm on an input file", mixinStandardHelpOptions = true)
public class App implements Callable<Integer> {

    @Option(names = {"-i", "--input"}, description = "The input file to process", required = true)
    private File file;

    @Option(names = {"-p", "--parsing"}, description = "The number of threads dedicated to parsing the input file")
    private int nParsingThreads = Constants.DEFAULT_PARSING_THREADS;

    @Option(names = {"-c", "--computing"}, description = "The number of threads dedicated to computing the result")
    private int nComputingThreads = Constants.DEFAULT_COMPUTING_THREADS;

    @Override
    public Integer call() {
        // Check if the file exists (and it's not a directory)
        if(!file.exists() || file.isDirectory()) {
            System.out.println("Invalid file.");
            return 1;
        }

        // Check if the file can be opened
        try {
            ComputeController computeController = new ComputeController(nParsingThreads, nComputingThreads);

            ParallelFileReader parallelFileReader = new ParallelFileReader(file, nParsingThreads, computeController);

            parallelFileReader.readAll();
            computeController.waitForTermination();
        } catch (IOException e) {
            System.out.println("Unable to open file.");
            return 1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return 0;
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }
}
