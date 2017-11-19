package com.danielmcheng1.wikiracing;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.view.Viewer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This class is used to visualize the path from source to destination title
 * To make the animation readable, up to 25 visited pages are shown going out from each start/destTile
 * Nodes are traced in BFS order, but order is not guaranteed within a given level
 */
public class Visualizer {
    public static void visualize(ArrayList<String> path, HashMap<String, WebNode> visitedForwards, HashMap<String, WebNode> visitedBackwards) {
        if (path == null) {
            System.out.println("Crawler must have found path in order to generate visualization");
            return;
        }
        sleep(2500);
        System.out.println("Generating visualization...");
        HashMap<Integer, HashSet<WebNode>> visitedGroupedByDistance = new HashMap<Integer, HashSet<WebNode>>();
        addToGroupByDistance(visitedForwards, visitedGroupedByDistance);
        addToGroupByDistance(visitedBackwards, visitedGroupedByDistance);

        showAnimation(visitedGroupedByDistance, path);
    }

    public static void showAnimation(HashMap<Integer, HashSet<WebNode>> visitedGroupedByDistance, ArrayList<String> path) {
        String sourceTitle = path.get(0);
        String destTitle = path.get(path.size() - 1);

        MultiGraph graph = new MultiGraph("wikigraph", false, true);
        graph.addAttribute("ui.stylesheet", generateStyleSheet());

        // mark initial path outline so that user can see the connection as the animation adds in other nodes
        showPathOutline(graph, path);
        markSourceNode(graph, sourceTitle);
        markDestNode(graph, destTitle);
        Viewer viewer = graph.display(true);

        // animate in nodes in BFS order (i.e. in increasing order of distance from source/dest nodes
        Integer[] sortedDistances = visitedGroupedByDistance.keySet().toArray(new Integer[visitedGroupedByDistance.keySet().size()]);
        Arrays.sort(sortedDistances);

        for (int dist : sortedDistances) {
            // restrict how many nodes we add at each level
            int numAddedForwards = 0, numAddedBackwards = 0, maxNumAdded = 25;
            HashSet<WebNode> webNodes = visitedGroupedByDistance.get(dist);
            for (WebNode currWebNode : webNodes) {
                Direction direction = currWebNode.getDirection();
                Boolean nodeOnPath = nodeOnPath(currWebNode, path);

                // only showing first level for this viz implementation, aside from the connecting path
                if (dist > 1 && !nodeOnPath) {
                    continue;
                }
                // only add if either (1) we haven't reached the max # of this level (2) we have a node on the path to add
                if (Direction.FORWARDS.equals(direction) && !nodeOnPath) {
                    if (numAddedForwards > maxNumAdded) continue;
                    else numAddedForwards++;
                } else if (Direction.BACKWARDS.equals(direction) && !nodeOnPath) {
                    if (numAddedBackwards > maxNumAdded) continue;
                    else numAddedBackwards++;
                }
                // create this edge (currNode to parentNode)
                Edge e = addEdge(graph, currWebNode, direction);

                // finally flag the connecting nodes (since we only drew the outline of this connecting path)
                if (nodeOnPath)
                    markConnectingNode(graph, currWebNode.getTitle());
                sleep(250);
            }
        }
        // remark these in case source/dest nodes overlap with other regular nodes
        markSourceNode(graph, sourceTitle);
        markDestNode(graph, destTitle);
    }

    private static void sleep(Long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void sleep(int millis) {
        sleep((long) millis);
    }

    // utility method to group visited web nodes by distance from source and destination titles
    private static synchronized void addToGroupByDistance(HashMap<String, WebNode> visitedNodes, HashMap<Integer, HashSet<WebNode>> visitedGroupedByDistance) {
        for (String title : visitedNodes.keySet()) {
            WebNode curr = visitedNodes.get(title);
            Integer dist = curr.getDistance();
            if (!visitedGroupedByDistance.containsKey(dist)) {
                visitedGroupedByDistance.put(dist, new HashSet<WebNode>());
            }
            visitedGroupedByDistance.get(dist).add(curr);
        }
    }

    // CSS file per GraphStream's guidelines: http://graphstream-project.org/doc/Advanced-Concepts/GraphStream-CSS-Reference/
    private static String generateStyleSheet() {
        StringBuilder sB = new StringBuilder();
        sB.append("node { fill-color: grey; size: 0px; text-background-mode: plain; text-background-color: purple;}"); // stroke-mode: plain; stroke-color: black; stroke-width: 1px;}");
        sB.append("node.sourceTitle {fill-color: red; size: 15px; text-size: 13; text-style: bold;}");
        sB.append("node.destTitle {fill-color: green; size: 15px; text-size: 13; text-style: bold;}");
        sB.append("node.connectingTitle {fill-color: yellow; size: 15px; text-size: 13; text-style: bold;}");
        sB.append("edge." + Direction.FORWARDS.toString() + "{fill-color: brown;}");
        sB.append("edge." + Direction.BACKWARDS.toString() + "{fill-color: gray;}");
        sB.append("edge.temp {fill-color: gray;}");
        return sB.toString();
    }

    private static void showPathOutline(MultiGraph graph, ArrayList<String> path) {
        for (int i = 0; i < path.size() - 1; i++) {
            String currTitle = path.get(i);
            String nextTitle = path.get(i + 1);
            Edge e = graph.addEdge(currTitle + "->" + nextTitle, currTitle, nextTitle, false);
        }
    }

    private static boolean nodeOnPath(WebNode node, ArrayList<String> path) {
        return path.contains(node.getTitle());
    }

    private static void markSourceNode(MultiGraph graph, String sourceTitle) {
        markSpecialNode(graph, sourceTitle, "sourceTitle");
    }

    private static void markDestNode(MultiGraph graph, String destTitle) {
        markSpecialNode(graph, destTitle, "destTitle");
    }

    private static void markConnectingNode(MultiGraph graph, String connectingTitle) {
        markSpecialNode(graph, connectingTitle, "connectingTitle");
    }

    private static void markSpecialNode(MultiGraph graph, String title, String specialAttribute) {
        Node node = graph.getNode(title);
        addLabelForNode(node);
        node.addAttribute("ui.class", specialAttribute);
    }

    private static Edge addEdge(MultiGraph graph, WebNode currWebNode, Direction direction) {
        String currTitle = currWebNode.getTitle();
        String parentTitle = currWebNode.getParent() == null ? currTitle : currWebNode.getParent().getTitle();

        Edge e;
        if (Direction.FORWARDS.equals(direction))
            e = graph.addEdge(parentTitle + "->" + currTitle, parentTitle, currTitle, true);
        else
            e = graph.addEdge(currTitle + "->" + parentTitle, currTitle, parentTitle, true);

        e.addAttribute("ui.class", direction.toString());
        addLabelForNode(e.getSourceNode());
        addLabelForNode(e.getTargetNode());
        return e;
    }

    private static void addLabelForNode(Node node) {
        node.addAttribute("ui.label", node.getId());
    }
}