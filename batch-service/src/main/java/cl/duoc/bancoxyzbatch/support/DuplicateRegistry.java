package cl.duoc.bancoxyzbatch.support;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class DuplicateRegistry {
    private final ConcurrentHashMap<String, Set<String>> keysByJob = new ConcurrentHashMap<>();

    public boolean firstOccurrence(String jobName, String key) {
        return keysByJob.computeIfAbsent(jobName, ignored -> ConcurrentHashMap.newKeySet()).add(key);
    }

    public void clear(String jobName) {
        keysByJob.remove(jobName);
    }
}
