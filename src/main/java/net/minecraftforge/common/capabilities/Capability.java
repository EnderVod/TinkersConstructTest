package net.minecraftforge.common.capabilities;

import net.neoforged.neoforge.common.util.LazyOptional;

/** Lightweight identity token replacing the removed legacy Forge Capability class. */
public class Capability<T> {
  public Capability() {}

  public <R> LazyOptional<R> orEmpty(Capability<R> toCheck, LazyOptional<?> instance) {
    return toCheck == this ? instance.cast() : LazyOptional.empty();
  }
}
