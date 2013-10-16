package com.alwaysallthetime.adnlibutils.manager;

import android.content.Context;
import android.util.Log;

import com.alwaysallthetime.adnlib.AppDotNetClient;
import com.alwaysallthetime.adnlib.QueryParameters;
import com.alwaysallthetime.adnlib.data.Message;
import com.alwaysallthetime.adnlib.data.MessageList;
import com.alwaysallthetime.adnlib.response.MessageListResponseHandler;
import com.alwaysallthetime.adnlib.response.MessageResponseHandler;
import com.alwaysallthetime.adnlibutils.MessagePlus;
import com.alwaysallthetime.adnlibutils.db.ADNDatabase;
import com.alwaysallthetime.adnlibutils.db.OrderedMessageBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MessageManager {

    private static final String TAG = "ADNLibUtils_MessageManager";

    /*
     * Singleton stuff
     */
    private static MessageManager sInstance;

    public static MessageManager getInstance(Context context, AppDotNetClient client) {
        if(sInstance == null) {
            sInstance = new MessageManager(context, client);
        }
        return sInstance;
    }

    /*
     * public data structures
     */
    public interface MessageManagerResponseHandler {
        public void onSuccess(final List<MessagePlus> responseData, final boolean appended);
        public void onError(Exception exception);
    }

    public interface MessageRefreshResponseHandler {
        public void onSuccess(final MessagePlus responseData);
        public void onError(Exception exception);
    }

    /**
     * A MessageDisplayDateAdapter can be used to return a date for which a Message should be
     * associated. This is most typically used when Message.getCreatedAt() should not be used
     * for sort order.
     */
    public interface MessageDisplayDateAdapter {
        public Date getDisplayDate(Message message);
    }

    private Context mContext;
    private AppDotNetClient mClient;
    private MessageDisplayDateAdapter mDateAdapter;
    private boolean mIsDatabaseInsertionEnabled;

    private HashMap<String, LinkedHashMap<String, MessagePlus>> mMessages;
    private HashMap<String, QueryParameters> mParameters;
    private HashMap<String, MinMaxPair> mMinMaxPairs;

    public MessageManager(Context context, AppDotNetClient client) {
        mContext = context;
        mClient = client;

        mMessages = new HashMap<String, LinkedHashMap<String, MessagePlus>>();
        mMinMaxPairs = new HashMap<String, MinMaxPair>();
        mParameters = new HashMap<String, QueryParameters>();
    }

    /**
     * Load persisted messages that were previously stored in the sqlite database.
     *
     * @param channelId the id of the channel for which messages should be loaded.
     * @param limit the maximum number of messages to load from the database.
     * @return a LinkedHashMap containing the newly loaded messages, mapped from message id
     * to Message Object. If no messages were loaded, then an empty Map is returned.
     *
     * @see com.alwaysallthetime.adnlibutils.manager.MessageManager#setDatabaseInsertionEnabled(boolean)
     */
    public synchronized LinkedHashMap<String, MessagePlus> loadPersistedMessages(String channelId, int limit) {
        ADNDatabase database = ADNDatabase.getInstance(mContext);

        Date beforeDate = null;
        MinMaxPair minMaxPair = getMinMaxPair(channelId);
        if(minMaxPair.minId != null) {
            MessagePlus message = mMessages.get(channelId).get(minMaxPair.minId);
            beforeDate = message.getDisplayDate();
        }
        OrderedMessageBatch orderedMessageBatch = database.getMessages(channelId, beforeDate, limit);
        LinkedHashMap<String, MessagePlus> messages = orderedMessageBatch.getMessages();
        MinMaxPair dbMinMaxPair = orderedMessageBatch.getMinMaxPair();
        minMaxPair = minMaxPair.combine(dbMinMaxPair);

        Log.d(TAG, "loaded " + messages.size() + " from database");

        LinkedHashMap<String, MessagePlus> channelMessages = mMessages.get(channelId);
        if(channelMessages != null) {
            channelMessages.putAll(messages);
        } else {
            mMessages.put(channelId, messages);
        }

        mMinMaxPairs.put(channelId, minMaxPair);

        //this should always return only the newly loaded messages.
        return messages;
    }

    /**
     * Enable or disable automatic insertion of Messages into a sqlite database
     * upon retrieval. By default, this feature is turned off.
     *
     * @param isEnabled true if all retrieved Messages should be stashed in a sqlite
     *                  database, false otherwise.
     */
    public void setDatabaseInsertionEnabled(boolean isEnabled) {
        mIsDatabaseInsertionEnabled = isEnabled;
    }

    /**
     * Set a MessageDisplayDateAdapter.
     *
     * @param adapter
     */
    public void setMessageDisplayDateAdapter(MessageDisplayDateAdapter adapter) {
        mDateAdapter = adapter;
    }

    public Map<String, MessagePlus> getMessageMap(String channelId) {
        return mMessages.get(channelId);
    }

    public List<MessagePlus> getMessageList(String channelId) {
        Map<String, MessagePlus> messageMap = mMessages.get(channelId);
        if(messageMap == null) {
            return null;
        }
        MessagePlus[] messages = messageMap.values().toArray(new MessagePlus[0]);
        return Arrays.asList(messages);
    }

    public void setParameters(String channelId, QueryParameters parameters) {
        mParameters.put(channelId, parameters);
    }

    private synchronized MinMaxPair getMinMaxPair(String channelId) {
        MinMaxPair minMaxPair = mMinMaxPairs.get(channelId);
        if(minMaxPair == null) {
            minMaxPair = new MinMaxPair();
            mMinMaxPairs.put(channelId, minMaxPair);
        }
        return minMaxPair;
    }

    public synchronized void retrieveMessages(String channelId, MessageManagerResponseHandler listener) {
        MinMaxPair minMaxPair = getMinMaxPair(channelId);
        retrieveMessages(channelId, minMaxPair.maxId, minMaxPair.minId, listener);
    }

    public synchronized void retrieveNewestMessages(String channelId, MessageManagerResponseHandler listener) {
        retrieveMessages(channelId, getMinMaxPair(channelId).maxId, null, listener);
    }

    public synchronized void retrieveMoreMessages(String channelId, MessageManagerResponseHandler listener) {
        retrieveMessages(channelId, null, getMinMaxPair(channelId).minId, listener);
    }

    public synchronized void refreshMessage(final Message message, final MessageRefreshResponseHandler handler) {
        final String channelId = message.getChannelId();
        mClient.retrieveMessage(channelId, message.getId(), mParameters.get(channelId), new MessageResponseHandler() {
            @Override
            public void onSuccess(Message responseData) {
                LinkedHashMap<String, MessagePlus> channelMessages = mMessages.get(channelId);
                MessagePlus mPlus = new MessagePlus(responseData);
                mPlus.setDisplayDate(getAdjustedDate(responseData));
                channelMessages.put(responseData.getId(), mPlus);

                if(mIsDatabaseInsertionEnabled) {
                    ADNDatabase database = ADNDatabase.getInstance(mContext);
                    database.insertOrReplaceMessage(mPlus);
                }
                Log.d(TAG, "successfully refreshedMessage");
                handler.onSuccess(mPlus);
            }

            @Override
            public void onError(Exception error) {
                super.onError(error);
                handler.onError(error);
            }
        });
    }

    private synchronized  void retrieveMessages(final String channelId, final String sinceId, final String beforeId, final MessageManagerResponseHandler handler) {
        QueryParameters params = (QueryParameters) mParameters.get(channelId).clone();
        params.put("since_id", sinceId);
        params.put("before_id", beforeId);
        mClient.retrieveMessagesInChannel(channelId, params, new MessageListResponseHandler() {
            @Override
            public void onSuccess(final MessageList responseData) {
                ADNDatabase database = ADNDatabase.getInstance(mContext);
                boolean appended = true;

                MinMaxPair minMaxPair = getMinMaxPair(channelId);
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

                LinkedHashMap<String, MessagePlus> channelMessages = mMessages.get(channelId);
                if(channelMessages == null) {
                    channelMessages = new LinkedHashMap<String, MessagePlus>(responseData.size());
                    mMessages.put(channelId, channelMessages);
                }

                ArrayList<MessagePlus> newMessages = new ArrayList<MessagePlus>(responseData.size());
                for(Message m : responseData) {
                    MessagePlus mPlus = new MessagePlus(m);
                    channelMessages.put(m.getId(), mPlus);
                    newMessages.add(mPlus);

                    Date adjustedDate = getAdjustedDate(m);
                    mPlus.setDisplayDate(adjustedDate);
                    if(mIsDatabaseInsertionEnabled) {
                        database.insertOrReplaceMessage(mPlus);
                    }
                }

                if(handler != null) {
                    handler.onSuccess(newMessages, appended);
                }
            }

            @Override
            public void onError(Exception error) {
                Log.d(TAG, error.getMessage(), error);

                if(handler != null) {
                    handler.onError(error);
                }
            }
        });
    }

    private Date getAdjustedDate(Message message) {
        return mDateAdapter == null ? message.getCreatedAt() : mDateAdapter.getDisplayDate(message);
    }
}
