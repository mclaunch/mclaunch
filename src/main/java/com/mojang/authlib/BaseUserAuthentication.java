package com.mojang.authlib;

import com.google.common.collect.Multimap;
import com.mojang.authlib.AuthenticationService;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.UserType;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.util.UUIDTypeAdapter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class BaseUserAuthentication
implements UserAuthentication {
    private static final Logger LOGGER = LogManager.getLogger();
    protected static final String STORAGE_KEY_PROFILE_NAME = "displayName";
    protected static final String STORAGE_KEY_PROFILE_ID = "uuid";
    protected static final String STORAGE_KEY_PROFILE_PROPERTIES = "profileProperties";
    protected static final String STORAGE_KEY_USER_NAME = "username";
    protected static final String STORAGE_KEY_USER_ID = "userid";
    protected static final String STORAGE_KEY_USER_PROPERTIES = "userProperties";
    private final AuthenticationService authenticationService;
    private final PropertyMap userProperties = new PropertyMap();
    private String userid;
    private String username;
    private String password;
    private GameProfile selectedProfile;
    private UserType userType;

    protected BaseUserAuthentication(AuthenticationService authenticationService) {
        Validate.notNull(authenticationService);
        this.authenticationService = authenticationService;
    }

    @Override
    public boolean canLogIn() {
        return !this.canPlayOnline() && StringUtils.isNotBlank(this.getUsername()) && StringUtils.isNotBlank(this.getPassword());
    }

    @Override
    public void logOut() {
        this.password = null;
        this.userid = null;
        this.setSelectedProfile(null);
        this.getModifiableUserProperties().clear();
        this.setUserType(null);
    }

    @Override
    public boolean isLoggedIn() {
        return this.getSelectedProfile() != null;
    }

    @Override
    public void setUsername(String username) {
        if (this.isLoggedIn() && this.canPlayOnline()) {
            throw new IllegalStateException("Cannot change username whilst logged in & online");
        }
        this.username = username;
    }

    @Override
    public void setPassword(String password) {
        if (this.isLoggedIn() && this.canPlayOnline() && StringUtils.isNotBlank(password)) {
            throw new IllegalStateException("Cannot set password whilst logged in & online");
        }
        this.password = password;
    }

    protected String getUsername() {
        return this.username;
    }

    protected String getPassword() {
        return this.password;
    }

    @Override
    public void loadFromStorage(Map<String, Object> credentials) {
        this.logOut();
        this.setUsername(String.valueOf(credentials.get("username")));
        if (credentials.containsKey("userid")) {
            this.userid = String.valueOf(credentials.get("userid"));
        } else {
            this.userid = this.username;
        }
        if (credentials.containsKey("userProperties")) {
            try {
                final List<Map<String, String>> list = (List<Map<String, String>>) credentials.get("userProperties");
                for (final Map<String, String> propertyMap : list) {
                    final String name = propertyMap.get("name");
                    final String value = propertyMap.get("value");
                    final String signature = propertyMap.get("signature");
                    if (signature == null) {
                        this.getModifiableUserProperties().put(name, new Property(name, value));
                    } else {
                        this.getModifiableUserProperties().put(name, new Property(name, value, signature));
                    }
                }
            } catch (Throwable t) {
                BaseUserAuthentication.LOGGER.warn("Couldn't deserialize user properties", t);
            }
        }
        if (credentials.containsKey("displayName") && credentials.containsKey("uuid")) {
            final GameProfile profile = new GameProfile(
                    UUIDTypeAdapter.fromString(String.valueOf(credentials.get("uuid"))),
                    String.valueOf(credentials.get("displayName")));
            if (credentials.containsKey("profileProperties")) {
                try {
                    final List<Map<String, String>> list2 = (List<Map<String, String>>) credentials.get("profileProperties");
                    for (final Map<String, String> propertyMap2 : list2) {
                        final String name2 = propertyMap2.get("name");
                        final String value2 = propertyMap2.get("value");
                        final String signature2 = propertyMap2.get("signature");
                        if (signature2 == null) {
                            profile.getProperties().put(name2, new Property(name2, value2));
                        } else {
                            profile.getProperties().put(name2, new Property(name2, value2, signature2));
                        }
                    }
                } catch (Throwable t2) {
                    BaseUserAuthentication.LOGGER.warn("Couldn't deserialize profile properties", t2);
                }
            }
            this.setSelectedProfile(profile);
        }
    }

    @Override
    public Map<String, Object> saveForStorage() {
        GameProfile selectedProfile;
        HashMap<String, Object> result = new HashMap<String, Object>();
        if (this.getUsername() != null) {
            result.put(STORAGE_KEY_USER_NAME, this.getUsername());
        }
        if (this.getUserID() != null) {
            result.put(STORAGE_KEY_USER_ID, this.getUserID());
        } else if (this.getUsername() != null) {
            result.put(STORAGE_KEY_USER_NAME, this.getUsername());
        }
        if (!this.getUserProperties().isEmpty()) {
            ArrayList properties = new ArrayList();
            for (Property userProperty : this.getUserProperties().values()) {
                HashMap<String, String> property = new HashMap<String, String>();
                property.put("name", userProperty.getName());
                property.put("value", userProperty.getValue());
                property.put("signature", userProperty.getSignature());
                properties.add(property);
            }
            result.put(STORAGE_KEY_USER_PROPERTIES, properties);
        }
        if ((selectedProfile = this.getSelectedProfile()) != null) {
            result.put(STORAGE_KEY_PROFILE_NAME, selectedProfile.getName());
            result.put(STORAGE_KEY_PROFILE_ID, selectedProfile.getId());
            ArrayList properties = new ArrayList();
            for (Property profileProperty : selectedProfile.getProperties().values()) {
                HashMap<String, String> property = new HashMap<String, String>();
                property.put("name", profileProperty.getName());
                property.put("value", profileProperty.getValue());
                property.put("signature", profileProperty.getSignature());
                properties.add(property);
            }
            if (!properties.isEmpty()) {
                result.put(STORAGE_KEY_PROFILE_PROPERTIES, properties);
            }
        }
        return result;
    }

    protected void setSelectedProfile(GameProfile selectedProfile) {
        this.selectedProfile = selectedProfile;
    }

    @Override
    public GameProfile getSelectedProfile() {
        return this.selectedProfile;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(this.getClass().getSimpleName());
        result.append("{");
        if (this.isLoggedIn()) {
            result.append("Logged in as ");
            result.append(this.getUsername());
            if (this.getSelectedProfile() != null) {
                result.append(" / ");
                result.append(this.getSelectedProfile());
                result.append(" - ");
                if (this.canPlayOnline()) {
                    result.append("Online");
                } else {
                    result.append("Offline");
                }
            }
        } else {
            result.append("Not logged in");
        }
        result.append("}");
        return result.toString();
    }

    public AuthenticationService getAuthenticationService() {
        return this.authenticationService;
    }

    @Override
    public String getUserID() {
        return this.userid;
    }

    @Override
    public PropertyMap getUserProperties() {
        if (this.isLoggedIn()) {
            PropertyMap result = new PropertyMap();
            result.putAll(this.getModifiableUserProperties());
            return result;
        }
        return new PropertyMap();
    }

    protected PropertyMap getModifiableUserProperties() {
        return this.userProperties;
    }

    @Override
    public UserType getUserType() {
        if (this.isLoggedIn()) {
            return this.userType == null ? UserType.LEGACY : this.userType;
        }
        return null;
    }

    protected void setUserType(UserType userType) {
        this.userType = userType;
    }

    protected void setUserid(String userid) {
        this.userid = userid;
    }
}

