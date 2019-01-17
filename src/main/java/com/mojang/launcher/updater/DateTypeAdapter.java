package com.mojang.launcher.updater;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTypeAdapter
implements JsonSerializer<Date>,
JsonDeserializer<Date> {
    private final DateFormat enUsFormat = DateFormat.getDateTimeInstance(2, 2, Locale.US);
    private final DateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    @Override
    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (!(json instanceof JsonPrimitive)) {
            throw new JsonParseException("The date should be a string value");
        }
        Date date = this.deserializeToDate(json.getAsString());
        if (typeOfT == Date.class) {
            return date;
        }
        throw new IllegalArgumentException(this.getClass() + " cannot deserialize to " + typeOfT);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
        DateFormat dateFormat = this.enUsFormat;
        synchronized (dateFormat) {
            return new JsonPrimitive(this.serializeToString(src));
        }
    }

    public Date deserializeToDate(String string) {
        DateFormat dateFormat = this.enUsFormat;
        synchronized (dateFormat) {
            try {
                return this.enUsFormat.parse(string);
            }
            catch (ParseException parseException) {
                try {
                    return this.iso8601Format.parse(string);
                }
                catch (ParseException parseException2) {
                    try {
                        String cleaned = string.replace("Z", "+00:00");
                        cleaned = cleaned.substring(0, 22) + cleaned.substring(23);
                        return this.iso8601Format.parse(cleaned);
                    }
                    catch (Exception e) {
                        throw new JsonSyntaxException("Invalid date: " + string, e);
                    }
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public String serializeToString(Date date) {
        DateFormat dateFormat = this.enUsFormat;
        synchronized (dateFormat) {
            String result = this.iso8601Format.format(date);
            return result.substring(0, 22) + ":" + result.substring(22);
        }
    }
}

