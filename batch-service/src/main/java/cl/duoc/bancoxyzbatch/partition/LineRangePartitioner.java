package cl.duoc.bancoxyzbatch.partition;

import cl.duoc.bancoxyzbatch.support.SourceDataCounter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

public class LineRangePartitioner implements Partitioner {
    private final String classpathLocation;
    private final SourceDataCounter counter;

    public LineRangePartitioner(String classpathLocation, SourceDataCounter counter) {
        this.classpathLocation = classpathLocation;
        this.counter = counter;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        int total = counter.countDataRows(classpathLocation);
        int partitions = Math.max(1, Math.min(gridSize, Math.max(total, 1)));
        int blockSize = (int) Math.ceil(total / (double) partitions);
        Map<String, ExecutionContext> result = new LinkedHashMap<>();

        int startIndex = 0;
        int partitionNumber = 0;
        while (startIndex < total) {
            int endIndex = Math.min(total - 1, startIndex + blockSize - 1);
            ExecutionContext context = new ExecutionContext();
            context.putInt("startIndex", startIndex);
            context.putInt("endIndex", endIndex);
            context.putInt("itemCount", endIndex - startIndex + 1);
            result.put("partition" + partitionNumber, context);
            startIndex = endIndex + 1;
            partitionNumber++;
        }
        return result;
    }
}
