package elite.intel.gameapi;

import com.google.common.eventbus.Subscribe;
import elite.intel.eventbus.GameEventBus;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * The SubscriberRegistration class handles the registration of subscriber classes
 * to the event bus system using reflection. It automatically identifies and registers
 * classes that contain methods annotated with {@code @Subscribe}.
 * <p>
 * This class is responsible for dynamically discovering and instantiating subscriber
 * classes under specific packages. It helps facilitate event-driven communication
 * within the application by ensuring all relevant subscribers are registered
 * with the event bus.
 */
public class SubscriberRegistration {
    private static final String[] SCANNED_PACKAGES = {
            "elite.intel.gameapi.journal.subscribers",
            "elite.intel.gameapi.gamestate.subscribers",
            "elite.intel.ai.mouth.subscribers",
            "elite.intel.gameapi.edsm"
    };

    /**
     * Registers all subscriber classes containing methods annotated with the {@code @Subscribe} annotation
     * to the event bus managed by the {@code GameEventBus}.
     *
     * If a class cannot be instantiated (e.g., due to a lack of a default constructor,
     * security restrictions, or other exceptions), an error message is logged to the standard error stream.
     */
    public static void registerSubscribers() {
        for (Class<?> subscriberClass : liveSubscriberClasses()) {
            try {
                Object subscriberInstance = subscriberClass.getDeclaredConstructor().newInstance();
                GameEventBus.register(subscriberInstance);
            } catch (Exception e) {
                System.err.println("Failed to instantiate subscriber: " + subscriberClass.getName());
            }
        }
    }

    /**
     * The classes that belong on the live bus: every {@code @Subscribe} carrier in the scanned
     * packages, less those marked {@link PreScanOnly}.
     *
     * <p>WHY: the pre-scan subscribers sit in a scanned package but belong to the JournalPreScanner
     * private bus alone. See {@link PreScanOnly} for what registering them here breaks. Visible so
     * the guard test can assert on the set this method actually produces.
     */
    public static Set<Class<?>> liveSubscriberClasses() {
        Reflections reflections = new Reflections(SCANNED_PACKAGES, new MethodAnnotationsScanner());
        Set<Method> annotatedMethods = reflections.getMethodsAnnotatedWith(Subscribe.class);

        Set<Class<?>> subscriberClasses = new HashSet<>();
        for (Method method : annotatedMethods) {
            Class<?> subscriberClass = method.getDeclaringClass();
            if (subscriberClass.isAnnotationPresent(PreScanOnly.class)) continue;
            subscriberClasses.add(subscriberClass);
        }
        return subscriberClasses;
    }
}