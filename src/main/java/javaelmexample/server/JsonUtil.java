package javaelmexample.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonUtil {
    
    private static final ThreadLocal<Gson> compactGson = ThreadLocal.withInitial(() -> new Gson());
    
    private static final ThreadLocal<Gson> prettyGson = ThreadLocal.withInitial(() -> new GsonBuilder().setPrettyPrinting().create());
    
    public static <T> T fromJson(String json, Class<T> clss) {
        return compactGson.get().fromJson(json, clss);
    }
    
    public static <T> String toJson(T object) {
        return toCompactJson(object);
    }
    
    public static <T> String toCompactJson(T object) {
        return compactGson.get().toJson(object);
    }
    
    public static <T> String toPrettyJson(T object) {
        return prettyGson.get().toJson(object);
    }
    
}
