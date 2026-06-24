package elite.intel.gameapi;

import com.google.common.eventbus.Subscribe;
import elite.intel.eventbus.GameEventBus;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;

import java.lang.reflect.Method;
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
    /**
     * Registers all subscriber classes containing methods annotated with the {@code @Subscribe} annotation
     * to the event bus managed by the {@code GameEventBus}.
     *
     * This method uses reflection to scan specific packages for classes housing methods
     * annotated with {@code @Subscribe}. For each identified class, this method attempts
     * to instantiate it and registers the resulting instance with the event bus.
     *
     * If a class cannot be instantiated (e.g., due to a lack of a default constructor,
     * security restrictions, or other exceptions), an error message is logged to the standard error stream.
     *
     * The method scans the following packages:
     * - elite.intel.gameapi.journal.subscribers
     * - elite.intel.gameapi.gamestate.subscribers
     *
     */
    public static void registerSubscribers() {
        Reflections reflections = new Reflections(
                "elite.intel.gameapi.journal.subscribers",
                "elite.intel.gameapi.gamestate.subscribers",
                "elite.intel.ai.mouth.subscribers",
                "elite.intel.gameapi.edsm",
                new MethodAnnotationsScanner()
        );
        // Find methods annotated with @Subscribe
        Set<Method> annotatedMethods = reflections.getMethodsAnnotatedWith(Subscribe.class);

        // Collect unique classes containing these methods
        Set<Class<?>> subscriberClasses = new java.util.HashSet<>();
        for (Method method : annotatedMethods) {
            subscriberClasses.add(method.getDeclaringClass());
        }

        // Instantiate and register each subscriber class
        for (Class<?> subscriberClass : subscriberClasses) {
            try {
                Object subscriberInstance = subscriberClass.getDeclaredConstructor().newInstance();
                GameEventBus.register(subscriberInstance);
            } catch (Exception e) {
                System.err.println("Failed to instantiate subscriber: " + subscriberClass.getName());
            }
        }
    }
}