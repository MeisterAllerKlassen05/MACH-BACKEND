package com.example.rideofferservice.graphql;

import com.example.rideofferservice.dto.RideResponse;
import com.example.rideofferservice.entity.RideOffer;
import com.example.rideofferservice.graphql.dto.CreateRideInput;
import com.example.rideofferservice.graphql.dto.GraphQLRideResponse;
import com.example.rideofferservice.graphql.dto.PageInfo;
import com.example.rideofferservice.graphql.dto.RideConnection;
import com.example.rideofferservice.graphql.dto.RideDataFetcher;
import com.example.rideofferservice.graphql.dto.RideEdge;
import com.example.rideofferservice.graphql.dto.RideSearchParamsInput;
import com.example.rideofferservice.repository.RideRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GraphQL Query-Resolver für Ride-Abfragen mit Cursor-basierter Pagination.
 */
@Controller
public class RideGraphQLResolver {

    private final RideDataFetcher rideDataFetcher;
    private final RideRepository rideRepository;

    @Autowired
    public RideGraphQLResolver(RideDataFetcher rideDataFetcher, RideRepository rideRepository) {
        this.rideDataFetcher = rideDataFetcher;
        this.rideRepository = rideRepository;
    }

    // --- Helper: leere Connection erstellen (immer non-null zurückgeben) ---
    private RideConnection emptyConnection() {
        PageInfo pageInfo = new PageInfo();
        pageInfo.setStartCursor(null);
        pageInfo.setEndCursor(null);
        pageInfo.setHasNextPage(false);
        pageInfo.setHasPreviousPage(false);
        return new RideConnection(new RideEdge[0], pageInfo);
    }

    // --- Queries mit Pagination ---

    public RideConnection availableRides(Integer first, String after) {
        try {
            List<RideResponse> allRides = rideDataFetcher.availableRides();
            return buildConnection(allRides, first, after);
        } catch (Exception e) {
            System.err.println("[GraphQL] availableRides error: " + e.getMessage());
            return emptyConnection();
        }
    }

    public RideConnection ridesByLocation(String startLocation, String endLocation, Integer first, String after) {
        try {
            List<RideResponse> rides;
            if (endLocation == null || endLocation.isBlank()) {
                rides = rideRepository.findByStatus(RideOffer.RideStatus.OFFERED)
                        .stream()
                        .filter(r -> r.getStartLocation().equals(startLocation))
                        .map(RideResponse::fromEntity)
                        .toList();
            } else {
                rides = rideDataFetcher.ridesByLocation(startLocation, endLocation);
            }
            return buildConnection(rides, first, after);
        } catch (Exception e) {
            System.err.println("[GraphQL] ridesByLocation error: " + e.getMessage());
            return emptyConnection();
        }
    }

    public RideConnection ridesBySearch(RideSearchParamsInput params) {
        try {
            String startLocation = params.startLocation();
            String endLocation = params.endLocation();
            String fromStr = params.from();
            String toStr = params.to();

            List<RideResponse> rides;
            if (fromStr != null && toStr != null) {
                LocalDateTime fromDt = LocalDateTime.parse(fromStr);
                LocalDateTime toDt = LocalDateTime.parse(toStr);
                rides = rideRepository.findByStartAndEndBetween(startLocation, endLocation, fromDt, toDt)
                        .stream().map(RideResponse::fromEntity).toList();
            } else if (fromStr != null) {
                LocalDateTime fromDt = LocalDateTime.parse(fromStr);
                rides = rideRepository.findByStartAndEndAfter(startLocation, endLocation, fromDt)
                        .stream().map(RideResponse::fromEntity).toList();
            } else if (toStr != null) {
                LocalDateTime toDt = LocalDateTime.parse(toStr);
                rides = rideRepository.findByStartAndEndBefore(startLocation, endLocation, toDt)
                        .stream().map(RideResponse::fromEntity).toList();
            } else {
                rides = rideDataFetcher.ridesByLocation(startLocation, endLocation);
            }

            Integer firstVal = params.first() != null ? params.first() : 0;
            String afterVal = params.after();
            return buildConnection(rides, firstVal, afterVal);
        } catch (Exception e) {
            System.err.println("[GraphQL] ridesBySearch error: " + e.getMessage());
            return emptyConnection();
        }
    }

    public GraphQLRideResponse ride(Long id) {
        try {
            Optional<RideResponse> opt = rideDataFetcher.ride(id);
            if (opt.isEmpty()) return null;
            RideResponseMapper mapper = new RideResponseMapper();
            return mapper.toGraphQLResponse(opt.get());
        } catch (Exception e) {
            System.err.println("[GraphQL] ride error: " + e.getMessage());
            return null;
        }
    }

    @PreAuthorize("isAuthenticated()")
    public RideConnection myOfferedRides(Integer first, String after) {
        try {
            List<RideResponse> rides = rideDataFetcher.myOfferedRides();
            return buildConnection(rides, first, after);
        } catch (Exception e) {
            System.err.println("[GraphQL] myOfferedRides error: " + e.getMessage());
            return emptyConnection();
        }
    }

    // --- Hilfsmethoden für Cursor-Pagination ---

    private RideConnection buildConnection(List<RideResponse> allRides, Integer first, String after) {
        boolean hadAfter = (after != null && !after.isEmpty());

        if (hadAfter) {
            long afterId;
            try {
                afterId = Long.parseLong(new String(java.util.Base64.getUrlDecoder().decode(after)));
            } catch (Exception e) {
                throw new IllegalArgumentException("Ungültiger Cursor: " + after, e);
            }
            allRides = allRides.stream()
                    .filter(r -> r.getId().compareTo(afterId) > 0)
                    .collect(Collectors.toList());
        }

        int effectiveFirst = (first != null && first > 0) ? first : allRides.size();
        if (first != null && first > 0) {
            List<RideResponse> sliced = allRides.stream()
                    .limit(first + 1)
                    .collect(Collectors.toList());

            boolean hasNext = sliced.size() > first;
            if (hasNext) {
                sliced = sliced.subList(0, first);
            }
            return createConnection(sliced, hasNext, hadAfter);
        }

        return createConnection(allRides, false, hadAfter);
    }

    private RideConnection createConnection(List<RideResponse> rides, boolean hasNext, boolean hasPreviousPage) {
        RideEdge[] edges = new RideEdge[rides.size()];

        for (int i = 0; i < rides.size(); i++) {
            RideResponse ride = rides.get(i);
            String cursor = java.util.Base64.getUrlEncoder().encodeToString(String.valueOf(ride.getId()).getBytes());
            GraphQLRideResponse gqlResponse = new RideResponseMapper().toGraphQLResponse(ride);
            edges[i] = new RideEdge(gqlResponse, cursor);
        }

        PageInfo pageInfo = new PageInfo();
        if (!rides.isEmpty()) {
            pageInfo.setStartCursor(edges[0].getCursor());
            pageInfo.setEndCursor(edges[edges.length - 1].getCursor());
            pageInfo.setHasNextPage(hasNext);
            pageInfo.setHasPreviousPage(hasPreviousPage);
        } else {
            pageInfo.setStartCursor(null);
            pageInfo.setEndCursor(null);
            pageInfo.setHasNextPage(false);
            pageInfo.setHasPreviousPage(false);
        }

        return new RideConnection(edges, pageInfo);
    }
}
