package com.mojang.launcher.versions;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.ReleaseTypeFactory;
import java.io.IOException;

public class ReleaseTypeAdapterFactory<T extends ReleaseType>
extends TypeAdapter<T> {
    private final ReleaseTypeFactory<T> factory;

    public ReleaseTypeAdapterFactory(ReleaseTypeFactory<T> factory) {
        this.factory = factory;
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        out.value(value.getName());
    }

    @Override
    public T read(JsonReader in) throws IOException {
        return this.factory.getTypeByName(in.nextString());
    }
}

