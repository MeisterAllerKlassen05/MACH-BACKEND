package com.example.rideofferservice.graphql.dto;

/**
 * GraphQL Connection für Ride-Ergebnisse (Paginierung).
 */
public class RideConnection {
    private final RideEdge[] edges;
    private final PageInfo pageInfo;

    public RideConnection(RideEdge[] edges, PageInfo pageInfo) {
        this.edges = edges;
        this.pageInfo = pageInfo;
    }

    public RideEdge[] getEdges() { return edges; }
    public PageInfo getPageInfo() { return pageInfo; }
}
