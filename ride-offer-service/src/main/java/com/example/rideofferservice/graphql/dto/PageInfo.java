package com.example.rideofferservice.graphql.dto;

/**
 * GraphQL PageInfo für RideConnection (Paginierung).
 */
public class PageInfo {
    private String startCursor;
    private String endCursor;
    private boolean hasNextPage;
    private boolean hasPreviousPage;

    public PageInfo() {}

    public String getStartCursor() { return startCursor; }
    public void setStartCursor(String startCursor) { this.startCursor = startCursor; }

    public String getEndCursor() { return endCursor; }
    public void setEndCursor(String endCursor) { this.endCursor = endCursor; }

    public boolean isHasNextPage() { return hasNextPage; }
    public void setHasNextPage(boolean hasNextPage) { this.hasNextPage = hasNextPage; }

    public boolean isHasPreviousPage() { return hasPreviousPage; }
    public void setHasPreviousPage(boolean hasPreviousPage) { this.hasPreviousPage = hasPreviousPage; }
}
