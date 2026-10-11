package cl.duoc.bancoxyzbatch.partition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cl.duoc.bancoxyzbatch.support.SourceDataCounter;
import org.junit.jupiter.api.Test;

class LineRangePartitionerTest {
    private static final String SOURCE = "data/semana_3/movimientos_financieros_diarios.csv";

    @Test
    void divideFuenteOficialSinSolapamiento() {
        SourceDataCounter counter = new SourceDataCounter();
        LineRangePartitioner partitioner = new LineRangePartitioner(SOURCE, counter);
        var parts = partitioner.partition(4);
        assertEquals(4, parts.size());
        int total = parts.values().stream().mapToInt(ctx -> ctx.getInt("itemCount")).sum();
        assertEquals(counter.countDataRows(SOURCE), total);
    }
}
