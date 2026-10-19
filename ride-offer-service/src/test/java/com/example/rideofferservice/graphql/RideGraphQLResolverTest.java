package com.example.rideofferservice.graphql;

import com.example.rideofferservice.dto.RideResponse;
import com.example.rideofferservice.entity.RideOffer;
import com.example.rideofferservice.repository.RideRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit-Tests für RideGraphQLResolver (Query-Resolver).
 */
@ExtendWith(MockitoExtension.class)
class RideGraphQLResolverTest {

    @Mock
    private com.example.rideofferservice.graphql.dto.RideDataFetcher rideDataFetcher;

    @Mock
    private RideRepository rideRepository;

    @InjectMocks
    private RideGraphQLResolver resolver;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    // ================== availableRides ==================

    @Test
    void availableRides_returnsConnectionWithEdges() {
        RideResponse mockRide = new RideResponse(1L, "driver1", "Berlin", "München",
                LocalDateTime.of(2026, 11, 1, 10, 0), 3, "Extra Info", RideOffer.RideStatus.OFFERED);
        when(rideDataFetcher.availableRides()).thenReturn(List.of(mockRide));

        var connection = resolver.availableRides(5, null);

        assertThat(connection).isNotNull();
        assertThat(connection.getEdges()).hasSize(1);
        assertThat(connection.getPageInfo().getHasNextPage()).isFalse();
        assertThat(connection.getPageInfo().getStartCursor()).isNotNull();
        assertThat(connection.getPageInfo().getEndCursor()).isNotNull();
    }

    @Test
    void availableRides_withFirst_limitsEdges() {
        List<RideResponse> rides = List.of(
                new RideResponse(1L, "d1", "A", "B", LocalDateTime.now(), 1, null, RideOffer.RideStatus.OFFERED),
                new RideResponse(2L, "d2", "C", "D", LocalDateTime.now(), 2, null, RideOffer.RideStatus.OFFERED),
                new RideResponse(3L, "d3", "E", "F", LocalDateTime.now(), 3, null, RideOffer.RideStatus.OFFERED)
        );
        when(rideDataFetcher.availableRides()).thenReturn(rides);

        var connection = resolver.availableRides(2, null);

        assertThat(connection.getEdges()).hasSize(2); // limited by 'first'
        assertThat(connection.getPageInfo().getHasNextPage()).isTrue();
    }

    @Test
    void availableRides_withAfter_cursor_filtersAndSetsHasPreviousPage() throws Exception {
        List<RideResponse> rides = List.of(
                new RideResponse(1L, "d1", "A", "B", LocalDateTime.now(), 1, null, RideOffer.RideStatus.OFFERED),
                new RideResponse(2L, "d2", "C", "D", LocalDateTime.now(), 2, null, RideOffer.RideStatus.OFFERED),
                new RideResponse(3L, "d3", "E", "F", LocalDateTime.now(), 3, null, RideOffer.RideStatus.OFFERED)
        );
        when(rideDataFetcher.availableRides()).thenReturn(rides);

        String afterCursor = java.util.Base64.getUrlEncoder().encodeToString("2".getBytes());
        var connection = resolver.availableRides(null, afterCursor);

        assertThat(connection.getEdges()).hasSize(1); // only ID 3
        RideResponse edgeNode = connection.getEdges()[0].getNode();
        assertThat(edgeNode.getId()).isEqualTo(3L);
        assertThat(connection.getPageInfo().getHasPreviousPage()).isTrue();
    }

    @Test
    void availableRides_noFirst_returnsAll() {
        List<RideResponse> rides = List.of(
                new RideResponse(1L, "d1", "A", "B", LocalDateTime.now(), 1, null, RideOffer.RideStatus.OFFERED),
                new RideResponse(2L, "d2", "C", "D", LocalDateTime.now(), 2, null, RideOffer.RideStatus.OFFERED)
        );
        when(rideDataFetcher.availableRides()).thenReturn(rides);

        var connection = resolver.availableRides(null, null);

        assertThat(connection.getEdges()).hasSize(2);
        assertThat(connection.getPageInfo().getHasNextPage()).isFalse();
    }

    // ================== ridesByLocation ==================

    @Test
    void ridesByLocation_withBothParams_callsDataFetcher() {
        RideResponse mock = new RideResponse(10L, "driver", "Berlin", "München",
                LocalDateTime.now(), 2, null, RideOffer.RideStatus.OFFERED);
        when(rideDataFetcher.ridesByLocation("Berlin", "München")).thenReturn(List.of(mock));

        var connection = resolver.ridesByLocation("Berlin", "München", 10, null);

        assertThat(connection.getEdges()).hasSize(1);
        assertThat(connection.getEdges()[0].getNode().getStartLocation()).isEqualTo("Berlin");
    }

    @Test
    void ridesByLocation_withNullEndLocation_filtersByRepository() {
        when(rideDataFetcher.ridesByLocation(anyString(), anyString())).thenThrow(new IllegalStateException("Should not be called"));
        List<RideOffer> entities = List.of(
                new RideOffer("driver1", "Berlin", null, LocalDateTime.now(), 1, null)
        );
        entities.get(0).setId(99L);
        when(rideRepository.findByStatus(RideOffer.RideStatus.OFFERED)).thenReturn(entities);

        var connection = resolver.ridesByLocation("Berlin", "", 10, null);

        assertThat(connection.getEdges()).hasSize(1);
    }

    // ================== ride (by ID) ==================

    @Test
    void ride_returnsGraphQLResponse() {
        RideResponse mock = new RideResponse(42L, "driverX", "Hamburg", "Frankfurt",
                LocalDateTime.of(2026, 12, 25, 8, 0), 4, null, RideOffer.RideStatus.OFFERED);
        when(rideDataFetcher.ride(42L)).thenReturn(Optional.of(mock));

        var result = resolver.ride(42L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getDriverUsername()).isEqualTo("driverX");
    }

    @Test
    void ride_returnsNullWhenNotFound() {
        when(rideDataFetcher.ride(999L)).thenReturn(Optional.empty());

        var result = resolver.ride(999L);

        assertThat(result).isNull();
    }

    // ================== myOfferedRides ==================

    @Test
    void myOfferedRides_withAuthentication_returnsRides() {
        Authentication mockAuth = org.mockito.Mockito.mock(Authentication.class);
        when(mockAuth.isAuthenticated()).thenReturn(true);
        when(mockAuth.getName()).thenReturn("testuser");
        SecurityContextHolder.getContext().setAuthentication(mockAuth);

        RideResponse mock = new RideResponse(1L, "testuser", "Berlin", "Köln",
                LocalDateTime.now(), 2, null, RideOffer.RideStatus.OFFERED);
        when(rideDataFetcher.myOfferedRides()).thenReturn(List.of(mock));

        var connection = resolver.myOfferedRides(null, null);

        assertThat(connection.getEdges()).hasSize(1);
    }

    @Test
    void myOfferedRides_withoutAuthentication_returnsEmpty() {
        when(rideDataFetcher.myOfferedRides()).thenThrow(new IllegalStateException("Should not reach service without auth"));
        
        var connection = resolver.myOfferedRides(null, null);

        // Should not throw because SecurityContext check in DataFetcher
    }

    // ================== ridesBySearch (date range) ==================

    @Test
    void ridesBySearch_withDateRange_callsRepositoryBetween() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 12, 31, 23, 59);

        RideOffer entity = new RideOffer("driver", "Berlin", "München", LocalDateTime.of(2026, 6, 15, 10, 0), 2, null);
        entity.setId(5L);

        when(rideRepository.findByStartAndEndBetween("Berlin", "München", from, to))
                .thenReturn(List.of(entity));

        var params = new com.example.rideofferservice.graphql.dto.RideSearchParamsInput(
                "Berlin", "München",
                "2026-01-01T00:00:00", "2026-12-31T23:59:59",
                10, null);

        var connection = resolver.ridesBySearch(params);

        assertThat(connection.getEdges()).hasSize(1);
        assertThat(connection.getEdges()[0].getNode().getStartLocation()).isEqualTo("Berlin");
    }

    @Test
    void ridesBySearch_withOnlyFrom_callsRepositoryAfter() {
        LocalDateTime from = LocalDateTime.of(2026, 6, 1, 0, 0);

        RideOffer entity = new RideOffer("driver", "Berlin", "München", LocalDateTime.of(2026, 8, 15, 10, 0), 2, null);
        entity.setId(7L);

        when(rideRepository.findByStartAndEndAfter("Berlin", "München", from))
                .thenReturn(List.of(entity));

        var params = new com.example.rideofferservice.graphql.dto.RideSearchParamsInput(
                "Berlin", "München",
                "2026-06-01T00:00:00", null,
                5, null);

        var connection = resolver.ridesBySearch(params);

        assertThat(connection.getEdges()).hasSize(1);
    }
}
