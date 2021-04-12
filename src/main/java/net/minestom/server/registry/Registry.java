package net.minestom.server.registry;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface Registry<T extends Keyed> {

    // Access

    @NotNull
    static <T extends Keyed> T get(@NotNull Registry<T> registry, @NotNull String id) {
        return registry.get(Key.key(id));
    }

    @NotNull
    static <T extends Keyed> T get(@NotNull Registry<T> registry, @NotNull NamespaceID id) {
        return registry.get(id);
    }

    @NotNull
    static <T extends Keyed> T get(@NotNull Registry<T> registry, @NotNull Key key) {
        return registry.get(key);
    }

    @Nullable
    static <T extends Keyed> T get(@NotNull IdCrossRegistry<T> registry, int id) {
        return registry.get((short) id);
    }

    static <T extends Keyed> boolean register(@NotNull Writable<T> registry, @NotNull T value) {
        return registry.register(value);
    }

    // Implementation

    T get(@NotNull Key id);

    List<T> values();

    interface Writable<T extends Keyed> extends Registry<T> {
        boolean register(@NotNull T value);
    }
}
