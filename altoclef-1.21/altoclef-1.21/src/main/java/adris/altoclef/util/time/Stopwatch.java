package adris.altoclef.util.time;

public class Stopwatch {

    boolean _running = false;
    private double _startTime = 0;

    private static double currentTime() {
        return (double) System.currentTimeMillis() / 1000.0;
    }

    public void begin() {
        _startTime = currentTime();
        _running = true;
    }

    public double time() {
        if (!_running) return 0;
        return currentTime() - _startTime;
    }
}
