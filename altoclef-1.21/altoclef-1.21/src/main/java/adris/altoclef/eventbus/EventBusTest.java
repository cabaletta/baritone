package adris.altoclef.eventbus;

public class EventBusTest {

    public static void test() {
        Subscription<TestEvent> a = EventBus.subscribe(TestEvent.class, evt -> System.out.println("A: " + evt.val));
        EventBus.publish(new TestEvent(1));
        Subscription<TestEvent> b = EventBus.subscribe(TestEvent.class, evt -> System.out.println("B: " + evt.val));
        EventBus.publish(new TestEvent(2));
        EventBus.unsubscribe(a);
        EventBus.publish(new TestEvent(3));
        EventBus.unsubscribe(b);
        EventBus.publish(new TestEvent(4));
    }

    static class TestEvent {
        public int val;

        public TestEvent(int val) {
            this.val = val;
        }
    }
}
