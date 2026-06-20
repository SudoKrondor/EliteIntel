package elite.intel.gameapi.journal.events;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.JsonObject;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.time.Duration;
import java.time.Instant;

public abstract class BaseEvent implements ToJsonConvertible, ToYamlConvertable {

    private static final Instant APP_START = Instant.now();

    public String timestamp;
    public String event;
    public Instant endOfLife;

    public BaseEvent(String timestamp,  Duration ttl, String eventName) {
        this.event = eventName;
        this.timestamp = timestamp;
        this.endOfLife = Instant.now().plus(ttl);
    }

    @JsonIgnore
    public boolean isReplay() {
        return Instant.parse(timestamp).isBefore(APP_START);
    }

    @JsonIgnore
    public boolean isExpired() {
        if (endOfLife == null) {
            return false;
        }
        Instant now = Instant.now();
        return now.isAfter(endOfLife);
    }


    public String getTimestamp() {
        return timestamp;
    }


    public Instant getEndOfLife() {
        return endOfLife;
    }

    public String getEvent() {
        return event;
    }


    public abstract String getEventType();

    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    public String toYaml() {
        this.timestamp = null;
        this.endOfLife = null;
        this.event = null;
        return YamlFactory.toYaml(this);
    }

    public abstract JsonObject toJsonObject();
}