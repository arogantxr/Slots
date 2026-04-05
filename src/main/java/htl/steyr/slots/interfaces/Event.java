package htl.steyr.slots.interfaces;

/**
 * Immutable event object passed from a {@link PublisherInterface} to all
 * registered {@link SubscriberInterface} instances.
 *
 * <p>An event bundles the originating publisher together with an arbitrary
 * message payload so that subscribers have full context when they are notified.</p>
 */
public class Event {

    private final PublisherInterface source;
    private final Object message;

    /**
     * Creates a new event.
     *
     * @param source  the publisher that raised this event
     * @param message the payload carried by the event (typically a {@link String})
     */
    public Event(PublisherInterface source, Object message) {
        this.source = source;
        this.message = message;
    }

    /**
     * Returns the publisher that raised this event.
     *
     * @return the event source
     */
    public PublisherInterface source() {
        return source;
    }

    /**
     * Returns the message payload of this event.
     *
     * @return the payload object
     */
    public Object message() {
        return message;
    }
}
