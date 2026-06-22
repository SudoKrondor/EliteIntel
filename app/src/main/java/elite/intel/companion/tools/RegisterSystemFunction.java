package elite.intel.companion.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link SystemFunction} for auto-discovery by {@link SystemFunctionRegistry}.
 * Mirrors {@code @RegisterCommand} for companion system functions.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterSystemFunction {
}
