package cl.duoc.bancoxyz.mobile.controller;

import cl.duoc.bancoxyz.mobile.config.SecurityConfig;
import cl.duoc.bancoxyz.mobile.service.MobileSummaryService;
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

@WebMvcTest(MobileController.class)
@Import(SecurityConfig.class)
class MobileControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MobileSummaryService service;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void rechazaSolicitudSinCredenciales() throws Exception {
        mockMvc.perform(get("/api/mobile/cuentas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void permiteSolicitudConCredencialesDelCanal() throws Exception {
        when(service.listarCuentas()).thenReturn(List.of());
        mockMvc.perform(get("/api/mobile/cuentas").with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_mobile.read"))))
                .andExpect(status().isOk());
    }
}
