package com.recred.backend.unit.infrastructure.openMeteo;

import com.recred.backend.infrastructure.openMeteo.NominatimGeocodingClient;
import com.recred.backend.infrastructure.web.GeocodingResponseDTO;
import java.net.URI;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NominatimGeocodingClientTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void getReverseGeocodingShouldCallNominatimAndReturnBody() {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec =
                mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        GeocodingResponseDTO body = new GeocodingResponseDTO();
        when(builder.baseUrl("https://nominatim.openstreetmap.org")).thenReturn(builder);
        when(builder.defaultHeader(
                "User-Agent",
                "RecredBackend/1.0 (contact@recred.com)"
        )).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(Function.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(GeocodingResponseDTO.class)).thenReturn(body);

        NominatimGeocodingClient client = new NominatimGeocodingClient(builder);
        GeocodingResponseDTO response = client.getReverseGeocoding(-34.6, -58.4);

        assertEquals(body, response);
        verify(builder).baseUrl("https://nominatim.openstreetmap.org");
        verify(builder).defaultHeader(
                "User-Agent",
                "RecredBackend/1.0 (contact@recred.com)"
        );
    }

    @Test
    void getReverseGeocodingShouldReturnNullWhenRestClientFails() {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient restClient = mock(RestClient.class);
        when(builder.baseUrl("https://nominatim.openstreetmap.org")).thenReturn(builder);
        when(builder.defaultHeader(any(String.class), any(String.class))).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);
        when(restClient.get()).thenThrow(new RuntimeException("network"));

        NominatimGeocodingClient client = new NominatimGeocodingClient(builder);

        assertNull(client.getReverseGeocoding(-34.6, -58.4));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void uriFunctionShouldIncludeReversePathAndCoordinates() {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec =
                mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(builder.baseUrl("https://nominatim.openstreetmap.org")).thenReturn(builder);
        when(builder.defaultHeader(any(String.class), any(String.class))).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(Function.class))).thenAnswer(invocation -> {
            Function<UriBuilder, URI> function = invocation.getArgument(0);
            UriBuilder uriBuilder = org.springframework.web.util.UriComponentsBuilder.newInstance();
            URI uri = function.apply(uriBuilder);
            assertEquals("/reverse?lat=-34.6&lon=-58.4&format=json", uri.toString());
            return headersSpec;
        });
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        new NominatimGeocodingClient(builder).getReverseGeocoding(-34.6, -58.4);
    }
}
