package cl.duoc.bancoxyz.core.controller;

import cl.duoc.bancoxyz.core.config.SecurityConfig;
import cl.duoc.bancoxyz.core.model.Cuenta;
import cl.duoc.bancoxyz.core.service.LegacyDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BackendController.class)
@Import(SecurityConfig.class)
class BackendControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LegacyDataService service;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void rechazaSolicitudSinToken() throws Exception {
        mockMvc.perform(get("/api/backend/cuentas/106"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void permiteLecturaConScopeCorrecto() throws Exception {
        when(service.getCuenta(106L)).thenReturn(
                new Cuenta(106L, "Jane Smith", new BigDecimal("12000"), 32, "ahorro"));

        mockMvc.perform(get("/api/backend/cuentas/106")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_bank.read"))))
                .andExpect(status().isOk());
    }

    @Test
    void rechazaEscrituraConScopeSoloLectura() throws Exception {
        mockMvc.perform(post("/api/backend/cuentas/106/retiros")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_bank.read")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":1000}"))
                .andExpect(status().isForbidden());
    }
}
