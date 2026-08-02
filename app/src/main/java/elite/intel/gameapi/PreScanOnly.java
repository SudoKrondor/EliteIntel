package elite.intel.gameapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a subscriber that belongs to the {@link JournalPreScanner} private bus and must never be
 * registered on the live {@link elite.intel.eventbus.GameEventBus}.
 *
 * <p>WHY this exists: {@link SubscriberRegistration} discovers live subscribers by scanning whole
 * packages for {@code @Subscribe} methods, and the pre-scan subscribers sit in one of those
 * packages. Nothing but this marker keeps them off the live bus, and on the live bus they are
 * actively harmful: they mirror the real subscribers' DB writes while deliberately doing less (no
 * network, no fuel arithmetic, no announcements), and they write synchronously while the real
 * subscribers hand off to a virtual thread — so the silent, narrower write lands first and the real
 * handler then reads its own conclusion back as the state it was about to compare against.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PreScanOnly {
}
