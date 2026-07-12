package elite.intel.eventbus;

import com.google.common.eventbus.Subscribe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameEventBusTest {

    private final ReentrantSubscriber subscriber = new ReentrantSubscriber();

    @BeforeEach
    void register() {
        GameEventBus.register(subscriber);
    }

    @AfterEach
    void unregister() {
        GameEventBus.unregister(subscriber);
    }

    @Test
    void afterDispatchRunsAfterAReentrantEventHasReachedItsSubscriber() {
        GameEventBus.publish(new OuterEvent());

        assertEquals(List.of("outer-before", "outer-after", "inner", "after-dispatch"), subscriber.order);
    }

    private static final class ReentrantSubscriber {
        private final List<String> order = new ArrayList<>();

        @Subscribe
        public void onOuter(OuterEvent event) {
            order.add("outer-before");
            GameEventBus.publish(new InnerEvent());
            GameEventBus.afterCurrentDispatch(() -> order.add("after-dispatch"));
            order.add("outer-after");
        }

        @Subscribe
        public void onInner(InnerEvent event) {
            order.add("inner");
        }
    }

    private record OuterEvent() {
    }

    private record InnerEvent() {
    }
}
