package com.danielmcheng1.wikiracing;

import com.google.gson.JsonObject;
import junit.framework.TestCase;
import okhttp3.HttpUrl;
import org.junit.Test;

import java.util.logging.Level;

public class WikiRetrieverTest extends TestCase {
    @Override
    protected void setUp() {
        WikiRetriever.setLogLevel(Level.WARNING);
    }

    @Test
    public void testGetWikiResponseDoesNotReturnInvalid() {
        JsonObject response = WikiRetriever.getWikiResponse("Apple", Direction.FORWARDS, null);
        JsonObject invalid = WikiRetriever.getMissingInvalidPages(response);
        assertEquals("Testing that Wiki API returns no invalid/missing pages for Apple", 0, invalid.size());
    }

    @Test
    public void testGetWikiResponseReturnsInvalidIfInvalid() {
        JsonObject response = WikiRetriever.getWikiResponse("AppleNONEXISTENT", Direction.FORWARDS, null);
        JsonObject invalid = WikiRetriever.getMissingInvalidPages(response);
        assertEquals("Testing that Wiki API returns invalid/missing pages for AppleNONEXISTENT", 1, invalid.size());
    }

    @Test
    public void testGetWikiResponseReturnsSuccessfully() {
        JsonObject response = WikiRetriever.getWikiResponse("Apple", Direction.FORWARDS, null);
        assertNotNull("Testing that Wiki API returns result for Apple", response);
    }

    @Test
    public void testBuildWikiURLReturnsValidURL() {
        HttpUrl httpUrl = WikiRetriever.buildWikiURL("Apple", Direction.FORWARDS, null);
        String expectedUrl = "https://en.wikipedia.org/w/api.php?action=query&titles=Apple&prop=links&pllimit=max&plnamespace=" + WikiRetriever.getWikiNamespaces() + "&format=json";
        assertEquals("Testing that buildWikiURL builds URL correctly", expectedUrl, httpUrl.toString());
    }

    @Test
    public void testGetWikiResponseLatency() {
        Long start = System.currentTimeMillis();
        JsonObject response = WikiRetriever.getWikiResponse("Apple", Direction.FORWARDS, null);
        Long end = System.currentTimeMillis();
        Long latency = end - start;
        Long expectedLatency = (long) 5000; // latency is usually < 200 ms on local machine but setting it to 5s so test isn't flaky
        assertTrue("Latency " + latency + "(ms) is longer than expected. Wikiracer may run more slowly", latency < expectedLatency);
    }


}
