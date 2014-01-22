package com.alwaysallthetime.messagebeast;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.alwaysallthetime.adnlib.data.Channel;
import com.alwaysallthetime.adnlib.data.Configuration;
import com.alwaysallthetime.adnlib.data.Token;
import com.alwaysallthetime.adnlib.data.User;
import com.alwaysallthetime.adnlib.gson.AppDotNetGson;
import com.alwaysallthetime.messagebeast.model.FullSyncState;
import com.google.gson.Gson;

import java.util.Date;

public class ADNSharedPreferences {
    private static final String ACCESS_TOKEN = "accessToken";
    private static final String TOKEN_OBJECT = "tokenObject";
    private static final String CONFIGURATION_OBJECT = "configurationObject";
    private static final String CONFIGURATION_DATE = "configurationDate";
    private static final String USER_OBJECT = "user";
    private static final String CHANNEL_OBJECT = "channel";
    private static final String ACTION_CHANNEL_OBJECT = "actionChannel";
    private static final String FULL_SYNC_STATE = "fullSyncState";

    private static SharedPreferences sPrefs;
    private static Gson gson;

    static {
        sPrefs = PreferenceManager.getDefaultSharedPreferences(ADNApplication.getContext());
        gson = AppDotNetGson.getPersistenceInstance();
    }

    public static FullSyncState getFullSyncState(String channelId) {
        return FullSyncState.fromOrdinal(sPrefs.getInt(FULL_SYNC_STATE + "_" + channelId, 0));
    }

    public static void setFullSyncState(String channelId, FullSyncState state) {
        SharedPreferences.Editor edit = sPrefs.edit();
        edit.putInt(FULL_SYNC_STATE + "_" + channelId, state.ordinal());
        edit.commit();
    }

    public static boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    public static String getAccessToken() {
        return sPrefs.getString(ACCESS_TOKEN, null);
    }

    public static Token getToken() {
        final String tokenJson = sPrefs.getString(TOKEN_OBJECT, null);
        return gson.fromJson(tokenJson, Token.class);
    }

    public static void saveCredentials(String accessToken, Token token) {
        final SharedPreferences.Editor editor = sPrefs.edit();
        final String tokenJson = gson.toJson(token);
        editor.putString(TOKEN_OBJECT, tokenJson);
        editor.putString(ACCESS_TOKEN, accessToken);
        editor.commit();
    }

    public static void clearCredentials() {
        final SharedPreferences.Editor editor = sPrefs.edit();
        editor.remove(TOKEN_OBJECT);
        editor.remove(ACCESS_TOKEN);
        editor.commit();
    }

    public static Configuration getConfiguration() {
        final String configurationJson = sPrefs.getString(CONFIGURATION_OBJECT, null);
        if(configurationJson != null) {
            return gson.fromJson(configurationJson, Configuration.class);
        }
        return null;
    }

    public static Date getConfigurationSaveDate() {
        final long configTime = sPrefs.getLong(CONFIGURATION_DATE, 0);
        if(configTime != 0) {
            return new Date(configTime);
        }
        return null;
    }

    public static void saveConfiguration(Configuration configuration) {
        final SharedPreferences.Editor editor = sPrefs.edit();
        final String configJson = gson.toJson(configuration);
        editor.putString(CONFIGURATION_OBJECT, configJson);
        editor.putLong(CONFIGURATION_DATE, new Date().getTime());
        editor.commit();
    }

    public static User getUser(String userId) {
        final String json = sPrefs.getString(USER_OBJECT + "_" + userId, null);
        if(json != null) {
            return gson.fromJson(json, User.class);
        }
        return null;
    }

    public static void saveUser(User user) {
        final SharedPreferences.Editor editor = sPrefs.edit();
        final String json = gson.toJson(user);
        editor.putString(USER_OBJECT + "_" + user.getId(), json);
        editor.commit();
    }

    public static Channel getPrivateChannel(String channelType) {
        final String json = sPrefs.getString(CHANNEL_OBJECT + "_" + channelType, null);
        if(json != null) {
            return gson.fromJson(json, Channel.class);
        }
        return null;
    }

    public static void savePrivateChannel(Channel channel) {
        final SharedPreferences.Editor editor = sPrefs.edit();
        final String json = gson.toJson(channel);
        editor.putString(CHANNEL_OBJECT + "_" + channel.getType(), json);
        editor.commit();
    }

    public static void deletePrivateChannel(Channel channel) {
        final SharedPreferences.Editor editor = sPrefs.edit();
        editor.remove(CHANNEL_OBJECT + "_" + channel.getType());
        editor.commit();
    }

    public static Channel getActionChannel(String actionType, String targetChannelId) {
        final String json = sPrefs.getString(ACTION_CHANNEL_OBJECT + "_" + actionType + "_" + targetChannelId, null);
        if(json != null) {
            return gson.fromJson(json, Channel.class);
        }
        return null;
    }

    public static void saveActionChannel(Channel actionChannel, String actionType, String targetChannelId) {
        final SharedPreferences.Editor editor = sPrefs.edit();
        final String json = gson.toJson(actionChannel);
        editor.putString(ACTION_CHANNEL_OBJECT + "_" + actionType + "_" + targetChannelId, json);
        editor.commit();
    }

    public static void deleteActionChannel(Channel actionChannel, String actionType, String targetChannelId) {
        final SharedPreferences.Editor editor = sPrefs.edit();
        editor.remove(ACTION_CHANNEL_OBJECT + "_" + actionType + "_" + targetChannelId);
        editor.commit();
    }
}
