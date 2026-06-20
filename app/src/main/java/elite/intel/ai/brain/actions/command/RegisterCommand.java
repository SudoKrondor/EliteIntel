package elite.intel.ai.brain.actions.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterCommand {
    /**
     * Optional ordering hints for the LLM action map: ids of OTHER actions
     * (commands or queries) that this action must appear BEFORE in the
     * generated action list. Cross-registry by design (a command may need
     * to precede a query and vice versa). Empty = no ordering constraint;
     * the action falls back to the default id-sorted position. Dangling
     * references (id not present) are ignored with a WARN; cycles abort
     * map generation. Consumed by the action-map generator, NOT here.
     */
    String[] before() default {};
}
