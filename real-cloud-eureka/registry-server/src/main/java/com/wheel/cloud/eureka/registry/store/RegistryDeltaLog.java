package com.wheel.cloud.eureka.registry.store;

import com.wheel.cloud.eureka.registry.model.DeltaEventType;
import com.wheel.cloud.eureka.registry.model.DeltaSyncResponse;
import com.wheel.cloud.eureka.registry.model.InstanceInfo;
import com.wheel.cloud.eureka.registry.model.RegistryDeltaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class RegistryDeltaLog {

    private static final Logger log = LoggerFactory.getLogger(RegistryDeltaLog.class);

    private final AtomicLong versionGenerator = new AtomicLong(0L);

    private final CopyOnWriteArrayList<RegistryDeltaEvent> events = new CopyOnWriteArrayList<>();

    public RegistryDeltaEvent append(DeltaEventType eventType, InstanceInfo instanceInfo) {
        long version = versionGenerator.incrementAndGet();
        RegistryDeltaEvent event = new RegistryDeltaEvent(version, eventType, instanceInfo);
        events.add(event);
        trimIfNecessary();
        log.info("appended delta event: version={}, eventType={}, serviceName={}, instanceId={}",
                version, eventType, instanceInfo.getServiceName(), instanceInfo.getInstanceId());
        return event;
    }

    public DeltaSyncResponse syncFrom(long lastSeenVersion) {
        long currentVersion = versionGenerator.get();
        if (lastSeenVersion >= currentVersion) {
            return new DeltaSyncResponse(lastSeenVersion, currentVersion, false, Collections.emptyList());
        }
        if (events.isEmpty()) {
            return new DeltaSyncResponse(lastSeenVersion, currentVersion, true, Collections.emptyList());
        }

        long earliestAvailableVersion = events.get(0).getVersion();
        if (lastSeenVersion + 1 < earliestAvailableVersion) {
            log.warn("delta sync requires full snapshot: lastSeenVersion={}, earliestAvailableVersion={}",
                    lastSeenVersion, earliestAvailableVersion);
            return new DeltaSyncResponse(lastSeenVersion, currentVersion, true, Collections.emptyList());
        }

        List<RegistryDeltaEvent> deltaEvents = events.stream()
                .filter(event -> event.getVersion() > lastSeenVersion)
                .collect(Collectors.toList());
        return new DeltaSyncResponse(lastSeenVersion, currentVersion, false, new ArrayList<>(deltaEvents));
    }

    private void trimIfNecessary() {
        int maxSize = 1000;
        while (events.size() > maxSize) {
            events.remove(0);
        }
    }
}
