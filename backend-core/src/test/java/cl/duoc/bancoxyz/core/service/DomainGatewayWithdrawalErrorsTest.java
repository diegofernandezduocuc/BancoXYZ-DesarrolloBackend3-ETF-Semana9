package cl.duoc.bancoxyz.core.service;

import cl.duoc.bancoxyz.core.exception.BusinessRuleException;
import cl.duoc.bancoxyz.core.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class DomainGatewayWithdrawalErrorsTest {
    @ParameterizedTest
    @ValueSource(ints = {400, 404, 409})
    void preservaRechazosDelDominio(int status) {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://test.invalid:80/api/pagos/retiros/1"))
                .andRespond(withStatus(HttpStatus.valueOf(status)));
        var discovery = mock(DiscoveryClient.class);
        when(discovery.getInstances("bancoxyz-pagos-service")).thenReturn(List.of(
                new DefaultServiceInstance("test", "bancoxyz-pagos-service", "test.invalid", 80, false)));
        var tokens = mock(InternalBankTokenClient.class);
        when(tokens.token("bank.write")).thenReturn("unit-test");
        try (var rest = mockStatic(RestClient.class)) {
            rest.when(RestClient::builder).thenReturn(builder);
            var gateway = new DomainGateway(discovery, tokens);
            if (status == 400) {
                var error = assertThrows(ResponseStatusException.class,
                        () -> gateway.retiro(1L, new BigDecimal("0.015")));
                assertEquals(400, error.getStatusCode().value());
            } else {
                Class<? extends RuntimeException> expected = status == 404
                        ? ResourceNotFoundException.class : BusinessRuleException.class;
                assertThrows(expected,
                        () -> gateway.retiro(1L, BigDecimal.TEN));
            }
        }
        server.verify();
    }
}
