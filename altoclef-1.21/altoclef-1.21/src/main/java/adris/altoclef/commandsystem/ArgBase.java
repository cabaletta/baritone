package adris.altoclef.commandsystem;

import java.lang.reflect.ParameterizedType;

public abstract class ArgBase {
    protected int _minArgCountToUseDefault;
    protected boolean _hasDefault;

    protected <V> V getConverted(Class<V> vType, Object ob) {
        try {
            //noinspection unchecked
            return (V) ob;
        } catch (Exception e) {
            throw new IllegalArgumentException("Tried to convert the following object to type {typeof(V)} and failed: {ob}. This is probably an internal problem, contact the dev!");
            //return default(T);
        }
    }

    //public abstract Object ParseUnit ( String unit, String[] unitPlusRemainder );

    @SuppressWarnings("unchecked")
    public <V> V parseUnit(String unit, String[] unitPlusRemainder) throws CommandException {
        // Fuck java
        Class<V> vType = (Class<V>)
                ((ParameterizedType) getClass()
                        .getGenericSuperclass())
                        .getActualTypeArguments()[0];

        return getConverted(vType, parseUnit(unit, unitPlusRemainder));
    }

    public abstract <V> V getDefault(Class<V> vType);

    public abstract String getHelpRepresentation();

    public int getMinArgCountToUseDefault() {
        return _minArgCountToUseDefault;
    }

    public boolean hasDefault() {
        return _hasDefault;
    }

    public boolean isArray() {
        return false;
    }

    public boolean isArbitrarilyLong() {
        return false;
    }

}
