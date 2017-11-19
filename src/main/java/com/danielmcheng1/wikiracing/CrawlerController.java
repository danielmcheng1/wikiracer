package com.danielmcheng1.wikiracing;

import com.google.gson.Gson;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.danielmcheng1.wikiracing.Util.getThread;

/**
 * This class orchestrates a single crawl. It does some validation checks and then calls the Crawler to start a forward
 * crawl from the source and a backward crawl from the destination. By starting two BFS searches from both directions
 * we can find the intersection ponit quicker.
 * <p>
 * There is also a CLI interface to trigger this controller in the main below.
 */
public class CrawlerController {
    private static final Logger LOGGER = Logger.getLogger(CrawlerController.class.getName());
    private static final int numThreads = 16;
    private Crawler crawler;
    private CrawlerResult result;

    public CrawlerController(String sourceTile, String destTile) {
        this.crawler = new Crawler(sourceTile, destTile);
    }

    public CrawlerController(String sourceTitle, String destTitle, Long timeoutSeconds) {
        this.crawler = new Crawler(sourceTitle, destTitle, timeoutSeconds);
    }

    public void runCrawl() {
        // Validate inputs
        System.out.println("Validating input source and destination titles exist on Wikipedia...");
        LOGGER.log(Level.INFO, "Validating input source and destination titles exist on Wikipedia...");
        try {
            Util.assertValidInputTitle(crawler.getSourceTitle());
            Util.assertValidInputTitle(crawler.getDestTitle());
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            result = new CrawlerResult(e.getMessage(), crawler.getPath(), 0, "Bidirectional BFS", numThreads, Crawler.maxBatchSize);
            LOGGER.log(Level.INFO, "Invalid input: {0}", e.getMessage());
            return;
        }

        // Kick off forwards and backwards crawl on their own set of threads
        System.out.println("Running crawl between " + crawler.getSourceTitle() + " and " + crawler.getDestTitle() + "...");
        LOGGER.log(Level.INFO, "Running crawl between " + crawler.getSourceTitle() + " and " + crawler.getDestTitle());

        long startTime = System.currentTimeMillis();
        ExecutorService taskExecutor = Executors.newFixedThreadPool(2);

        // forwards crawl
        Runnable taskForwards = new Runnable() {
            public void run() {
                LOGGER.log(Level.INFO, getThread() + "Starting taskForwards");
                crawler.runCrawlMultithreaded(Direction.FORWARDS, startTime, numThreads / 2);
            }
        };
        taskExecutor.execute(taskForwards);

        // backwards crawl
        Runnable taskBackwards = new Runnable() {
            public void run() {
                LOGGER.log(Level.INFO, getThread() + "Starting taskBackwards");
                crawler.runCrawlMultithreaded(Direction.BACKWARDS, startTime, numThreads / 2);
            }
        };
        taskExecutor.execute(taskBackwards);

        // wait for both tasks to finish
        taskExecutor.shutdown();
        try {
            taskExecutor.awaitTermination(crawler.getTimeoutMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOGGER.log(Level.INFO, getThread() + "runCrawl thread interrupted from sleep: " + e.getMessage());
        }

        // save the results
        String infoMessage;
        if (crawler.getPath() != null) {
            infoMessage = "Found path!";
        } else {
            infoMessage = "Failed to complete within timeout period of " + crawler.getTimeoutMillis() / 1000 + "s"; // input is a long so no need to use double division
        }
        result = new CrawlerResult(infoMessage, crawler.getPath(), System.currentTimeMillis() - startTime, "Bidirectional BFS", numThreads, Crawler.maxBatchSize);

    }

    public CrawlerResult getResult() {
        return result;
    }

    public String getResultSerialized() {
        return new Gson().toJson(result);
    }

    public void visualize() {
        CrawlerState crawlerState = crawler.getCrawlerState();
        Visualizer.visualize(getResult().getPath(), crawlerState.getVisitedForwards(), crawlerState.getVisitedBackwards());
    }

    public static void setLogLevel(Level level) {
        LOGGER.setLevel(level);
    }

    public static void main(String[] args) {
        CommandLineValues values = new CommandLineValues();
        CmdLineParser parser = new CmdLineParser(values);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            System.exit(1);
        }
        String sourceTitle = values.getSourceTitle();
        String destTitle = values.getDestTitle();
        Long timeoutSecs = values.getTimeout();
        String startREST = values.getStartREST();

        if ("Y".equals(startREST)) {
            CrawlerService.main(new String[]{});
        } else {
			if (sourceTitle == null || destTitle == null) {
				System.out.println("ERROR: Both sourceTitle and destTitle must be specified");
				System.exit(1);
			}
            CrawlerController crawlerController = new CrawlerController(sourceTitle, destTitle, timeoutSecs);
            crawlerController.runCrawl();
            crawlerController.getResult().printResult();
            crawlerController.visualize();
        }

    }
}
