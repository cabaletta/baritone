package adris.altoclef.util.time;

public abstract class BaseTimer {
    private double _prevTime = 0;
    private double _interval;

    public BaseTimer(double intervalSeconds) {
        _interval = intervalSeconds;
    }

    public double getDuration() {
        return currentTime() - _prevTime;
    }

    public void setInterval(double interval) {
        _interval = interval;
    }

    public boolean elapsed() {
        return getDuration() > _interval;
    }

    public void reset() {
        _prevTime = currentTime();
    }

    public void forceElapse() {
        _prevTime = 0;
    }

    protected abstract double currentTime();

    protected void setPrevTimeForce(double toSet) {
        _prevTime = toSet;
    }

    protected double getPrevTime() {
        return _prevTime;
    }

}
