package cl.duoc.bancoxyzbatch.support;

import java.util.Arrays;
import java.util.List;
import org.springframework.batch.core.step.skip.SkipPolicy;

public class BancoSkipPolicy implements SkipPolicy {
    private final long skipLimit;
    private final List<Class<? extends Throwable>> skippable;

    @SafeVarargs
    public BancoSkipPolicy(long skipLimit, Class<? extends Throwable>... skippable) {
        this.skipLimit = skipLimit;
        this.skippable = Arrays.asList(skippable);
    }

    @Override
    public boolean shouldSkip(Throwable t, long skipCount) {
        boolean allowedType = skippable.stream().anyMatch(type -> type.isAssignableFrom(t.getClass()));
        if (!allowedType) {
            return false;
        }
        return skipCount < 0 || skipCount < skipLimit;
    }
}
