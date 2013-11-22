package com.alwaysallthetime.adnlibutils.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.alwaysallthetime.adnlib.Annotations;
import com.alwaysallthetime.adnlib.AppDotNetClient;
import com.alwaysallthetime.adnlib.QueryParameters;
import com.alwaysallthetime.adnlib.data.Annotation;
import com.alwaysallthetime.adnlib.data.File;
import com.alwaysallthetime.adnlib.data.Message;
import com.alwaysallthetime.adnlib.data.MessageList;
import com.alwaysallthetime.adnlib.gson.AppDotNetGson;
import com.alwaysallthetime.adnlib.response.MessageListResponseHandler;
import com.alwaysallthetime.adnlib.response.MessageResponseHandler;
import com.alwaysallthetime.adnlibutils.db.ADNDatabase;
import com.alwaysallthetime.adnlibutils.db.DisplayLocationInstances;
import com.alwaysallthetime.adnlibutils.db.HashtagInstances;
import com.alwaysallthetime.adnlibutils.db.OrderedMessageBatch;
import com.alwaysallthetime.adnlibutils.model.DisplayLocation;
import com.alwaysallthetime.adnlibutils.model.Geolocation;
import com.alwaysallthetime.adnlibutils.model.MessagePlus;
import com.alwaysallthetime.asyncgeocoder.AsyncGeocoder;
import com.alwaysallthetime.asyncgeocoder.response.AsyncGeocoderResponseHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MessageManager {

    private static final String TAG = "ADNLibUtils_MessageManager";

    private static final int MAX_MESSAGES_RETURNED_ON_SYNC = 100;

    public static final String INTENT_ACTION_UNSENT_MESSAGES_SENT = "com.alwaysallthetime.adnlibutils.manager.MessageManager.intent.unsentMessagesSent";
    public static final String EXTRA_CHANNEL_ID = "com.alwaysallthetime.adnlibutils.manager.MessageManager.extras.channelId";
    public static final String EXTRA_SENT_MESSAGE_IDS = "com.alwaysallthetime.adnlibutils.manager.MessageManager.extras.sentMessageIds";

    /*
     * public data structures
     */
    public static abstract class MessageManagerResponseHandler {
        private boolean isMore;

        public abstract void onSuccess(final List<MessagePlus> responseData, final boolean appended);
        public abstract void onError(Exception exception);

        public void setIsMore(boolean isMore) {
            this.isMore = isMore;
        }

        public boolean isMore() {
            return this.isMore;
        }
    }

    public static abstract class MessageManagerSyncResponseHandler extends MessageManagerResponseHandler {
        private int numMessagesSynced;

        void setNumMessagesSynced(int numMessagesSynced) {
            this.numMessagesSynced = numMessagesSynced;
        }

        public int getNumMessagesSynced() {
            return numMessagesSynced;
        }

        public void onBatchSynced(List<MessagePlus> messages) {
            //override this to do some processing on a batch by batch basis
        }
    }

    public interface MessageRefreshResponseHandler {
        public void onSuccess(final MessagePlus responseData);
        public void onError(Exception exception);
    }

    public interface MessageDeletionResponseHandler {
        public void onSuccess();
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

    private static MessageManager sInstance;

    private Context mContext;
    private ADNDatabase mDatabase;
    private AppDotNetClient mClient;
    private MessageManagerConfiguration mConfiguration;

    private HashMap<String, LinkedHashMap<String, MessagePlus>> mMessages;
    private HashMap<String, LinkedHashMap<String, MessagePlus>> mUnsentMessages;
    private HashMap<String, List<MessagePlus>> mPendingFiles;
    private HashMap<String, QueryParameters> mParameters;
    private HashMap<String, MinMaxPair> mMinMaxPairs;

    public static MessageManager getInstance() {
        return sInstance;
    }

    public static MessageManager init(Context context, AppDotNetClient client, MessageManagerConfiguration configuration) {
        sInstance = new MessageManager(context, client, configuration);
        return sInstance;
    }

    public MessageManager(Context context, AppDotNetClient client, MessageManagerConfiguration configuration) {
        mContext = context;
        mClient = client;
        mConfiguration = configuration;
        mDatabase = ADNDatabase.getInstance(mContext);

        mMessages = new HashMap<String, LinkedHashMap<String, MessagePlus>>();
        mUnsentMessages = new HashMap<String, LinkedHashMap<String, MessagePlus>>();
        mMinMaxPairs = new HashMap<String, MinMaxPair>();
        mParameters = new HashMap<String, QueryParameters>();
        mPendingFiles = new HashMap<String, List<MessagePlus>>();

        IntentFilter intentFilter = new IntentFilter(FileUploadService.INTENT_ACTION_FILE_UPLOAD_COMPLETE);
        context.registerReceiver(fileUploadReceiver, intentFilter);
    }

    /**
     * Load persisted messages that were previously stored in the sqlite database.
     *
     * @param channelId the id of the channel for which messages should be loaded.
     * @param limit the maximum number of messages to load from the database.
     * @return a LinkedHashMap containing the newly loaded messages, mapped from message id
     * to Message Object. If no messages were loaded, then an empty Map is returned.
     *
     * @see com.alwaysallthetime.adnlibutils.manager.MessageManager.MessageManagerConfiguration#setDatabaseInsertionEnabled(boolean)
     */
    public synchronized LinkedHashMap<String, MessagePlus> loadPersistedMessages(String channelId, int limit) {
        Date beforeDate = null;
        MinMaxPair minMaxPair = getMinMaxPair(channelId);
        if(minMaxPair.minId != null) {
            MessagePlus message = mMessages.get(channelId).get(minMaxPair.minId);
            beforeDate = message.getDisplayDate();
        }
        OrderedMessageBatch orderedMessageBatch = mDatabase.getMessages(channelId, beforeDate, limit);
        LinkedHashMap<String, MessagePlus> messages = orderedMessageBatch.getMessages();
        MinMaxPair dbMinMaxPair = orderedMessageBatch.getMinMaxPair();
        minMaxPair = minMaxPair.combine(dbMinMaxPair);

        LinkedHashMap<String, MessagePlus> channelMessages = mMessages.get(channelId);
        if(channelMessages != null) {
            channelMessages.putAll(messages);
        } else {
            mMessages.put(channelId, messages);
        }

        mMinMaxPairs.put(channelId, minMaxPair);

        if(mConfiguration.isLocationLookupEnabled) {
            lookupLocation(messages.values(), false);
        }
        if(mConfiguration.isOEmbedLookupEnabled) {
            lookupOEmbed(messages.values(), false);
        }

        //this should always return only the newly loaded messages.
        return messages;
    }

    public LinkedHashMap<String, MessagePlus> loadPersistedMessagesTemporarily(String channelId, DisplayLocation location, ADNDatabase.LocationPrecision precision) {
        DisplayLocationInstances locationInstances = mDatabase.getDisplayLocationInstances(channelId, location, precision);
        return loadAndConfigureTemporaryMessages(channelId, locationInstances.getMessageIds());
    }

    public LinkedHashMap<String, MessagePlus> loadPersistedMessagesTemporarily(String channelId, String hashtagName) {
        HashtagInstances hashtagInstances = mDatabase.getHashtagInstances(channelId, hashtagName);
        return loadAndConfigureTemporaryMessages(channelId, hashtagInstances.getMessageIds());
    }

    public LinkedHashMap<String, MessagePlus> loadAndConfigureTemporaryMessages(String channelId, Collection<String> messageIds) {
        OrderedMessageBatch orderedMessageBatch = mDatabase.getMessages(channelId, messageIds);
        LinkedHashMap<String, MessagePlus> messages = orderedMessageBatch.getMessages();

        if(mConfiguration.isLocationLookupEnabled) {
            lookupLocation(messages.values(), false);
        }
        if(mConfiguration.isOEmbedLookupEnabled) {
            lookupOEmbed(messages.values(), false);
        }

        return messages;
    }

    private void lookupOEmbed(Collection<MessagePlus> messages, boolean persistIfEnabled) {
        for(MessagePlus messagePlus : messages) {
            Message message = messagePlus.getMessage();

            List<Annotation> oembeds = message.getAnnotationsOfType(Annotations.OEMBED);
            if(oembeds != null) {
                messagePlus.addOEmbedsFromAnnotations(oembeds);
                if(persistIfEnabled && mConfiguration.isDatabaseInsertionEnabled) {
                    mDatabase.insertOrReplaceOEmbedInstances(messagePlus);
                }
            }
        }
    }

    private void lookupLocation(Collection<MessagePlus> messages, boolean persistIfEnabled) {
        for(MessagePlus messagePlus : messages) {
            Message message = messagePlus.getMessage();

            Annotation checkin = message.getFirstAnnotationOfType(Annotations.CHECKIN);
            if(DisplayLocation.isDisplayableCheckinAnnotation(checkin)) {
                messagePlus.setDisplayLocation(DisplayLocation.fromCheckinAnnotation(checkin));
                if(persistIfEnabled && mConfiguration.isDatabaseInsertionEnabled) {
                    mDatabase.insertOrReplaceDisplayLocationInstance(messagePlus);
                }
                continue;
            }

            Annotation ohaiLocation = message.getFirstAnnotationOfType(Annotations.OHAI_LOCATION);
            if(ohaiLocation != null) {
                messagePlus.setDisplayLocation(DisplayLocation.fromOhaiLocation(ohaiLocation));
                if(persistIfEnabled && mConfiguration.isDatabaseInsertionEnabled) {
                    mDatabase.insertOrReplaceDisplayLocationInstance(messagePlus);
                }
                continue;
            }

            Annotation geoAnnotation = message.getFirstAnnotationOfType(Annotations.GEOLOCATION);
            if(geoAnnotation != null) {
                HashMap<String,Object> value = geoAnnotation.getValue();
                final double latitude = (Double)value.get("latitude");
                final double longitude = (Double)value.get("longitude");
                Geolocation geolocationObj = mDatabase.getGeolocation(latitude, longitude);
                if(geolocationObj != null) {
                    messagePlus.setDisplayLocation(DisplayLocation.fromGeolocation(geolocationObj));

                    //this might seem odd based on the fact that we just pulled the geolocation
                    //from the database, but the point is to save the instance of this geolocation's
                    //use - we might obtain a geolocation with this message's lat/long, but that
                    //doesn't mean that this message + geolocation combo has been saved.
                    //(this database lookup is merely an optimization to avoid having to fire off
                    // the async task in reverseGeocode().)
                    if(persistIfEnabled && mConfiguration.isDatabaseInsertionEnabled) {
                        mDatabase.insertOrReplaceDisplayLocationInstance(messagePlus);
                    }
                    continue;
                } else {
                    reverseGeocode(messagePlus, latitude, longitude, persistIfEnabled);
                }
            }
        }
    }

    private void reverseGeocode(final MessagePlus messagePlus, final double latitude, final double longitude, final boolean persistIfEnabled) {
        if(Geocoder.isPresent()) {
            AsyncGeocoder.getInstance(mContext).getFromLocation(latitude, longitude, 5, new AsyncGeocoderResponseHandler() {
                @Override
                public void onSuccess(final List<Address> addresses) {
                    Geolocation geolocation = getGeoLocation(addresses, latitude, longitude);
                    if(geolocation != null) {
                        messagePlus.setDisplayLocation(DisplayLocation.fromGeolocation(geolocation));

                        if(persistIfEnabled && mConfiguration.isDatabaseInsertionEnabled) {
                            mDatabase.insertOrReplaceGeolocation(geolocation);
                            mDatabase.insertOrReplaceDisplayLocationInstance(messagePlus);
                        }
                    }
                    if(mConfiguration.locationLookupHandler != null) {
                        mConfiguration.locationLookupHandler.onSuccess(messagePlus);
                    }
                }

                @Override
                public void onException(Exception exception) {
                    Log.e(TAG, exception.getMessage(), exception);
                    if(mConfiguration.locationLookupHandler != null) {
                        mConfiguration.locationLookupHandler.onException(messagePlus, exception);
                    }
                }
            });
        }
    }

    public LinkedHashMap<String, MessagePlus> getMessageMap(String channelId) {
        return mMessages.get(channelId);
    }

    public AppDotNetClient getClient() {
        return mClient;
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

    private synchronized LinkedHashMap<String, MessagePlus> getChannelMessages(String channelId) {
        LinkedHashMap<String, MessagePlus> channelMessages = mMessages.get(channelId);
        if(channelMessages == null) {
            channelMessages = new LinkedHashMap<String, MessagePlus>(10);
            mMessages.put(channelId, channelMessages);
        }
        return channelMessages;
    }

    private synchronized LinkedHashMap<String, MessagePlus> getUnsentMessages(String channelId) {
        LinkedHashMap<String, MessagePlus> unsentMessages = mUnsentMessages.get(channelId);
        if(unsentMessages == null) {
            unsentMessages = mDatabase.getUnsentMessages(channelId);
            mUnsentMessages.put(channelId, unsentMessages);
        }
        return unsentMessages;
    }

    private synchronized List<MessagePlus> getMessagesNeedingPendingFile(String pendingFileId) {
        List<MessagePlus> messagePlusses = mPendingFiles.get(pendingFileId);
        if(messagePlusses == null) {
            messagePlusses = new ArrayList<MessagePlus>(1);
            mPendingFiles.put(pendingFileId, messagePlusses);
        }
        return messagePlusses;
    }

    public synchronized void clearMessages(String channelId) {
        mMinMaxPairs.put(channelId, null);
        LinkedHashMap<String, MessagePlus> channelMessages = mMessages.get(channelId);
        if(channelMessages != null) {
            channelMessages.clear();
            mDatabase.deleteMessages(channelId);
        }
    }

    public synchronized boolean retrieveMessages(final String channelId, final MessageManagerResponseHandler handler) {
        MinMaxPair minMaxPair = getMinMaxPair(channelId);
        return retrieveMessages(channelId, minMaxPair.maxId, minMaxPair.minId, handler);
    }

    public synchronized boolean retrieveNewestMessages(final String channelId, final MessageManagerResponseHandler handler) {
        return retrieveMessages(channelId, getMinMaxPair(channelId).maxId, null, handler);
    }

    public synchronized boolean retrieveMoreMessages(final String channelId, final MessageManagerResponseHandler handler) {
        return retrieveMessages(channelId, null, getMinMaxPair(channelId).minId, handler);
    }

    public synchronized void createMessage(final String channelId, final Message message, final MessageManagerResponseHandler handler) {
        if(getUnsentMessages(channelId).size() > 0) {
            throw new RuntimeException("This method should not be called when you have unsent messages.");
        }
        mClient.createMessage(channelId, message, new MessageResponseHandler() {
            @Override
            public void onSuccess(Message responseData) {
                //we finish this off by retrieving the newest messages in case we were missing any
                //that came before the one we just created.
                retrieveNewestMessages(channelId, handler);
            }

            @Override
            public void onError(Exception error) {
                super.onError(error);
                handler.onError(error);
            }
        });
    }

    public synchronized MessagePlus createUnsentMessageAndAttemptSend(final String channelId, Message message) {
        return createUnsentMessageAndAttemptSend(channelId, message, new HashSet<String>(0));
    }

    public synchronized MessagePlus createUnsentMessageAndAttemptSend(final String channelId, Message message, Set<String> pendingFileIds) {
        if(!mConfiguration.isDatabaseInsertionEnabled) {
            throw new RuntimeException("Database insertion must be enabled in order to use the unsent messages feature");
        }

        //An unsent message id is always set to the max id + 1.
        //
        //This will work because we will never allow message retrieval to happen
        //until unsent messages are sent to the server and they get their "real"
        //message id. After they reach the server, we will delete them from existence
        //on the client and retrieve them from the server.
        //
        LinkedHashMap<String, MessagePlus> channelMessages = getChannelMessages(channelId);
        if(channelMessages.size() == 0) {
            //we do this so that the max id is known.
            loadPersistedMessages(channelId, 1);
        }

        MinMaxPair minMaxPair = getMinMaxPair(channelId);
        Integer maxInteger = minMaxPair.getMaxAsInteger();
        Integer newMessageId = maxInteger != null ? maxInteger + 1 : 1;
        String newMessageIdString = String.valueOf(newMessageId);

        MessagePlus.UnsentMessagePlusBuilder unsentBuilder = MessagePlus.UnsentMessagePlusBuilder.newBuilder(channelId, newMessageIdString, message);
        Iterator<String> iterator = pendingFileIds.iterator();
        while(iterator.hasNext()) {
            unsentBuilder.addPendingOEmbed(iterator.next());
        }
        final MessagePlus messagePlus = unsentBuilder.build();
        mDatabase.insertOrReplaceMessage(messagePlus);

        //problem to solve - display locations need
//        if(mConfiguration.isLocationLookupEnabled) {
//            ArrayList<MessagePlus> mp = new ArrayList<MessagePlus>(1);
//            mp.add(messagePlus);
//            lookupLocation(mp, true);
//        }

        LinkedHashMap<String, MessagePlus> channelUnsentMessages = getUnsentMessages(channelId);
        channelUnsentMessages.put(newMessageIdString, messagePlus);

        LinkedHashMap<String, MessagePlus> newChannelMessages = new LinkedHashMap<String, MessagePlus>(channelMessages.size() + 1);
        newChannelMessages.put(messagePlus.getMessage().getId(), messagePlus);
        newChannelMessages.putAll(channelMessages);
        mMessages.put(channelId, newChannelMessages);

        minMaxPair.maxId = newMessageIdString;

        Log.d(TAG, "Created and stored unsent message with id " + newMessageIdString);

        sendUnsentMessages(channelId);

        return messagePlus;
    }

    public synchronized void deleteMessage(final MessagePlus messagePlus, final MessageDeletionResponseHandler handler) {
        if(messagePlus.isUnsent()) {
            Message message = messagePlus.getMessage();
            String messageId = message.getId();
            String channelId = message.getChannelId();
            LinkedHashMap<String, MessagePlus> channelMessages = getChannelMessages(channelId);

            mDatabase.deleteMessage(messagePlus);
            getUnsentMessages(channelId).remove(messageId);
            channelMessages.remove(messageId);

            MinMaxPair minMaxPair = getMinMaxPair(channelId);
            if(channelMessages.size() > 0) {
                minMaxPair.maxId = channelMessages.keySet().iterator().next();
            } else {
                minMaxPair.maxId = null;
            }
            handler.onSuccess();
        } else {
            mClient.deleteMessage(messagePlus.getMessage(), new MessageResponseHandler() {
                @Override
                public void onSuccess(Message responseData) {
                    LinkedHashMap<String, MessagePlus> channelMessages = mMessages.get(responseData.getChannelId());
                    channelMessages.remove(responseData.getId());
                    mDatabase.deleteMessage(messagePlus); //this one because the deleted one doesn't have the entities.

                    handler.onSuccess();
                }

                @Override
                public void onError(Exception error) {
                    super.onError(error);
                    handler.onError(error);
                }
            });
        }
    }

    public synchronized void refreshMessage(final Message message, final MessageRefreshResponseHandler handler) {
        final String channelId = message.getChannelId();
        mClient.retrieveMessage(channelId, message.getId(), mParameters.get(channelId), new MessageResponseHandler() {
            @Override
            public void onSuccess(Message responseData) {
                MessagePlus mPlus = new MessagePlus(responseData);
                mPlus.setDisplayDate(getAdjustedDate(responseData));

                LinkedHashMap<String, MessagePlus> channelMessages = mMessages.get(channelId);
                if(channelMessages != null) { //could be null of channel messages weren't loaded first, etc.
                    channelMessages.put(responseData.getId(), mPlus);
                }

                if(mConfiguration.isDatabaseInsertionEnabled) {
                    mDatabase.insertOrReplaceMessage(mPlus);
                }
                handler.onSuccess(mPlus);
            }

            @Override
            public void onError(Exception error) {
                super.onError(error);
                handler.onError(error);
            }
        });
    }

    /**
     * Sync and persist all Messages in a Channel.
     *
     * This is intended to be used as a one-time sync, e.g. after a user signs in. For this reason,
     * it is required that your MessageManagerConfiguration has its isDatabaseInsertionEnabled property
     * set to true.
     *
     * Because this could potentially result in a very large amount of Messages being obtained,
     * the provided MessageManagerResponseHandler will only be passed the first 100 Messages that are
     * obtained, while the others will be persisted to the sqlite database, but not kept in memory.
     * However, these can easily be loaded into memory afterwards by calling loadPersistedMessages().
     *
     * @param channelId The id of the Channel from which to obtain Messages.
     * @param responseHandler MessageManagerResponseHandler
     *
     * @see com.alwaysallthetime.adnlibutils.manager.MessageManager.MessageManagerConfiguration#setDatabaseInsertionEnabled(boolean)
     * @see MessageManager#loadPersistedMessages(String, int)
     */
    public synchronized void retrieveAndPersistAllMessages(String channelId, MessageManagerSyncResponseHandler responseHandler) {
        if(!mConfiguration.isDatabaseInsertionEnabled) {
            throw new RuntimeException("Database insertion must be enabled to use this functionality.");
        }
        final ArrayList<MessagePlus> messages = new ArrayList<MessagePlus>(MAX_MESSAGES_RETURNED_ON_SYNC);
        String sinceId = null;
        String beforeId = null;
        retrieveAllMessages(messages, sinceId, beforeId, channelId, responseHandler);
    }

    private synchronized void retrieveAllMessages(final ArrayList<MessagePlus> messages, String sinceId, String beforeId, final String channelId, final MessageManagerSyncResponseHandler responseHandler) {
        QueryParameters params = (QueryParameters) mParameters.get(channelId).clone();
        params.put("since_id", sinceId);
        params.put("before_id", beforeId);
        params.put("count", String.valueOf(MAX_MESSAGES_RETURNED_ON_SYNC));

        retrieveMessages(params, channelId, new MessageManagerResponseHandler() {
            @Override
            public void onSuccess(List<MessagePlus> responseData, boolean appended) {
                if(messages.size() == 0) {
                    messages.addAll(responseData);
                }
                responseHandler.setNumMessagesSynced(responseHandler.getNumMessagesSynced() + responseData.size());
                responseHandler.onBatchSynced(responseData);

                if(isMore()) {
                    MinMaxPair minMaxPair = getMinMaxPair(channelId);
                    retrieveAllMessages(messages, null, minMaxPair.minId, channelId, responseHandler);
                } else {
                    Log.d(TAG, "Num messages synced: " + responseHandler.getNumMessagesSynced());
                    responseHandler.onSuccess(messages, true);
                }
            }

            @Override
            public void onError(Exception exception) {
                Log.e(TAG, exception.getMessage(), exception);
                responseHandler.onError(exception);
            }
        });
    }

    private synchronized boolean retrieveMessages(final String channelId, final String sinceId, final String beforeId, final MessageManagerResponseHandler handler) {
        QueryParameters params = (QueryParameters) mParameters.get(channelId).clone();
        params.put("since_id", sinceId);
        params.put("before_id", beforeId);
        return retrieveMessages(params, channelId, handler);
    }

    private synchronized void sendUnsentMessages(final LinkedHashMap<String, MessagePlus> unsentMessages, final ArrayList<String> sentMessageIds) {
        final MessagePlus messagePlus = unsentMessages.get(unsentMessages.keySet().iterator().next());
        if(messagePlus.hasPendingOEmbeds()) {
            String pendingFileId = messagePlus.getPendingOEmbeds().iterator().next();
            List<MessagePlus> messagesNeedingPendingFile = getMessagesNeedingPendingFile(pendingFileId);
            messagesNeedingPendingFile.add(messagePlus);
            //TODO: this should somehow be prepopulated?

            FileManager.getInstance(mContext, mClient).startPendingFileUpload(pendingFileId);
            return;
        }
        final Message message = messagePlus.getMessage();

        //we had them set for display locally, but we should
        //let the server generate the "real" entities.
        message.setEntities(null);

        mClient.createMessage(message.getChannelId(), message, new MessageResponseHandler() {
            @Override
            public void onSuccess(Message responseData) {
                Log.d(TAG, "Successfully sent unsent message with id " + message.getId());

                unsentMessages.remove(message.getId());
                sentMessageIds.add(message.getId());

                mDatabase.deleteMessage(messagePlus);

                //remove the message from in-memory message map.
                LinkedHashMap<String, MessagePlus> channelMessages = getChannelMessages(message.getChannelId());
                channelMessages.remove(message.getId());

                MinMaxPair minMaxPair = getMinMaxPair(message.getChannelId());
                if(unsentMessages.size() > 0) {
                    String nextId = unsentMessages.keySet().iterator().next();
                    minMaxPair.maxId = nextId;
                    sendUnsentMessages(unsentMessages, sentMessageIds);
                } else {
                    if(channelMessages.size() > 0) {
                        //step back in time until we find the first message that was NOT one
                        //of the unsent messages. this will be the max id.
                        Iterator<String> channelMessagesIterator = channelMessages.keySet().iterator();
                        while(channelMessagesIterator.hasNext()) {
                            String next = channelMessagesIterator.next();
                            if(!sentMessageIds.contains(next)) {
                                minMaxPair.maxId = next;
                                break;
                            }
                        }
                    } else {
                        minMaxPair.maxId = null;
                    }

                    Intent i = new Intent(INTENT_ACTION_UNSENT_MESSAGES_SENT);
                    i.putExtra(EXTRA_CHANNEL_ID, message.getChannelId());
                    i.putStringArrayListExtra(EXTRA_SENT_MESSAGE_IDS, sentMessageIds);
                    mContext.sendBroadcast(i);
                }
            }

            @Override
            public void onError(Exception exception) {
                super.onError(exception);
                messagePlus.incrementSendAttempts();
                mDatabase.insertOrReplaceMessage(messagePlus);
            }
        });
    }

    public synchronized boolean hasUnsentMessages(String channelId) {
        return getUnsentMessages(channelId).size() > 0;
    }

    public synchronized void sendUnsentMessages(final String channelId) {
        LinkedHashMap<String, MessagePlus> unsentMessages = getUnsentMessages(channelId);
        if(unsentMessages.size() > 0) {
            LinkedHashMap<String, MessagePlus> channelMessages = getChannelMessages(channelId);
            if(channelMessages.size() == 0) {
                //we do this so that the max id is known.
                loadPersistedMessages(channelId, unsentMessages.size() + 1);
            }
            ArrayList<String> sentMessageIds = new ArrayList<String>(unsentMessages.size());
            sendUnsentMessages(unsentMessages, sentMessageIds);
        }
    }

    private synchronized boolean retrieveMessages(final QueryParameters queryParameters, final String channelId, final MessageManagerResponseHandler handler) {
        LinkedHashMap<String, MessagePlus> unsentMessages = getUnsentMessages(channelId);
        if(unsentMessages.size() > 0) {
            return false;
        }
        mClient.retrieveMessagesInChannel(channelId, queryParameters, new MessageListResponseHandler() {
            @Override
            public void onSuccess(final MessageList responseData) {
                boolean appended = true;
                String beforeId = queryParameters.get("before_id");
                String sinceId = queryParameters.get("since_id");

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

                LinkedHashMap<String, MessagePlus> channelMessages = getChannelMessages(channelId);

                ArrayList<MessagePlus> newestMessages = new ArrayList<MessagePlus>(responseData.size());
                LinkedHashMap<String, MessagePlus> newFullChannelMessagesMap = new LinkedHashMap<String, MessagePlus>(channelMessages.size() + responseData.size());

                if(appended) {
                    newFullChannelMessagesMap.putAll(channelMessages);
                }
                for(Message m : responseData) {
                    MessagePlus mPlus = new MessagePlus(m);
                    newestMessages.add(mPlus);
                    adjustDateAndInsert(mPlus);

                    newFullChannelMessagesMap.put(m.getId(), mPlus);
                }
                if(!appended) {
                    newFullChannelMessagesMap.putAll(channelMessages);
                }
                mMessages.put(channelId, newFullChannelMessagesMap);

                if(mConfiguration.isLocationLookupEnabled) {
                    lookupLocation(newestMessages, true);
                }
                if(mConfiguration.isOEmbedLookupEnabled) {
                    lookupOEmbed(newestMessages, true);
                }

                if(handler != null) {
                    handler.setIsMore(isMore());
                    handler.onSuccess(newestMessages, appended);
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
        return true;
    }

    private void adjustDateAndInsert(MessagePlus mPlus) {
        Date adjustedDate = getAdjustedDate(mPlus.getMessage());
        mPlus.setDisplayDate(adjustedDate);
        if(mConfiguration.isDatabaseInsertionEnabled) {
            mDatabase.insertOrReplaceMessage(mPlus);
            mDatabase.insertOrReplaceHashtagInstances(mPlus);
        }
    }

    private Date getAdjustedDate(Message message) {
        return mConfiguration.dateAdapter == null ? message.getCreatedAt() : mConfiguration.dateAdapter.getDisplayDate(message);
    }

    private Geolocation getGeoLocation(List<Address> addresses, double latitude, double longitude) {
        String locality = null;
        String subLocality = null;

        for(Address address : addresses) {
            if(subLocality == null) {
                subLocality = address.getSubLocality();
            }
            if(subLocality != null || locality == null) {
                locality = address.getLocality();
            }

            if(subLocality != null && locality != null) {
                break;
            }
        }

        if(subLocality != null && locality != null) {
            return new Geolocation(locality, subLocality, latitude, longitude);
        }

        return null;
    }

    private final BroadcastReceiver fileUploadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(FileUploadService.INTENT_ACTION_FILE_UPLOAD_COMPLETE.equals(intent.getAction())) {
                String pendingFileId = intent.getStringExtra(FileUploadService.EXTRA_PENDING_FILE_ID);
                if(pendingFileId != null) {
                    boolean success = intent.getBooleanExtra(FileUploadService.EXTRA_SUCCESS, false);
                    if(success) {
                        Log.d(TAG, "Successfully uploaded pending file with id " + pendingFileId);

                        List<MessagePlus> messagesNeedingFile = mPendingFiles.get(pendingFileId);
                        if(messagesNeedingFile != null) {
                            HashSet<String> channelIdsWithMessagesToSend = new HashSet<String>();
                            String fileJson = intent.getStringExtra(FileUploadService.EXTRA_FILE);
                            File file = AppDotNetGson.getPersistenceInstance().fromJson(fileJson, File.class);

                            for(MessagePlus messagePlus : messagesNeedingFile) {
                                Message message = messagePlus.getMessage();
                                messagePlus.replacePendingOEmbedWithOEmbedAnnotation(pendingFileId, file);
                                mDatabase.insertOrReplaceMessage(messagePlus);
                                mDatabase.deletePendingOEmbed(pendingFileId, message.getId(), message.getChannelId());

                                if(messagePlus.getPendingOEmbeds().size() == 0) {
                                    channelIdsWithMessagesToSend.add(message.getChannelId());
                                }
                            }

                            for(String channelId : channelIdsWithMessagesToSend) {
                                sendUnsentMessages(channelId);
                                Log.d(TAG, "Now retrying send for unsent messages in channel " + channelId);
                            }
                        }
                    } else {
                        //TODO
                        Log.e(TAG, "Failed to upload pending file with id " + pendingFileId);
                    }
                }
            }
        }
    };

    public static class MessageManagerConfiguration {

        public static interface MessageLocationLookupHandler {
            public void onSuccess(MessagePlus messagePlus);
            public void onException(MessagePlus messagePlus, Exception exception);
        }

        boolean isDatabaseInsertionEnabled;
        boolean isOEmbedLookupEnabled;
        boolean isLocationLookupEnabled;
        MessageDisplayDateAdapter dateAdapter;
        MessageLocationLookupHandler locationLookupHandler;

        /**
         * Enable or disable automatic insertion of Messages into a sqlite database
         * upon retrieval. By default, this feature is turned off.
         *
         * @param isEnabled true if all retrieved Messages should be stashed in a sqlite
         *                  database, false otherwise.
         */
        public void setDatabaseInsertionEnabled(boolean isEnabled) {
            this.isDatabaseInsertionEnabled = isEnabled;
        }

        /**
         * Set a MessageDisplayDateAdapter.
         *
         * @param adapter
         * @see com.alwaysallthetime.adnlibutils.manager.MessageManager.MessageDisplayDateAdapter
         */
        public void setMessageDisplayDateAdapter(MessageDisplayDateAdapter adapter) {
            this.dateAdapter = adapter;
        }

        /**
         * Enable OEmbed lookup on Messages. If enabled, annotations will be examined in order to
         * determine if OEmbed photo or video annotations are present. The associated MessagePlus
         * will then have these OEmbed Objects obtainable via convenience methods.
         *
         * This is especially useful when database insertion is enabled – instances of photo and
         * video OEmbeds will be stored in a table for look up at a later time (e.g. "gimme all
         * messages for which there are photos attached").
         *
         * @param isEnabled
         */
        public void setOEmbedLookupEnabled(boolean isEnabled) {
            this.isOEmbedLookupEnabled = isEnabled;
        }

        /**
         * Enable location lookup on Messages. If enabled, annotations will be examined in order
         * to construct a DisplayLocation. A DisplayLocation will be set on the associated MessagePlus
         * Object, based off one of these three annotations, if they exist:
         *
         * net.app.core.checkin
         * net.app.ohai.location
         * net.app.core.geolocation
         *
         * In the case of net.app.core.geolocation, an asynchronous task will be fired off to
         * perform reverse geolocation on the latitude/longitude coordinates. For this reason, you
         * should set a MessageLocationLookupHandler on this configuration if you want to perform
         * a task such as update UI after a location is obtained.
         *
         * If none of these annotations are found, then a null DisplayLocation is set on the
         * associated MessagePlus.
         *
         * @param isEnabled true if location lookup should be performed on all Messages
         *
         * @see com.alwaysallthetime.adnlibutils.model.MessagePlus#getDisplayLocation()
         * @see com.alwaysallthetime.adnlibutils.model.MessagePlus#hasSetDisplayLocation()
         * @see com.alwaysallthetime.adnlibutils.model.MessagePlus#hasDisplayLocation()
         */
        public void setLocationLookupEnabled(boolean isEnabled) {
            this.isLocationLookupEnabled = isEnabled;
        }

        /**
         * Specify a handler to be notified when location lookup has completed for a MessagePlus.
         * This is particularly useful when a geolocation annotation requires an asynchronous
         * reverse geocoding task.
         *
         * @param handler
         */
        public void setLocationLookupHandler(MessageLocationLookupHandler handler) {
            this.locationLookupHandler = handler;
        }
    }
}
