package com.danielmcheng1.wikiracing;


/**
 * Data structure for a link
 */
public class WebNode {
    private String title;
    private WebNode parent;
    private Direction direction;
    private int distance;

    public WebNode(String title, WebNode parent, Direction direction, int distance) {
        this.title = title;
        this.parent = parent;
        this.direction = direction;
        this.distance = distance; // # of nodes from the source or destination node (depending on the direction)
    }

    public String getTitle() {
        return this.title;
    }

    public WebNode getParent() {
        return this.parent;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public int getDistance() {
        return this.distance;
    }

    public String toString() {
        String parentTitle = parent == null ? "NULL" : parent.getTitle(); // source and dest nodes will not have a parent
        return "{title: " + title + ", direction: " + direction + ", parentTitle: " + parentTitle + ", distance: " + distance + "}";
    }
}
