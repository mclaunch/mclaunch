package net.minecraft.launcher.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.authlib.Agent;
import com.mojang.authlib.AuthenticationService;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import java.lang.reflect.Type;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.launcher.Launcher;

public class AuthenticationDatabase {
    public static final String DEMO_UUID_PREFIX = "demo-";
    private final Map<String, UserAuthentication> authById;
    private final AuthenticationService authenticationService;

    public AuthenticationDatabase(AuthenticationService authenticationService) {
        this(new HashMap<String, UserAuthentication>(), authenticationService);
    }

    public AuthenticationDatabase(Map<String, UserAuthentication> authById, AuthenticationService authenticationService) {
        this.authById = authById;
        this.authenticationService = authenticationService;
    }

    public UserAuthentication getByName(String name) {
        if (name == null) {
            return null;
        }
        for (Map.Entry<String, UserAuthentication> entry : this.authById.entrySet()) {
            GameProfile profile = entry.getValue().getSelectedProfile();
            if (profile != null && profile.getName().equals(name)) {
                return entry.getValue();
            }
            if (profile != null || !AuthenticationDatabase.getUserFromDemoUUID(entry.getKey()).equals(name)) continue;
            return entry.getValue();
        }
        return null;
    }

    public UserAuthentication getByUUID(String uuid) {
        return this.authById.get(uuid);
    }

    public Collection<String> getKnownNames() {
        ArrayList<String> names = new ArrayList<String>();
        for (Map.Entry<String, UserAuthentication> entry : this.authById.entrySet()) {
            GameProfile profile = entry.getValue().getSelectedProfile();
            if (profile != null) {
                names.add(profile.getName());
                continue;
            }
            names.add(AuthenticationDatabase.getUserFromDemoUUID(entry.getKey()));
        }
        return names;
    }

    public void register(String uuid, UserAuthentication authentication) {
        this.authById.put(uuid, authentication);
    }

    public Set<String> getknownUUIDs() {
        return this.authById.keySet();
    }

    public void removeUUID(String uuid) {
        this.authById.remove(uuid);
    }

    public AuthenticationService getAuthenticationService() {
        return this.authenticationService;
    }

    public static String getUserFromDemoUUID(String uuid) {
        if (uuid.startsWith(DEMO_UUID_PREFIX) && uuid.length() > DEMO_UUID_PREFIX.length()) {
            return "Demo User " + uuid.substring(DEMO_UUID_PREFIX.length());
        }
        return "Demo User";
    }

    public static class Serializer
    implements JsonDeserializer<AuthenticationDatabase>,
    JsonSerializer<AuthenticationDatabase> {
        private final Launcher launcher;

        public Serializer(Launcher launcher) {
            this.launcher = launcher;
        }

        @Override
        public AuthenticationDatabase deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            HashMap<String, UserAuthentication> services = new HashMap<String, UserAuthentication>();
            Map<String, Map<String, Object>> credentials = this.deserializeCredentials((JsonObject)json, context);
            YggdrasilAuthenticationService authService = new YggdrasilAuthenticationService(this.launcher.getLauncher().getProxy(), this.launcher.getClientToken().toString());
            for (Map.Entry<String, Map<String, Object>> entry : credentials.entrySet()) {
                UserAuthentication auth = authService.createUserAuthentication(this.launcher.getLauncher().getAgent());
                auth.loadFromStorage(entry.getValue());
                services.put(entry.getKey(), auth);
            }
            return new AuthenticationDatabase(services, authService);
        }

        protected Map<String, Map<String, Object>> deserializeCredentials(JsonObject json, JsonDeserializationContext context) {
            LinkedHashMap<String, Map<String, Object>> result = new LinkedHashMap<String, Map<String, Object>>();
            for (Map.Entry<String, JsonElement> authEntry : json.entrySet()) {
                LinkedHashMap<String, Object> credentials = new LinkedHashMap<String, Object>();
                for (Map.Entry<String, JsonElement> credentialsEntry : ((JsonObject)authEntry.getValue()).entrySet()) {
                    credentials.put(credentialsEntry.getKey(), this.deserializeCredential(credentialsEntry.getValue()));
                }
                result.put(authEntry.getKey(), credentials);
            }
            return result;
        }

        private Object deserializeCredential(JsonElement element) {
            if (element instanceof JsonObject) {
                LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
                for (Map.Entry<String, JsonElement> entry : ((JsonObject)element).entrySet()) {
                    result.put(entry.getKey(), this.deserializeCredential(entry.getValue()));
                }
                return result;
            }
            if (element instanceof JsonArray) {
                ArrayList<Object> result = new ArrayList<Object>();
                for (JsonElement entry : (JsonArray)element) {
                    result.add(this.deserializeCredential(entry));
                }
                return result;
            }
            return element.getAsString();
        }

        @Override
        public JsonElement serialize(final AuthenticationDatabase src, final Type typeOfSrc, final JsonSerializationContext context) {
            final Map<String, UserAuthentication> services = src.authById;
            final Map<String, Map<String, Object>> credentials = new HashMap<String, Map<String, Object>>();
            for (final Map.Entry<String, UserAuthentication> entry : services.entrySet()) {
                credentials.put(entry.getKey(), entry.getValue().saveForStorage());
            }
            return context.serialize(credentials);
        }
    }

}

