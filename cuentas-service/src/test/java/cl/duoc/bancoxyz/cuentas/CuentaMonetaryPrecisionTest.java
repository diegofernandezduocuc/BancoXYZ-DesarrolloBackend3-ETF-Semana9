package cl.duoc.bancoxyz.cuentas;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CuentaMonetaryPrecisionTest {
    @Test
    void fraccionesDeCentavoDevuelven400AntesDeAccederALaBase() throws Exception {
        var db = mock(JdbcTemplate.class);
        var mvc = MockMvcBuilders.standaloneSetup(new CuentaController(new CuentaRepository(db))).build();
        for (String monto : List.of("0.015", "10.001")) {
            for (String operation : List.of("debitar", "acreditar", "transferir")) {
                mvc.perform(post("/api/cuentas/1/" + operation).contentType("application/json")
                        .content("{\"monto\":" + monto + ",\"destinoId\":2}"))
                        .andExpect(status().isBadRequest());
            }
            mvc.perform(post("/api/cuentas").contentType("application/json")
                    .content("{\"titular\":\"Prueba\",\"edad\":30,\"tipo\":\"ahorro\",\"saldo\":" + monto + "}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(db);
    }

    @Test
    void conservaImporteExactoConCerosAdicionales() {
        var db = mock(JdbcTemplate.class);
        when(db.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<BigDecimal>>any(), eq(1L)))
                .thenReturn(List.of(new BigDecimal("100.00")));
        var result = new CuentaRepository(db).movimiento(1L, new BigDecimal("10.000"), true, "retiro");
        assertEquals(0, new BigDecimal("90.00").compareTo(result.saldoActual()));
        verify(db).update("UPDATE cuentas SET saldo=? WHERE id=?", result.saldoActual(), 1L);
    }

    @Test
    void aperturaConSaldoCeroOExactoSiguePermitida() {
        var db = mock(JdbcTemplate.class);
        when(db.update(any(PreparedStatementCreator.class), any(KeyHolder.class))).thenAnswer(invocation -> {
            invocation.<KeyHolder>getArgument(1).getKeyList().add(Map.of("id", 1L));
            return 1;
        });
        var repository = new CuentaRepository(db);
        assertEquals(1L, repository.apertura("Prueba", 30, "ahorro", BigDecimal.ZERO));
        assertEquals(1L, repository.apertura("Prueba", 30, "ahorro", new BigDecimal("10.000")));
    }
}
