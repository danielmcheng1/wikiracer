package com.danielmcheng1.wikiracing;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.HashMap;

public class CrawlTest extends TestCase {
    private Crawler crawler;
    private CrawlerState crawlerState;

    @Override
    protected void setUp() {
        this.crawler = new Crawler("Apple", "Chair", (long) 1);
        crawlerState = crawler.getCrawlerState();
    }

    @Test
    public void testGetNextBatchToVisit() {
        // initialized queue in setUp with one node in each
        // run through sequential tests to validate batches are pulled off properly

        // TEST SIZE = 1
        HashMap<String, WebNode> nextBatch = crawlerState.getNextBatchToVisit(Direction.FORWARDS);
        assertEquals("Testing crawler pulls off exactly one node to visit " + Direction.FORWARDS, 1, nextBatch.size());

        nextBatch = crawlerState.getNextBatchToVisit(Direction.BACKWARDS);
        assertEquals("Testing crawler pulls off exactly one BACKWARDS node to visit " + Direction.BACKWARDS, 1, nextBatch.size());

        // TEST SIZE = 0
        nextBatch = crawlerState.getNextBatchToVisit(Direction.FORWARDS);
        assertEquals("Testing crawler pulls off 0 nodes if queue is empty" + Direction.FORWARDS, 0, nextBatch.size());

        nextBatch = crawlerState.getNextBatchToVisit(Direction.BACKWARDS);
        assertEquals("Testing crawler pulls off 0 nodes if queue is empty" + Direction.BACKWARDS, 0, nextBatch.size());

        // TEST SIZE < maxBatchSize
        int maxBatchSize = crawler.getMaxBatchSize();
        addToVisitNTimes(maxBatchSize - 1);
        nextBatch = crawlerState.getNextBatchToVisit(Direction.FORWARDS);
        assertEquals("Testing crawler pulls off " + (maxBatchSize - 1) + " from queue", maxBatchSize - 1, nextBatch.size());

        // TEST SIZE == maxBatchSize
        addToVisitNTimes(maxBatchSize);
        nextBatch = crawlerState.getNextBatchToVisit(Direction.FORWARDS);
        assertEquals("Testing crawler pulls off maxBatchSize(" + (maxBatchSize) + ") from queue", maxBatchSize, nextBatch.size());

        // TEST SIZE > maxBatchSize
        addToVisitNTimes(maxBatchSize + 1);
        nextBatch = crawlerState.getNextBatchToVisit(Direction.FORWARDS);
        assertEquals("Testing crawler pulls off maxBatchSize(" + (maxBatchSize) + ") even if queue has more", maxBatchSize, nextBatch.size());
    }

    private void addToVisitNTimes(int N) {
        for (int i = 0; i < N; i++) {
            WebNode dummy = new WebNode("Dummy_" + i, null, Direction.FORWARDS, 0);
            crawlerState.addToVisit(dummy);
        }
    }


    @Test
    public void testWithinTimeoutBoundsReturnsFalseIfTimedOut() {
        Long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 1001) {
            // wait until timeout of 1 seconds = 1000 milliseconds has passed
        }
        assertFalse("Testing that crawler times out if timeout has expired", crawler.withinTimeoutBounds(start));
    }

    @Test
    public void testWithinTimeoutBoundsReturnsTrueIfNotTimedOut() {
        Long start = System.currentTimeMillis();
        assertTrue("Testing that crawler does not time out if timeout has not expired", crawler.withinTimeoutBounds(start));
    }

    @Test
    public void testNormalizeTitle() {
        String normalized = Util.normalizeTitle("apple COMPUTER");
        assertEquals("Testing that normalize capitalizes the first letter without changing the rest", "Apple COMPUTER", normalized);
    }
}