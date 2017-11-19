package com.danielmcheng1.wikiracing;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to build a Path once we have found an intersection between the forward and backward crawl.
 */
public class Path {
    private static final Logger LOGGER = Logger.getLogger(Path.class.getName());
    private HashMap<String, WebNode> visitedForwards;
    private HashMap<String, WebNode> visitedBackwards;
    private WebNode finalConnectingNode;
    private Deque<WebNode> webNodePath;
    private boolean foundPath;

    public Path(HashMap<String, WebNode> visitedForwards, HashMap<String, WebNode> visitedBackwards) {
        this.visitedForwards = visitedForwards;
        this.visitedBackwards = visitedBackwards;
    }

    // if we've found the connecting midpoint node, then save the path going forwards and backwards
    public void savePath(WebNode connectingNode) {
        finalConnectingNode = connectingNode;
        tracePath();
        this.foundPath = true;
    }

    // use a deque so we can build bidirectionally
    private void tracePath() {
        if (finalConnectingNode == null) {
            throw new java.lang.IllegalStateException("Path cannot be calculated");
        }
        this.webNodePath = new ArrayDeque<WebNode>();

        // we know the midpoint -- but we need to retrieve the nodes from both FORWARDS and BACKWARDS
        // so that we can walk in both directions AWAY from this midpoint
        WebNode forwardsNode = getForwardsConnectingNode();
        WebNode backwardsNode = getBackwardsConnectingNode();
        LOGGER.log(Level.INFO, Util.getThread() + "Final connecting node: " + finalConnectingNode);
        LOGGER.log(Level.INFO, Util.getThread() + "ForwardsNode: " + forwardsNode);
        LOGGER.log(Level.INFO, Util.getThread() + "BackwardsNode: " + backwardsNode);

        // now retrace our path until we hit the source (or destination) node
        while (forwardsNode != null) {
            webNodePath.addFirst(forwardsNode);
            forwardsNode = forwardsNode.getParent();
        }
        while (backwardsNode != null) {
            webNodePath.addLast(backwardsNode);
            backwardsNode = backwardsNode.getParent();
        }
    }

    // bidirectional: check if the we've found a node that intersects with the OPPOSITE crawl
    // unidirectional: works identically since backwards queue only contains the source node
    public void markIfNodeCompletesPath(WebNode wN) {
        String title = wN.getTitle();
        if (Direction.FORWARDS.equals(wN.getDirection())) {
            if (visitedBackwards.containsKey(title)) {
                savePath(wN);
            }
        } else {
            if (visitedForwards.containsKey(title)) {
                savePath(wN);
            }
        }
    }
    // if the connecting node was found when crawling forwards
    // we need to find the node crawling backwards--
    // by looking up the connecting String title in visitedBackwards
    // (and vice versa for crawling the opposite direction)
    private WebNode getForwardsConnectingNode() {
        if (finalConnectingNode == null) {
            return null;
        }
        if (Direction.FORWARDS.equals(finalConnectingNode.getDirection())) {
            return finalConnectingNode;
        } else {
            return visitedForwards.get(finalConnectingNode.getTitle());
        }
    }

    private WebNode getBackwardsConnectingNode() {
        if (finalConnectingNode == null) {
            return null;
        }
        if (Direction.BACKWARDS.equals(finalConnectingNode.getDirection())) {
            return finalConnectingNode;
        } else {
            return visitedBackwards.get(finalConnectingNode.getTitle());
        }
    }

    public Deque<WebNode> getPath() {
        return webNodePath;
    }

    public boolean foundPath() {
        return foundPath;
    }

    public static void setLogLevel(Level level) {
        LOGGER.setLevel(level);
    }
}
