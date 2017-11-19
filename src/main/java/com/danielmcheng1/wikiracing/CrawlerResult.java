package com.danielmcheng1.wikiracing;

import java.util.ArrayList;
import java.util.Deque;

/**
 * This class provides a convenient data structure for storing the results of our crawl
 * This includes storing the path as a series of webNodes, and as a string array (for easy display in the REST API)
 * This also includes stats and other config information from this crawl execution
 **/
public class CrawlerResult {
    private final char foundPath;
    private final String info;
    private final ArrayList<String> path;
    private final transient Deque<WebNode> webNodePath;

    private final long runtimeMillis;
    private final String algorithm;
    private final int numThreads;
    private final int batchSize;

    public CrawlerResult(String info, Deque<WebNode> webNodePath, long runtimeMillis, String algorithm, int numThreads, int batchSize) {
        this.info = info;

        this.webNodePath = webNodePath;
        this.path = savePathAsArray(webNodePath);
        this.foundPath = webNodePath == null ? 'N' : 'Y';

        this.runtimeMillis = runtimeMillis;
        this.algorithm = algorithm;
        this.numThreads = numThreads;
        this.batchSize = batchSize;
    }

    public ArrayList<String> getPath() {
        return path;
    }

    public Deque<WebNode> getWebNodePath() {
        return webNodePath;
    }

    public String getInfo() {
        return info;
    }

    public void printResult() {
        System.out.println("--------------------------------------------");
        System.out.println(info);
        printPath();
        printStats();
        System.out.println();
    }

    private void printPath() {
        if (webNodePath == null) {
            return;
        }
        System.out.println();
        for (WebNode wN : webNodePath) {
            if (wN.getParent() == null) {
                continue;
            }
            String fromTitle, toTitle;
            if (Direction.FORWARDS.equals(wN.getDirection())) {
                fromTitle = wN.getParent().getTitle();
                toTitle = wN.getTitle();
            } else {
                fromTitle = wN.getTitle();
                toTitle = wN.getParent().getTitle();
            }
            System.out.println(fromTitle + " --> " + toTitle);
        }
    }

    private void printStats() {
        System.out.println();
        System.out.println("Algorithm: " + algorithm);
        System.out.println("Runtime (ms): " + runtimeMillis);
        System.out.println("# of Threads: " + numThreads);
        System.out.println("Batch Size: " + batchSize);
    }

    private ArrayList<String> savePathAsArray(Deque<WebNode> webNodePath) {
        if (webNodePath == null) {
            return null;
        }
        ArrayList<String> pathAsArray = new ArrayList<String>();
        for (WebNode wN : webNodePath) {
            if (Direction.FORWARDS.equals(wN.getDirection())) {
                pathAsArray.add(wN.getTitle());
            } else {
                WebNode parent = wN.getParent();
                if (parent != null) {
                    pathAsArray.add(parent.getTitle());
                }
            }
        }
        return pathAsArray;
    }

}
