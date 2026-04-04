package htl.steyr.slots.interfaces;

public class Event {
    private final PublisherInterface source;
    private final Object message;

    public Event(PublisherInterface source, Object message) {
        this.source = source;
        this.message = message;
    }

    public PublisherInterface source() {
        return source;
    }

    public Object message() {
        return message;
    }
}
