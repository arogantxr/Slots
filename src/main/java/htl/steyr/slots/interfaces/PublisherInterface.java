package htl.steyr.slots.interfaces;

public interface PublisherInterface {
    void addSubscriber(SubscriberInterface subscriber);

    void notifySubscribers(Object message);
}
