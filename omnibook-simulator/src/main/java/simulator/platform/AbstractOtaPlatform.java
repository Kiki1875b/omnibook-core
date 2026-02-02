package simulator.platform;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for OTA platform simulators.
 * Each platform maintains its own isolated in-memory reservation state.
 * Platforms trust only their own state and emit events optimistically.
 */
public abstract class AbstractOtaPlatform implements OtaPlatform {

    protected final Map<String, Object> reservations = new ConcurrentHashMap<>();

    protected String generateId(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public boolean hasReservation(String reservationId) {
        return reservations.containsKey(reservationId);
    }

    protected void storeReservation(String id, Object payload) {
        reservations.put(id, payload);
    }

    protected Object removeReservation(String id) {
        return reservations.remove(id);
    }
}
