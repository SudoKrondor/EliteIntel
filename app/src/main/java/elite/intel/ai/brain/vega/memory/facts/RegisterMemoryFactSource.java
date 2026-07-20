package elite.intel.ai.brain.vega.memory.facts;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link MemoryFactSource}: a pluggable provider of live {@code <facts>} appended to the commander system prompt,
 * discovered by {@link MemoryFactSourceRegistry} at startup with the same annotation-scan pattern as
 * {@code @RegisterCommand} / {@code @RegisterQuery}. The annotated class must implement {@link MemoryFactSource}
 * and expose a public no-arg constructor.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterMemoryFactSource {
}
