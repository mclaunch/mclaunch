package com.mojang.launcher.updater;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LowerCaseEnumTypeAdapterFactory
implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
        final Class<T> rawType = (Class<T>) type.getRawType();
        if (!rawType.isEnum()) {
            return null;
        }
        final Map<String, T> lowercaseToConstant = new HashMap<String, T>();
        for (final T constant : rawType.getEnumConstants()) {
            lowercaseToConstant.put(this.toLowercase(constant), constant);
        }
        return new TypeAdapter<T>() {
            @Override
            public void write(final JsonWriter out, final T value) throws IOException {
                if (value == null) {
                    out.nullValue();
                } else {
                    out.value(LowerCaseEnumTypeAdapterFactory.this.toLowercase(value));
                }
            }

            @Override
            public T read(final JsonReader reader) throws IOException {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                    return null;
                }
                return lowercaseToConstant.get(reader.nextString());
            }
        };
    }

    private String toLowercase(Object o) {
        return o.toString().toLowerCase(Locale.US);
    }

}

