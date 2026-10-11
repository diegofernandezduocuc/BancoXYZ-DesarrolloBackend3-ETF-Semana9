package cl.duoc.bancoxyzbatch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "batch")
public class BatchExecutionProperties {
    private int chunkSize = 50;
    private int skipLimit = 10000;
    private int retryLimit = 3;
    private long retryBackoffMs = 200;
    private final Partition partition = new Partition();

    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
    public int getSkipLimit() { return skipLimit; }
    public void setSkipLimit(int skipLimit) { this.skipLimit = skipLimit; }
    public int getRetryLimit() { return retryLimit; }
    public void setRetryLimit(int retryLimit) { this.retryLimit = retryLimit; }
    public long getRetryBackoffMs() { return retryBackoffMs; }
    public void setRetryBackoffMs(long retryBackoffMs) { this.retryBackoffMs = retryBackoffMs; }
    public Partition getPartition() { return partition; }

    public static class Partition {
        private int gridSize = 4;
        public int getGridSize() { return gridSize; }
        public void setGridSize(int gridSize) { this.gridSize = gridSize; }
    }
}
