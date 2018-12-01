package baritone.gradle.util;

import java.lang.reflect.Field;
import java.util.Objects;

public class ReobfWrapper {

  private final Object instance;
  private final Class<?> type;

  public ReobfWrapper(Object instance) {
    this.instance = instance;
    Objects.requireNonNull(instance);
    this.type = instance.getClass();
  }

  public String getName() {
    try {
      Field nameField = type.getDeclaredField("name");
      nameField.setAccessible(true);
      return (String)nameField.get(this.instance);
    } catch (ReflectiveOperationException ex) {
      throw new Error(ex);
    }
  }

  public MappingType getMappingType() {
    try {
      Field enumField = type.getDeclaredField("mappingType");
      enumField.setAccessible(true);
      Enum<?> meme = (Enum<?>) enumField.get(this.instance);
      MappingType mappingType = MappingType.values()[meme.ordinal()];
      if (!meme.name().equals(mappingType.name()))
        throw new IllegalStateException("ForgeGradle ReobfMappingType is not equivalent to MappingType (version error?)");
      return mappingType;
    } catch (ReflectiveOperationException ex) {
      throw new Error(ex);
    }
  }
}