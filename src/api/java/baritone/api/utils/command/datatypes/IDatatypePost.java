package baritone.api.utils.command.datatypes;

public interface IDatatypePost<T, O> extends IDatatype {
    T apply(O original);
}
