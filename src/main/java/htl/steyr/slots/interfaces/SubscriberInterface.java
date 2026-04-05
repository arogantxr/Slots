package htl.steyr.slots.interfaces;

/**
 * Defines the subscriber side of the observer pattern used throughout the server layer.
 *
 * <p>Implementations react to {@link Event} objects published by a
 * {@link PublisherInterface}.</p>
 */
public interface SubscriberInterface {

    /**
     * Called whenever the publisher dispatches an event.
     *
     * @param event the event carrying the source and the message payload
     */
    void notify(Event event);
}
