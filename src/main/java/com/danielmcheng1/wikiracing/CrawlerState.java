package com.danielmcheng1.wikiracing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.danielmcheng1.wikiracing.Crawler.maxBatchSize;

/**
 * This class is used to interact with all the data the crawler needs as it goes about finding a page.
 * In the current implementation these are all maintained as in-memory Maps and Queues. The current crawler can be scaled
 * to more than one machine to handle more than one request. But to make it lightning fast we would want each request
 * to be scaled to more than one machine. In that case all we would have to change are the state access methods in this
 * class to get webpages from a Redis instance rather than the in-memory map.
 */
public class CrawlerState {
    private HashMap<String, WebNode> visitedForwards;
    private HashMap<String, WebNode> visitedBackwards;
    private ConcurrentLinkedQueue<WebNode> toVisitForwards;
    private ConcurrentLinkedQueue<WebNode> toVisitBackwards;
    private HashSet<String> isProcessingForwards;
    private HashSet<String> isProcessingBackwards;

    public CrawlerState() {
        visitedForwards = new HashMap<String, WebNode>();
        visitedBackwards = new HashMap<String, WebNode>();
        toVisitForwards = new ConcurrentLinkedQueue<WebNode>();
        toVisitBackwards = new ConcurrentLinkedQueue<WebNode>();
        isProcessingForwards = new HashSet<String>();
        isProcessingBackwards = new HashSet<String>();
    }

    /******************************************/
    // METHODS FOR INTERACTING WITH THE QUEUE OF NODES TO VISIT
    /******************************************/
    protected HashMap<String, WebNode> getNextBatchToVisit(Direction direction) {
        HashMap<String, WebNode> titlesToWebNodes = new HashMap<String, WebNode>();
        int numAdded = 0;
        while (numAdded < maxBatchSize) {
            // .size() is O(n) for concurrent queue so more efficient to try to remove until we cannot
            WebNode curr = removeNodeFromQueueToVisit(direction);
            // nothing left in the queue;
            if (curr == null) {
                break;
            }
            addNodeToIsProcessing(curr);
            titlesToWebNodes.put(curr.getTitle(), curr);
            numAdded++;
        }
        return titlesToWebNodes;
    }

    protected void addToVisit(WebNode wN) {
        if (Direction.FORWARDS.equals(wN.getDirection())) {
            toVisitForwards.add(wN);
        } else {
            toVisitBackwards.add(wN);
        }
    }

    // safe remove of node from queue
    protected WebNode removeNodeFromQueueToVisit(Direction direction) {
        // use try-catch to reduce any time lag between checking if queue is empty and actually removing it (as another thread/machine may remove)
        try {
            if (Direction.FORWARDS.equals(direction)) {
                return toVisitForwards.remove();
            } else {
                return toVisitBackwards.remove();
            }
        } catch (Exception NoSuchElementException) {
            return null;
        }
    }

    protected boolean noNodesInQueueToVisit(Direction direction) {
        if (Direction.FORWARDS.equals(direction)) {
            return toVisitForwards.isEmpty();
        }
        return toVisitBackwards.isEmpty();
    }


    /******************************************/
    // METHODS FOR DETERMINING NODES THAT ARE BEING PROCESSED
    // (i.e. removed from toVisit queue, but still going through the Wiki API call)
    /******************************************/
    protected boolean someNodesAreProcessing(Direction direction) {
        if (Direction.FORWARDS.equals(direction)) {
            return !isProcessingForwards.isEmpty();
        }
        return !isProcessingBackwards.isEmpty();
    }

    protected void addNodeToIsProcessing(WebNode wN) {
        if (Direction.FORWARDS.equals(wN.getDirection())) {
            isProcessingForwards.add(wN.getTitle());
        } else {
            isProcessingBackwards.add(wN.getTitle());
        }
    }

    protected void removeNodeFromIsProcessing(WebNode wN) {
        if (Direction.FORWARDS.equals(wN.getDirection())) {
            isProcessingForwards.remove(wN.getTitle());
        } else {
            isProcessingBackwards.remove(wN.getTitle());
        }

    }

    /******************************************/
    // METHODS FOR DETERMINING IF NODE HAS BEEN VISITED
    /******************************************/
    // check if the current crawl has seen this node before
    // note that this does not check for the intersection between backwards and forwards
    // rather, it confirms whether the current crawl has seen this node before within its own queue
    protected boolean visited(WebNode wN) {
        if (Direction.FORWARDS.equals(wN.getDirection())) {
            return visitedForwards.containsKey(wN.getTitle());
        } else {
            return visitedBackwards.containsKey(wN.getTitle());
        }
    }

    protected void markAsVisited(WebNode wN) {
        if (Direction.FORWARDS.equals(wN.getDirection())) {
            visitedForwards.put(wN.getTitle(), wN);
        } else {
            visitedBackwards.put(wN.getTitle(), wN);
        }
    }

    public HashMap<String, WebNode> getVisitedForwards() {
        return visitedForwards;
    }

    public HashMap<String, WebNode> getVisitedBackwards() {
        return visitedBackwards;
    }

}
