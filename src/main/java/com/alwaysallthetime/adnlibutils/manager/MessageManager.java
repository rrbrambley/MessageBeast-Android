package com.alwaysallthetime.adnlibutils.manager;

import android.util.Log;

import com.alwaysallthetime.adnlib.AppDotNetClient;
import com.alwaysallthetime.adnlib.QueryParameters;
import com.alwaysallthetime.adnlib.data.Message;
import com.alwaysallthetime.adnlib.data.MessageList;
import com.alwaysallthetime.adnlib.response.MessageListResponseHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by brambley on 10/10/13.
 */
public class MessageManager {

    private static final String TAG = "MessageManager";

    /*
     * Singleton stuff
     */
    private static MessageManager sInstance;

    public static MessageManager getInstance(AppDotNetClient client) {
        if(sInstance == null) {
            sInstance = new MessageManager(client);
        }
        return sInstance;
    }

    /*
     * public data structures
     */
    public interface MessageManagerResponseHandler {
        public void onSuccess(final MessageList responseData, final boolean appended);
        public void onError(Exception exception);
    }

    /*
     * private data structures
     */

    private class MinMaxPair {
        public String minId;
        public String maxId;

        public MinMaxPair() {}
    }

    private AppDotNetClient mClient;

    private HashMap<String, Map<String, Message>> mMessages;
    private HashMap<String, QueryParameters> mParameters;
    private HashMap<String, MinMaxPair> mMinMaxPairs;

    public MessageManager(AppDotNetClient client) {
        mClient = client;

        mMessages = new HashMap<String, Map<String, Message>>();
        mMinMaxPairs = new HashMap<String, MinMaxPair>();
        mParameters = new HashMap<String, QueryParameters>();
    }

    public Map<String, Message> getMessageMap(String channelId) {
        return mMessages.get(channelId);
    }

    public List<Message> getMessageList(String channelId) {
        Map<String, Message> messageMap = mMessages.get(channelId);
        if(messageMap == null) {
            return null;
        }
        Message[] messages = messageMap.values().toArray(new Message[0]);
        return Arrays.asList(messages);
    }

    public void setParameters(String channelId, QueryParameters parameters) {
        mParameters.put(channelId, parameters);
    }

    private MinMaxPair getMinMaxPair(String channelId) {
        MinMaxPair minMaxPair = mMinMaxPairs.get(channelId);
        if(minMaxPair == null) {
            minMaxPair = new MinMaxPair();
            mMinMaxPairs.put(channelId, minMaxPair);
        }
        return minMaxPair;
    }

    public void retrieveMessages(String channelId, MessageManagerResponseHandler listener) {
        MinMaxPair minMaxPair = getMinMaxPair(channelId);
        retrieveMessages(channelId, minMaxPair.maxId, minMaxPair.minId, listener);
    }

    public void retrieveNewestMessages(String channelId, MessageManagerResponseHandler listener) {
        retrieveMessages(channelId, getMinMaxPair(channelId).maxId, null, listener);
    }

    public void retrieveMoreMessages(String channelId, MessageManagerResponseHandler listener) {
        retrieveMessages(channelId, null, getMinMaxPair(channelId).minId, listener);
    }

    private void retrieveMessages(final String channelId, final String sinceId, final String beforeId, final MessageManagerResponseHandler listener) {
        QueryParameters params = (QueryParameters) mParameters.get(channelId).clone();
        params.put("since_id", sinceId);
        params.put("before_id", beforeId);
        mClient.retrieveMessagesInChannel(channelId, params, new MessageListResponseHandler() {
            @Override
            public void onSuccess(final MessageList responseData) {
                MinMaxPair minMaxPair = getMinMaxPair(channelId);
                boolean appended = true;
                if(beforeId != null && sinceId == null) {
                    String newMinId = getMinId();
                    if(newMinId != null) {
                        minMaxPair.minId = newMinId;
                    }
                } else if(beforeId == null && sinceId != null) {
                    appended = false;
                    String newMaxId = getMaxId();
                    if(newMaxId != null) {
                        minMaxPair.maxId = newMaxId;
                    }
                } else if(beforeId == null && sinceId == null) {
                    minMaxPair.minId = getMinId();
                    minMaxPair.maxId = getMaxId();
                }

                Map<String, Message> channelMessages = mMessages.get(channelId);
                if(channelMessages == null) {
                    channelMessages = new LinkedHashMap<String, Message>(responseData.size());
                    mMessages.put(channelId, channelMessages);
                }
                for(Message m : responseData) {
                    channelMessages.put(m.getId(), m);
                }

                if(listener != null) {
                    listener.onSuccess(responseData, appended);
                }
            }

            @Override
            public void onError(Exception error) {
                Log.d(TAG, error.getMessage(), error);

                if(listener != null) {
                    listener.onError(error);
                }
            }
        });
    }
}
