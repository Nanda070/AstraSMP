package com.astrasmp.model;

import com.astrasmp.util.LocationKey;
import java.util.Objects;

public class MENode {
    private final LocationKey location;
    private final NodeType type;

    public enum NodeType {
        CONTROLLER, DRIVE, TERMINAL
    }

    public MENode(LocationKey location, NodeType type) {
        this.location = location;
        this.type = type;
    }

    public LocationKey getLocation() {
        return location;
    }

    public NodeType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MENode meNode = (MENode) o;
        return location.equals(meNode.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location);
    }
}