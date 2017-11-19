package com.danielmcheng1.wikiracing;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class CrawlEndToEndTest extends TestCase {
    private Crawler crawl;

    @Override
    protected void setUp() {
    }

    @Test
    public void testZeroSteps() {
        CrawlerController crawlerController = new CrawlerController("Apple", "Apple");
        crawlerController.runCrawl();
        assertEquals(new ArrayList<String>(Arrays.asList("Apple")), crawlerController.getResult().getPath());
    }

    @Test
    public void testOneStep() {
        CrawlerController crawlerController = new CrawlerController("Apple", "Ancestor");
        crawlerController.runCrawl();
        assertEquals(new ArrayList<String>(Arrays.asList("Apple", "Ancestor")), crawlerController.getResult().getPath());
    }

    @Test
    public void testMultipleSteps() {
        CrawlerController crawlerController = new CrawlerController("Apple", "Chair");
        crawlerController.runCrawl();

        // the FASTEST path is nondeterministic so validate we have more than 2 steps
        assertTrue("Testing that crawler finds path with multiple steps", crawlerController.getResult().getPath().size() > 2);

    }

    @Test
    public void testBidirectional() {
        CrawlerController crawlerController = new CrawlerController("Apple", "Chair");
        crawlerController.runCrawl();

        HashSet<Direction> directions = new HashSet<Direction>();
        for (WebNode wN : crawlerController.getResult().getWebNodePath()) {
            directions.add(wN.getDirection());
        }
        assertEquals("Testing that crawler searches in both directions", 2, directions.size());
    }

    @Test
    public void testTimeout() {
        CrawlerController crawlerController = new CrawlerController("Apple", "Chair", (long) 0);
        crawlerController.runCrawl();
        String info = crawlerController.getResult().getInfo();
        assertEquals("Failed to complete within timeout period of 0s", info);
    }
}