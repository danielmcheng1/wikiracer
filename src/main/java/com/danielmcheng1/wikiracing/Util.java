package com.danielmcheng1.wikiracing;

import com.google.gson.JsonObject;

import java.util.logging.Level;

/**
 * Utility class for additional methods used by the crawler
 */
public class Util {
    // https://en.wikipedia.org/wiki/Wikipedia:Naming_conventions_(capitalization)
    // Wikipedia conventions uppercase the first letter for articles
    // This will standardize whatever titles the user inputs for source and destination
    // TBD strip /n and underscores too? https://www.mediawiki.org/wiki/API:Query#Title_normalization
    public static String normalizeTitle(String title) {
        if (title == null) {
            return null;
        }
        if (title.length() == 0) {
            return title;
        }
        return title.substring(0, 1).toUpperCase() + title.substring(1);
    }

    public static void assertValidInputTitle(String title) {
        JsonObject response = WikiRetriever.getWikiResponse(title, Direction.FORWARDS, null);
        if (WikiRetriever.getMissingInvalidPages(response).size() != 0) {
            throw new IllegalArgumentException("Input title of " + title + " does not exist on Wikipedia");
        }
    }

    public static String getThread() {
        return Thread.currentThread().getName() + "-->";
    }

    public static void setAllLogLevels(Level level) {
        CrawlerController.setLogLevel(level);
        Crawler.setLogLevel(level);
        Path.setLogLevel(level);
        WikiRetriever.setLogLevel(level);
    }
}
