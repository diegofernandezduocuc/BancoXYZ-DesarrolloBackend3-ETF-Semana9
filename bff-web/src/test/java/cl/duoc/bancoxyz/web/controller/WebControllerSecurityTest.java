package cl.duoc.bancoxyz.web.controller;

import cl.duoc.bancoxyz.web.config.SecurityConfig;
import cl.duoc.bancoxyz.web.service.WebAggregationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebController.class)
@Import(SecurityConfig.class)
class WebControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WebAggregationService service;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void rechazaSolicitudSinCredenciales() throws Exception {
        mockMvc.perform(get("/api/web/cuentas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void permiteSolicitudConCredencialesDelCanal() throws Exception {
        when(service.listarCuentas()).thenReturn(List.of());
        mockMvc.perform(get("/api/web/cuentas").with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_web.read"))))
                .andExpect(status().isOk());
    }
}
