package com.example.rideofferservice.graphql;

import com.example.rideofferservice.dto.RideResponse;
import com.example.rideofferservice.graphql.dto.GraphQLRideResponse;
import org.springframework.stereotype.Component;

@Component
public class RideResponseMapper {

    public GraphQLRideResponse toGraphQLResponse(RideResponse ride) {
        if (ride == null) return null;

        GraphQLRideResponse mapper = new GraphQLRideResponse();
        mapper.setId(ride.getId());
        mapper.setDriverUsername(ride.getDriverUsername());
        mapper.setStartLocation(ride.getStartLocation());
        mapper.setEndLocation(ride.getEndLocation());
        mapper.setDepartureTime(ride.getDepartureTime().toString());
        mapper.setAvailableSeats(ride.getAvailableSeats());
        mapper.setAdditionalInfo(ride.getAdditionalInfo());
        mapper.setStatus(ride.getStatus().name());
        // createdAt wird sp�ter vom Entity gemappt
        return mapper;
    }
}
