package com.example.rideofferservice.graphql;

import com.example.rideofferservice.dto.CreateRideRequest;
import com.example.rideofferservice.dto.RideResponse;
import com.example.rideofferservice.entity.RideOffer;
import com.example.rideofferservice.service.RideService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit-Tests für RideMutationResolver.
 */
@ExtendWith(MockitoExtension.class)
class RideMutationResolverTest {

    @Mock
    private RideService rideService;

    @InjectMocks
    private RideMutationResolver resolver;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    // ================== createRide ==================

    @Test
    void createRide_callsRideServiceWithMappedInput() {
        String testUser = "graphql-user";
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.isAuthenticated()).thenReturn(true);
        when(mockAuth.getName()).thenReturn(testUser);
        SecurityContextHolder.getContext().setAuthentication(mockAuth);

        RideResponse expected = new RideResponse(99L, testUser, "Berlin", "Hamburg",
                LocalDateTime.of(2026, 7, 1, 14, 0), 3, "Nice ride!", RideOffer.RideStatus.OFFERED);
        when(rideService.createRide(eq(testUser), any(CreateRideRequest.class))).thenReturn(expected);

        var input = new com.example.rideofferservice.graphql.dto.CreateRideInput(
                "Berlin", "Hamburg",
                "2026-07-01T14:00:00", 3, "Nice ride!");

        RideResponse result = resolver.createRide(input);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getDriverUsername()).isEqualTo(testUser);
        verify(rideService).createRide(eq(testUser), argThat(req ->
                req.getStartLocation().equals("Berlin") &&
                req.getEndLocation().equals("Hamburg") &&
                req.getAvailableSeats() == 3 &&
                req.getAdditionalInfo().equals("Nice ride!")
        ));
    }

    @Test
    void createRide_withAnonUser_mapsToAnonymous() {
        RideResponse expected = new RideResponse(1L, "anonymous", "A", "B",
                LocalDateTime.now(), 1, null, RideOffer.RideStatus.OFFERED);
        when(rideService.createRide(eq("anonymous"), any(CreateRideRequest.class))).thenReturn(expected);

        var input = new com.example.rideofferservice.graphql.dto.CreateRideInput(
                "A", "B", LocalDateTime.now().toString(), 1, null);

        RideResponse result = resolver.createRide(input);

        assertThat(result.getDriverUsername()).isEqualTo("anonymous");
    }

    // ================== cancelRide ==================

    @Test
    void cancelRide_callsRideService() {
        String testUser = "canceller";
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.isAuthenticated()).thenReturn(true);
        when(mockAuth.getName()).thenReturn(testUser);
        SecurityContextHolder.getContext().setAuthentication(mockAuth);

        RideResponse expected = new RideResponse(10L, testUser, "X", "Y",
                LocalDateTime.now(), 2, null, RideOffer.RideStatus.CANCELLED);
        when(rideService.cancelRide(10L, testUser)).thenReturn(expected);

        RideResponse result = resolver.cancelRide(10L);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(RideOffer.RideStatus.CANCELLED);
        verify(rideService).cancelRide(10L, testUser);
    }

    @Test
    void cancelRide_withAnonUser_mapsToAnonymous() {
        RideResponse expected = new RideResponse(10L, "anonymous", "X", "Y",
                LocalDateTime.now(), 2, null, RideOffer.RideStatus.CANCELLED);
        when(rideService.cancelRide(10L, "anonymous")).thenReturn(expected);

        RideResponse result = resolver.cancelRide(10L);

        assertThat(result.getDriverUsername()).isEqualTo("anonymous");
    }

    // ================== deleteRide ==================

    @Test
    void deleteRide_callsRideServiceAndReturnsTrue() {
        String testUser = "deleter";
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.isAuthenticated()).thenReturn(true);
        when(mockAuth.getName()).thenReturn(testUser);
        SecurityContextHolder.getContext().setAuthentication(mockAuth);

        doNothing().when(rideService).deleteRide(20L, testUser);

        boolean result = resolver.deleteRide(20L);

        assertThat(result).isTrue();
        verify(rideService).deleteRide(20L, testUser);
    }
}
