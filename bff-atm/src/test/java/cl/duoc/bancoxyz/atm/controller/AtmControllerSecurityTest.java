package cl.duoc.bancoxyz.atm.controller;

import cl.duoc.bancoxyz.atm.config.SecurityConfig;
import cl.duoc.bancoxyz.atm.model.AtmSaldoResponse;
import cl.duoc.bancoxyz.atm.service.AtmService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AtmController.class)
@Import(SecurityConfig.class)
class AtmControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AtmService service;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void rechazaSolicitudSinCredenciales() throws Exception {
        mockMvc.perform(get("/api/atm/cuentas/106/saldo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void permiteSolicitudConCredencialesDelCanal() throws Exception {
        when(service.consultarSaldo(106L)).thenReturn(new AtmSaldoResponse(106L, new BigDecimal("12000")));
        mockMvc.perform(get("/api/atm/cuentas/106/saldo").with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_atm.read"))))
                .andExpect(status().isOk());
    }

    @Test
    void informaSaldoInsuficienteComoConflicto() throws Exception {
        when(service.retirar(106L, new BigDecimal("999999")))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Saldo insuficiente para realizar el retiro"));

        mockMvc.perform(post("/api/atm/cuentas/106/retiros")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_atm.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":999999}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Saldo insuficiente para realizar el retiro"));
    }
}
