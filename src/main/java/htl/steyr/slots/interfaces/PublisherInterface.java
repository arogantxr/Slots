package htl.steyr.slots.interfaces;

/**
 * Defines the publisher side of the observer pattern used throughout the server layer.
 *
 * <p>A publisher maintains a list of {@link SubscriberInterface} instances and
 * broadcasts events to all of them whenever something noteworthy happens.</p>
 */
public interface PublisherInterface {

    /**
     * Registers a subscriber to receive future events from this publisher.
     *
     * @param subscriber the subscriber to add
     */
    void addSubscriber(SubscriberInterface subscriber);

    /**
     * Notifies all registered subscribers with the given message payload.
     *
     * @param message the payload to deliver (typically a protocol string)
     */
    void notifySubscribers(Object message);
}
