package com.example.rideofferservice.graphql.dto;

/**
 * GraphQL Edge für RideConnection (Paginierung).
 */
public class RideEdge {
    private final GraphQLRideResponse node;
    private final String cursor;

    public RideEdge(GraphQLRideResponse node, String cursor) {
        this.node = node;
        this.cursor = cursor;
    }

    public GraphQLRideResponse getNode() { return node; }
    public String getCursor() { return cursor; }
}
