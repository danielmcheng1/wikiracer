package com.danielmcheng1.wikiracing;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;

import java.util.Deque;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.danielmcheng1.wikiracing.Util.getThread;

/**
 * This is the heart of the program. It takes a source and destination and starts multithreaded crawls from each.
 * As part of the crawl, it fetches links for each page and maintains state about the pages it has already visit and
 * the ones it is going to visit next
 */
public class Crawler {
    public static final int maxBatchSize = 50;

    private static final Logger LOGGER = Logger.getLogger(Crawler.class.getName());
    private static final long defaultTimeoutMillis = (long) (15 * 1000);

    private final String sourceTitle;
    private final String destTitle;
    private long timeoutMillis;

    private Path path;
    private CrawlerState crawlerState;

    public Crawler(String sourceTitle, String destTitle, Long timeoutSeconds) {
        this.sourceTitle = Util.normalizeTitle(sourceTitle);
        this.destTitle = Util.normalizeTitle(destTitle);
        if (timeoutSeconds == null) this.timeoutMillis = defaultTimeoutMillis;
        else this.timeoutMillis = timeoutSeconds * 1000;

        this.crawlerState = new CrawlerState();
        this.path = new Path(crawlerState.getVisitedForwards(), crawlerState.getVisitedBackwards());
        initializeQueues();

        Util.setAllLogLevels(Level.WARNING);
    }

    public Crawler(String sourceTitle, String destTitle) {
        this(sourceTitle, destTitle, defaultTimeoutMillis);
    }

    // call the Wiki API to get all links corresponding to the input list of titles
    public void findNextLinks(HashMap<String, WebNode> titlesToWebNodes, Direction direction) {
        if (titlesToWebNodes.isEmpty()) {
            return;
        }

        String titlesConcatenated = StringUtils.join(titlesToWebNodes.keySet().toArray(), "|");
        LOGGER.log(Level.INFO, getThread() + "API call for: " + titlesConcatenated);

        Boolean isFirstCall = true;
        JsonObject continueParams = null;
        // continue to query Wiki API for this set of titles, as long as Wikipedia keeps passing us back a continue parameter
        while (isFirstCall || continueParams != null) {
            isFirstCall = false;
            JsonObject response = WikiRetriever.getWikiResponse(titlesConcatenated, direction, continueParams);
            // add resulting links for each page/title in our API call
            if (response != null) {
                addAllPagesToVisit(response, titlesToWebNodes, direction);
                // return as soon as possible since another thread may have found the path
                if (path.foundPath()) {
                    return;
                }
                // check if we need to send a continue request to retrieve the next set of links for this same query
                continueParams = WikiRetriever.getContinue(response);
            }
            // backoff if WikiAPI timed out request
            else {
                // wait before trying to call again
                try {
                    Thread.currentThread().sleep(500);
                } catch (InterruptedException e) {
                    LOGGER.log(Level.INFO, getThread() + "findNextLinks interrupted while sleeping");
                }
            }
        }

        // all processing complete for these nodes
        for (WebNode wN : titlesToWebNodes.values()) {
            crawlerState.removeNodeFromIsProcessing(wN);
        }
    }

    // save the links for all pages from the Wiki API response
    private void addAllPagesToVisit(JsonObject response, HashMap<String, WebNode> titlesToWebNodes, Direction direction) {
        // TBD throw error here
        if (response == null) {
            return;
        }
        // API response maps each page to a set of links
        JsonObject pageResults = response.getAsJsonObject("query").getAsJsonObject("pages");
        String linksKey;
        if (Direction.FORWARDS.equals(direction)) {
            linksKey = "links";
        } else {
            linksKey = "linkshere";
        }
        for (String page : pageResults.keySet()) {
            JsonObject resultsForPage = pageResults.getAsJsonObject(page);
            JsonArray linksForPage = resultsForPage.getAsJsonArray(linksKey);

            // either there are no links on this page, or this batch did not return anything for this page
            if (linksForPage == null) {
                continue;
            }
            String title = resultsForPage.get("title").getAsString();
            WebNode parentNode = titlesToWebNodes.get(title);
            LOGGER.log(Level.INFO, getThread() + "Saving title links on page: " + title);

            // should never happen but we cannot add this node then since the parent is unknown
            if (parentNode == null) {
                LOGGER.log(Level.WARNING, getThread() + "Found null parent node when retrieving: " + title);
                continue;
            }

            // add all discovered links for this page
            addOnePageToVisit(linksForPage, parentNode);

            // return as soon as possible since another thread may have found the path
            if (path.foundPath()) {
                return;
            }
        }
    }

    // save the links for one page within the Wiki API response
    private void addOnePageToVisit(JsonArray linksForPage, WebNode parent) {
        for (int i = 0; i < linksForPage.size(); i++) {
            String title = linksForPage.get(i).getAsJsonObject().get("title").getAsString();
            WebNode titleNode = new WebNode(title, parent, parent.getDirection(), parent.getDistance() + 1);

            if (!crawlerState.visited(titleNode)) {
                crawlerState.addToVisit(titleNode); // add this as a new title to visit
                crawlerState.markAsVisited(titleNode); // eagerly mark it as visited (prevents duplicates from queueing)
                path.markIfNodeCompletesPath(titleNode); // check if this new link connects our backwards and forwards search
            }

            // return as soon as possible since another thread may have found the path
            if (path.foundPath()) {
                return;
            }
        }
    }

    protected void initializeQueues() {
        WebNode sourceWebNode = new WebNode(sourceTitle, null, Direction.FORWARDS, 0);
        WebNode destWebNode = new WebNode(destTitle, null, Direction.BACKWARDS, 0);
        crawlerState.addToVisit(sourceWebNode);
        crawlerState.addToVisit(destWebNode);

        // eager implementation: preemptively mark to prevent duplicates from queueing up
        crawlerState.markAsVisited(sourceWebNode);
        crawlerState.markAsVisited(destWebNode);

        // check if source = dest
        path.markIfNodeCompletesPath(sourceWebNode);
    }

    protected void runCrawlMultithreaded(Direction direction, long startTime, int threads) {
        ExecutorService taskExecutor = Executors.newFixedThreadPool(threads);
        while (!path.foundPath() && withinTimeoutBounds(startTime)) {
            while (crawlerState.noNodesInQueueToVisit(direction) && crawlerState.someNodesAreProcessing(direction)) {
                try {
                    Thread.currentThread().sleep(100);
                } catch (InterruptedException e) {
                    // continue on as this is merely to prevent repeated calls from building up when queue is starting out
                    LOGGER.log(Level.INFO, getThread() + "runCrawlMultithreaded interrupted from sleep: " + e.getMessage());
                }
            }
            Runnable task = new Runnable() {
                public void run() {
                    if (!path.foundPath()) {
                        findNextLinks(crawlerState.getNextBatchToVisit(direction), direction);
                    }
                }
            };
            taskExecutor.execute(task);
        }

        taskExecutor.shutdown(); // prevent more tasks
        taskExecutor.shutdownNow(); // cancel current threads
    }


    protected boolean withinTimeoutBounds(Long startTime) {
        return System.currentTimeMillis() - startTime < timeoutMillis;
    }

    protected int getMaxBatchSize() {
        return maxBatchSize;
    }

    public static void setLogLevel(Level level) {
        LOGGER.setLevel(level);
    }

    public String getSourceTitle() {
        return sourceTitle;
    }

    public String getDestTitle() {
        return destTitle;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public Deque<WebNode> getPath() {
        return path.getPath();
    }

    public CrawlerState getCrawlerState() {
        return crawlerState;
    }
}

