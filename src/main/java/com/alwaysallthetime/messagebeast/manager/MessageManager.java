package com.alwaysallthetime.messagebeast.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.alwaysallthetime.adnlib.Annotations;
import com.alwaysallthetime.adnlib.AppDotNetClient;
import com.alwaysallthetime.adnlib.AppDotNetObjectCloner;
import com.alwaysallthetime.adnlib.QueryParameters;
import com.alwaysallthetime.adnlib.data.Annotation;
import com.alwaysallthetime.adnlib.data.Channel;
import com.alwaysallthetime.adnlib.data.File;
import com.alwaysallthetime.adnlib.data.Message;
import com.alwaysallthetime.adnlib.data.MessageList;
import com.alwaysallthetime.adnlib.gson.AppDotNetGson;
import com.alwaysallthetime.adnlib.response.FileResponseHandler;
import com.alwaysallthetime.adnlib.response.MessageListResponseHandler;
import com.alwaysallthetime.adnlib.response.MessageResponseHandler;
import com.alwaysallthetime.asyncgeocoder.AsyncGeocoder;
import com.alwaysallthetime.asyncgeocoder.response.AsyncGeocoderResponseHandler;
import com.alwaysallthetime.messagebeast.ADNApplication;
import com.alwaysallthetime.messagebeast.ADNSharedPreferences;
import com.alwaysallthetime.messagebeast.PrivateChannelUtility;
import com.alwaysallthetime.messagebeast.db.ADNDatabase;
import com.alwaysallthetime.messagebeast.db.AnnotationInstances;
import com.alwaysallthetime.messagebeast.db.DisplayLocationInstances;
import com.alwaysallthetime.messagebeast.db.FilteredMessageBatch;
import com.alwaysallthetime.messagebeast.db.HashtagInstances;
import com.alwaysallthetime.messagebeast.db.OrderedMessageBatch;
import com.alwaysallthetime.messagebeast.db.PendingFileAttachment;
import com.alwaysallthetime.messagebeast.db.PendingMessageDeletion;
import com.alwaysallthetime.messagebeast.filter.MessageFilter;
import com.alwaysallthetime.messagebeast.filter.MessageInstancesFilter;
import com.alwaysallthetime.messagebeast.model.DisplayLocation;
import com.alwaysallthetime.messagebeast.model.FullSyncState;
import com.alwaysallthetime.messagebeast.model.Geolocation;
import com.alwaysallthetime.messagebeast.model.MessagePlus;

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
import java.util.TreeMap;

/**
 * MessageManager is used to retrieve, create, and delete Messages in any number of channels.<br><br>
 *
 * All Messages that are obtained via the MessageManager will use a wrapper class called MessagePlus,
 * which contains extra metadata associated with the Message. A MessageManagerConfiguration is required
 * in order to determine which of the following features are used to add metadata to the MessagePlus:<br><br>
 *
 * • DisplayLocation lookup. Geolocation, Checkin, and Ohai Location annotations are consolidated into a single
 * DisplayLocation object. In the case of Geolocation, an asynchronous task is fired off to perform
 * reverse geolocation (in order to find a human-readable name for the location).<br>
 *
 * • OEmbed lookup. For all OEmbed annotations found on a Message, PhotoOEmbed and Html5OEmbed
 * objects are constructed and stored for easy access on the MessagePlus. Additionally, this data
 * is stored in a sqlite database so lookups can be performed (e.g. give me all messages with videos).<br>
 *
 * • Association of an a display date that is different than the created_at date via a MessageDateAdapter.<br><br>
 *
 * Some additional key features are are:<br><br>
 *
 * • Persistence of all Messages in a sqlite database. Messages can be loaded on subsequent launches
 * by using the loadPersistedMessages methods, and several ADNDatabase methods are available for obtaining
 * Messages or performing operations on them (e.g. full text search).<br>
 *
 * • Offline Message creation and deletion. The createUnsentMessageAndAttemptSend() methods can be used
 * to create Messages that will be saved to the local sqlite database with an unsent flag set when there
 * is no internet connection available. The MessageManager will return the associated MessagePlus objects
 * as if they are "real" Messages and delete them after they are successfully sent (so that the "real"
 * Message can be retrieved - with its server-assigned id).<br>
 *
 * • Perform a full sync on a specified channel, obtaining every Message in existence in that Channel.
 * This is especially useful in cases where a Channel is used privately by a single user.
 */
public class MessageManager {

    private static final String TAG = "ADNLibUtils_MessageManager";

    private static final int MAX_MESSAGES_RETURNED_ON_SYNC = 100;

    /**
     * An intent with this action is broadcasted when unsent messages are successfully sent.
     *
     * The EXTRA_CHANNEL_ID and EXTRA_SENT_MESSAGE_IDS extras contain relevant info.
     *
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#EXTRA_CHANNEL_ID
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#EXTRA_SENT_MESSAGE_IDS
     */
    public static final String INTENT_ACTION_UNSENT_MESSAGES_SENT = "com.alwaysallthetime.messagebeast.manager.MessageManager.intent.unsentMessagesSent";

    /**
     * An intent with this action is broadcasted when an unsent message fails to send upon request.
     *
     * The EXTRA_CHANNEL_ID, EXTRA_MESSAGE_ID, and EXTRA_SEND_ATTEMPTS extras contain relevant info.
     *
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#EXTRA_CHANNEL_ID
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#EXTRA_MESSAGE_ID
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#EXTRA_SEND_ATTEMPTS
     */
    public static final String INTENT_ACTION_UNSENT_MESSAGE_SEND_FAILURE = "com.alwaysallthetime.messagebeast.manager.MessageManager.intent.unsentMessageSendFailure";

    public static final String EXTRA_CHANNEL_ID = "com.alwaysallthetime.messagebeast.manager.MessageManager.extras.channelId";
    public static final String EXTRA_SENT_MESSAGE_IDS = "com.alwaysallthetime.messagebeast.manager.MessageManager.extras.sentMessageIds";
    public static final String EXTRA_MESSAGE_ID = "com.alwaysallthetime.messagebeast.manager.MessageManager.extras.messageId";
    public static final String EXTRA_SEND_ATTEMPTS = "com.alwaysallthetime.messagebeast.manager.MessageManager.extras.sendAttempts";

    public static abstract class MessageManagerResponseHandler {
        private boolean isMore;
        private TreeMap<Long, MessagePlus> excludedResults;

        public abstract void onSuccess(final List<MessagePlus> messages);
        public abstract void onError(Exception exception);

        public void setIsMore(boolean isMore) {
            this.isMore = isMore;
        }
        public boolean isMore() {
            return this.isMore;
        }

        public void setExcludedResults(TreeMap<Long, MessagePlus> excludedResults) {
            this.excludedResults = excludedResults;
        }

        public TreeMap<Long, MessagePlus> getExcludedResults() {
            return excludedResults;
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

    public interface MessageManagerMultiChannelSyncResponseHandler {
        public void onSuccess();
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

    private HashMap<String, TreeMap<Long, MessagePlus>> mMessages;
    private HashMap<String, TreeMap<Long, MessagePlus>> mUnsentMessages;

    private HashMap<String, Set<String>> mMessagesNeedingPendingFiles;
    private HashMap<String, QueryParameters> mParameters;
    private HashMap<String, MinMaxPair> mMinMaxPairs;

    public MessageManager(AppDotNetClient client, MessageManagerConfiguration configuration) {
        mContext = ADNApplication.getContext();
        mClient = client;
        mConfiguration = configuration;
        mDatabase = ADNDatabase.getInstance(mContext);

        mMessages = new HashMap<String, TreeMap<Long, MessagePlus>>();
        mUnsentMessages = new HashMap<String, TreeMap<Long, MessagePlus>>();
        mMinMaxPairs = new HashMap<String, MinMaxPair>();
        mParameters = new HashMap<String, QueryParameters>();
        mMessagesNeedingPendingFiles = new HashMap<String, Set<String>>();

        IntentFilter intentFilter = new IntentFilter(FileUploadService.INTENT_ACTION_FILE_UPLOAD_COMPLETE);
        mContext.registerReceiver(fileUploadReceiver, intentFilter);
    }

    /**
     * Purge all Message and Channel-related data from memory. This will leave the MessageManager
     * in the initial state – as it was upon construction. Note that this does not delete any
     * persisted data from the sqlite database.
     *
     * @see com.alwaysallthetime.messagebeast.db.ADNDatabase#deleteAll() 
     */
    public synchronized void clear() {
        mMessages.clear();
        mUnsentMessages.clear();
        mMessagesNeedingPendingFiles.clear();
        mParameters.clear();
        mMinMaxPairs.clear();
        mMessagesNeedingPendingFiles.clear();
    }

    private synchronized OrderedMessageBatch loadPersistedMessageBatch(String channelId, int limit, boolean performLookups) {
        Date beforeDate = null;
        MinMaxPair minMaxPair = getMinMaxPair(channelId);
        if(minMaxPair.minDate != null) {
            beforeDate = new Date(minMaxPair.minDate);
        }
        OrderedMessageBatch orderedMessageBatch = mDatabase.getMessages(channelId, beforeDate, limit);
        TreeMap<Long, MessagePlus> messages = orderedMessageBatch.getMessages();
        MinMaxPair dbMinMaxPair = orderedMessageBatch.getMinMaxPair();
        minMaxPair.updateWithCombinedValues(dbMinMaxPair);

        TreeMap<Long, MessagePlus> channelMessages = mMessages.get(channelId);
        if(channelMessages != null) {
            channelMessages.putAll(messages);
        } else {
            mMessages.put(channelId, messages);
        }

        if(performLookups) {
            performLookups(messages.values(), false);
        }

        return orderedMessageBatch;
    }

    /**
     * Load persisted messages that were previously stored in the sqlite database.
     *
     * @param channelId the id of the channel for which messages should be loaded.
     * @param limit the maximum number of messages to load from the database.
     * @return a TreeMap containing the newly loaded messages, mapped from message time in millis
     * to MessagePlus Object. If no Messages were loaded, then an empty Map is returned.
     */
    public synchronized TreeMap<Long, MessagePlus> loadPersistedMessages(String channelId, int limit) {
        OrderedMessageBatch batch = loadPersistedMessageBatch(channelId, limit, true);
        return batch.getMessages();
    }

    /**
     * Load persisted messages that were previously stored in the sqlite database, using a filter
     * to exclude any number of messages.
     *
     * @param channelId the id of the channel for which messages should be loaded.
     * @param limit the maximum number of messages to load from the database.
     * @param filter the MessageFilter to use to exclude messages.
     *
     * @return A FilteredMessageBatch containing the messages after a filter was applied, and additionally
     * a Map of messages containing the excluded Messages.
     *
     * @see com.alwaysallthetime.messagebeast.filter.MessageFilter
     * @see com.alwaysallthetime.messagebeast.db.FilteredMessageBatch
     */
    public synchronized FilteredMessageBatch loadPersistedMessages(String channelId, int limit, MessageFilter filter) {
        OrderedMessageBatch batch = loadPersistedMessageBatch(channelId, limit, false);
        FilteredMessageBatch filteredBatch = FilteredMessageBatch.getFilteredMessageBatch(batch, filter);
        TreeMap<Long, MessagePlus> excludedMessages = filteredBatch.getExcludedMessages();

        //remove the filtered messages from the main channel message map.
        TreeMap<Long, MessagePlus> channelMessages = mMessages.get(channelId);
        removeExcludedMessages(channelMessages, excludedMessages);

        //do this after we have successfully filtered out stuff,
        //as to not perform lookups on things we didn't keep.
        performLookups(filteredBatch.getMessages().values(), false);

        return filteredBatch;
    }

    /**
     * Load persisted Messages without keeping them in MessageManager memory.
     *
     * No DisplayLocation or OEmbed lookup will be performed on these Messages.
     *
     * @param channelId the Channel id
     * @param limit the maximum number of Messages to load from the database.
     * @return a TreeMap mapping Message times in millis to MessagePlus objects
     */
    public TreeMap<Long, MessagePlus> getMessages(String channelId, int limit) {
        OrderedMessageBatch orderedMessageBatch = mDatabase.getMessages(channelId, limit);
        return orderedMessageBatch.getMessages();
    }

    /**
     * Load all persisted Messages with an associated DisplayLocation without keeping them in
     * MessageManager memory.
     *
     * Messages will be returned after performing DisplayLocation and OEmbed lookup, provided those
     * features are enabled in the MessageManagerConfiguration.
     *
     * @param channelId the Channel id
     * @param location the DisplayLocation
     * @param precision the precision to use when obtaining location instances.
     * @return a TreeMap mapping Message times in millis to MessagePlus objects
     *
     * @see com.alwaysallthetime.messagebeast.db.ADNDatabase#getDisplayLocationInstances(String, com.alwaysallthetime.messagebeast.model.DisplayLocation, com.alwaysallthetime.messagebeast.db.ADNDatabase.LocationPrecision)
     */
    public TreeMap<Long, MessagePlus> getMessages(String channelId, DisplayLocation location, ADNDatabase.LocationPrecision precision) {
        DisplayLocationInstances locationInstances = mDatabase.getDisplayLocationInstances(channelId, location, precision);
        return getMessages(locationInstances.getMessageIds());
    }

    /**
     * Load all persisted Messages with a hashtag entity matching the provided hashtag, without keeping them in
     * MessageManager memory.
     *
     * Messages will be returned after performing DisplayLocation and OEmbed lookup, provided those
     * features are enabled in the MessageManagerConfiguration.
     *
     * @param channelId the Channel id
     * @param hashtagName the hashtag with which the lookup will be done
     * @return a TreeMap mapping Message times in millis to MessagePlus objects
     */
    public TreeMap<Long, MessagePlus> getMessages(String channelId, String hashtagName) {
        HashtagInstances hashtagInstances = mDatabase.getHashtagInstances(channelId, hashtagName);
        return getMessages(hashtagInstances.getMessageIds());
    }

    /**
     * Load persisted Messages that use a specific type of Annotation, without keeping them in
     * MessageManager memory.
     *
     * @param channelId the id of the Channel in which the returned Messages will be contained
     * @param annotationType the Annotation type to look for
     * @return a TreeMap mapping Message times in millis to MessagePlus objects
     */
    public TreeMap<Long, MessagePlus> getMessagesWithAnnotation(String channelId, String annotationType) {
        AnnotationInstances instances = getAnnotationInstances(channelId, annotationType);
        return getMessages(instances.getMessageIds());
    }

    /**
     * Load persisted Messages without keeping them in MessageManager memory.
     *
     * Messages will be returned after performing DisplayLocation and OEmbed lookup, provided those
     * features are enabled in the MessageManagerConfiguration.
     *
     * @param messageIds the Message ids
     * @return a TreeMap mapping Message times in millis to MessagePlus objects
     */
    public TreeMap<Long, MessagePlus> getMessages(Collection<String> messageIds) {
        OrderedMessageBatch orderedMessageBatch = mDatabase.getMessages(messageIds);
        TreeMap<Long, MessagePlus> messages = orderedMessageBatch.getMessages();
        performLookups(messages.values(), false);
        return messages;
    }

    /**
     * Get all HashtagInstances in a Channel.
     *
     * @param channelId the id of the Channel
     * @return a LinkedHashMap, mapping hashtag name to a HashtagInstances object. This is
     *         in descending order, from most to least recent.
     */
    public LinkedHashMap<String, HashtagInstances> getHashtagInstances(String channelId) {
        return mDatabase.getHashtagInstances(channelId);
    }

    /**
     * Get all HashtagInstances in a Channel, using a filter to remove unwanted results.
     *
     * @param channelId the id of the Channel
     * @param messageFilter the filter to use to excluded unwanted results.
     * @return a LinkedHashMap, mapping hashtag name to a HashtagInstances object. This is
     *         in descending order, from most to least recent.
     */
    public LinkedHashMap<String, HashtagInstances> getHashtagInstances(String channelId, MessageInstancesFilter messageFilter) {
        LinkedHashMap<String, HashtagInstances> hashtagInstances = mDatabase.getHashtagInstances(channelId);
        messageFilter.filterInstances(hashtagInstances);
        return hashtagInstances;
    }

    /**
     * Get all DisplayLocationInstances in a Channel.
     *
     * @param channelId the id of the Channel
     * @return a List of DisplayLocationInstances in descending order, from most to least recent
     */
    public List<DisplayLocationInstances> getDisplayLocationInstances(String channelId) {
        return mDatabase.getDisplayLocationInstances(channelId);
    }

    /**
     * Get all DisplayLocationInstances in a Channel, using a filter to remove unwanted results.
     *
     * @param channelId the id of the Channel
     * @param messageFilter the filter to use to excluded unwanted results.
     * @return a List of DisplayLocationInstances in descending order, from most to least recent
     */
    public List<DisplayLocationInstances> getDisplayLocationInstances(String channelId, MessageInstancesFilter messageFilter) {
        LinkedHashMap<String, DisplayLocationInstances> displayLocationInstancesMap = mDatabase.getDisplayLocationInstancesMap(channelId);
        messageFilter.filterInstances(displayLocationInstancesMap);
        return new ArrayList<DisplayLocationInstances>(displayLocationInstancesMap.values());
    }

    /**
     * Get references to Messages that use a specific type of Annotationtype.
     *
     * @param channelId the id of the Channel in which the returned Messages will be contained.
     * @param annotationType the Annotation type to look for.
     * @return an AnnotationInstances containing message ids that correspond to Messages with
     * the provided annotation type.
     */
    public AnnotationInstances getAnnotationInstances(String channelId, String annotationType) {
        return mDatabase.getAnnotationInstances(channelId, annotationType);
    }

    /**
     * Search persisted Message text with a query.
     *
     * @param channelId the id of the Channel from which Messages will be retrieved
     * @param query the search query
     * @return OrderedMessageBatch
     */
    public OrderedMessageBatch searchMessagesWithQuery(String channelId, String query) {
        return searchMessagesWithQuery(channelId, query, null);
    }

    /**
     * Search persisted Message text with a query, using a MessageFilter to excluded some results.
     *
     * @param channelId the id of the Channel from which Messages will be retrieved
     * @param query the search query
     * @param messageFilter the MessageFilter to use to exclude results
     * @return OrderedMessageBatch
     */
    public OrderedMessageBatch searchMessagesWithQuery(String channelId, String query, MessageFilter messageFilter) {
        OrderedMessageBatch orderedMessageBatch = mDatabase.searchForMessages(channelId, query);
        if(messageFilter != null) {
            orderedMessageBatch = FilteredMessageBatch.getFilteredMessageBatch(orderedMessageBatch, messageFilter);
        }
        performLookups(orderedMessageBatch.getMessages().values(), false);
        return orderedMessageBatch;
    }

    /**
     * Search for persisted Messages, using a query that matches against their associated DisplayLocations.
     *
     * @param channelId the id of the Channel from which Messages will be retrieved
     * @param query the search query
     * @return OrderedMessageBatch
     */
    public OrderedMessageBatch searchMessagesWithDisplayLocationQuery(String channelId, String query) {
        return searchMessagesWithDisplayLocationQuery(channelId, query, null);
    }

    /**
     * Search for persisted Messages, using a query that matches against their associated DisplayLocations.
     * The provided filter will exclude unwanted results.
     *
     * @param channelId the id of the Channel from which Messages will be retrieved
     * @param query the search query
     * @param messageFilter the MessageFilter to use to exclude results.
     * @return OrderedMessageBatch
     */
    public OrderedMessageBatch searchMessagesWithDisplayLocationQuery(String channelId, String query, MessageFilter messageFilter) {
        OrderedMessageBatch orderedMessageBatch = mDatabase.searchForMessagesByDisplayLocation(channelId, query);
        if(messageFilter != null) {
            orderedMessageBatch = FilteredMessageBatch.getFilteredMessageBatch(orderedMessageBatch, messageFilter);
        }
        performLookups(orderedMessageBatch.getMessages().values(), false);
        return orderedMessageBatch;
    }

    private void lookupLocation(Collection<MessagePlus> messages, boolean persist) {
        for(MessagePlus messagePlus : messages) {
            Message message = messagePlus.getMessage();

            Annotation checkin = message.getFirstAnnotationOfType(Annotations.CHECKIN);
            if(checkin != null) {
                DisplayLocation displayLocation = DisplayLocation.fromCheckinAnnotation(mContext, checkin);
                if(displayLocation != null) {
                    messagePlus.setDisplayLocation(displayLocation);
                    if(persist) {
                        mDatabase.insertOrReplaceDisplayLocationInstance(messagePlus);
                    }
                    continue;
                }
            }

            Annotation ohaiLocation = message.getFirstAnnotationOfType(Annotations.OHAI_LOCATION);
            if(ohaiLocation != null) {
                messagePlus.setDisplayLocation(DisplayLocation.fromOhaiLocation(ohaiLocation));
                if(persist) {
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
                    if(persist) {
                        mDatabase.insertOrReplaceDisplayLocationInstance(messagePlus);
                    }
                    continue;
                } else {
                    reverseGeocode(messagePlus, latitude, longitude, persist);
                }
            }
        }
    }

    private void reverseGeocode(final MessagePlus messagePlus, final double latitude, final double longitude, final boolean persist) {
        if(Geocoder.isPresent()) {
            AsyncGeocoder.getInstance(mContext).getFromLocation(latitude, longitude, 5, new AsyncGeocoderResponseHandler() {
                @Override
                public void onSuccess(final List<Address> addresses) {
                    Geolocation geolocation = getGeoLocation(addresses, latitude, longitude);
                    if(geolocation != null) {
                        messagePlus.setDisplayLocation(DisplayLocation.fromGeolocation(geolocation));

                        if(persist) {
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

    /**
     * Get the AppDotNetClient used by this MessageManager
     *
     * @return AppDotNetClient
     */
    public AppDotNetClient getClient() {
        return mClient;
    }

    /**
     * Get a List of all MessagePlus objects currently loaded into memory for a specific Channel
     *
     * @param channelId the Channel id
     * @return a List of MessagePlus objects
     */
    public List<MessagePlus> getMessageList(String channelId) {
        Map<Long, MessagePlus> messageMap = mMessages.get(channelId);
        if(messageMap == null) {
            return null;
        }
        MessagePlus[] messages = messageMap.values().toArray(new MessagePlus[0]);
        return Arrays.asList(messages);
    }

    /**
     * Get a TreeMap that maps Message times in millis to MessagePlus objects, containing
     * entries for all Messages currently loaded into memory in a specific Channel.
     *
     * Note that modifications to this Map can alter the functionality of the MessageManager,
     * so in most cases you probably should be using getMessageList()
     *
     * @param channelId the Channel id
     * @return the TreeMap of MessagePlus objects currently loaded into memory for the
     * specified channel
     */
    public TreeMap<Long, MessagePlus> getMessageMap(String channelId) {
        return mMessages.get(channelId);
    }

    /**
     * Set the QueryParameters to be used with a specific Channel
     *
     * @param channelId the id of the Channel
     * @param parameters QueryParameters
     */
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

    private synchronized TreeMap<Long, MessagePlus> getChannelMessages(String channelId) {
        TreeMap<Long, MessagePlus> channelMessages = mMessages.get(channelId);
        if(channelMessages == null) {
            channelMessages = new TreeMap<Long, MessagePlus>(new ReverseChronologicalComparator());
            mMessages.put(channelId, channelMessages);
        }
        return channelMessages;
    }

    private synchronized TreeMap<Long, MessagePlus> getUnsentMessages(String channelId) {
        TreeMap<Long, MessagePlus> unsentMessages = mUnsentMessages.get(channelId);
        if(unsentMessages == null) {
            unsentMessages = mDatabase.getUnsentMessages(channelId);
            mUnsentMessages.put(channelId, unsentMessages);
        }
        return unsentMessages;
    }

    private synchronized Set<String> getMessageIdsNeedingPendingFile(String pendingFileId) {
        Set<String> messageIds = mMessagesNeedingPendingFiles.get(pendingFileId);
        if(messageIds == null) {
            messageIds = mDatabase.getMessagesDependentOnPendingFile(pendingFileId);
            mMessagesNeedingPendingFiles.put(pendingFileId, messageIds);
        }
        return messageIds;
    }

    /**
     * Retrieve messages in the specified channel.
     *
     * The since_id and before_id used in the request are based off the ids of the messages
     * currently loaded into memory for this channel. For this reason, you should probably be
     * exhausting the results of loadPersistedMessages() before calling this method.
     *
     * If false is returned, there are unsent messages or pending deletions that must be sent before retrieving.
     *
     * @param channelId The id of the channel for which more messages should be obtained
     * @param handler The handler that will deliver the result of this request
     * @return return false if unsent messages exist and we are unable to retrieve more, true otherwise.
     *
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#loadPersistedMessages(String, int)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendUnsentMessages(String)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendPendingDeletions(String, com.alwaysallthetime.messagebeast.manager.MessageManager.MessageDeletionResponseHandler)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendAllUnsent(String)
     */
    public synchronized boolean retrieveMessages(String channelId, MessageManagerResponseHandler handler) {
        return retrieveMessages(channelId, null, handler);
    }

    /**
     * Retrieve messages in the specified channel, using a MessageFilter to exclude messages
     * in the response. Excluded Messages will not be persisted and will not be returned by the
     * provided response handler.
     *
     * The since_id and before_id used in the request are based off the ids of the messages
     * currently loaded into memory for this channel. For this reason, you should probably be
     * exhausting the results of loadPersistedMessages() before calling this method.
     *
     * If false is returned, there are unsent messages or pending deletions that must be sent before retrieving.
     *
     * @param channelId The id of the channel for which more messages should be obtained
     * @param filter The MessageFilter to be used when retrieving messages.
     * @param handler The handler that will deliver the result of this request
     * @return return false if unsent messages exist and we are unable to retrieve more, true otherwise.
     *
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#loadPersistedMessages(String, int)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendUnsentMessages(String)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendPendingDeletions(String, com.alwaysallthetime.messagebeast.manager.MessageManager.MessageDeletionResponseHandler)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendAllUnsent(String)
     */
    public synchronized boolean retrieveMessages(String channelId, MessageFilter filter, MessageManagerResponseHandler handler) {
        MinMaxPair minMaxPair = getMinMaxPair(channelId);
        return retrieveMessages(channelId, minMaxPair.maxId, minMaxPair.minId, filter, handler);
    }

    /**
     * Retrieve more messages in the specified channel, using a MessageFilter to exclude messages
     * in the response. Excluded Messages will not be persisted and will not be returned by the
     * provided response handler.
     *
     * The since_id used in this request is based off the ids of the messages
     * currently loaded into memory for this channel. For this reason, you should probably be calling
     * loadPersistedMessages() at least once so that the max message Id is known.
     *
     * If false is returned, there are unsent messages or pending deletions that must be sent before retrieving.
     *
     * @param channelId The id of the channel for which more messages should be obtained
     * @param messageFilter The MessageFilter to be used when retrieving messages.
     * @param handler The handler that will deliver the result of this request
     * @return return false if unsent messages exist and we are unable to retrieve more, true otherwise.
     *
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#loadPersistedMessages(String, int)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendUnsentMessages(String)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendPendingDeletions(String, com.alwaysallthetime.messagebeast.manager.MessageManager.MessageDeletionResponseHandler)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendAllUnsent(String)
     */
    public synchronized boolean retrieveNewestMessages(String channelId, MessageFilter messageFilter, MessageManagerResponseHandler handler) {
        return retrieveMessages(channelId, getMinMaxPair(channelId).maxId, null, messageFilter, handler);
    }

    /**
     * Retrieve the newest messages in the specified channel.
     *
     * The since_id used in this request is based off the ids of the messages
     * currently loaded into memory for this channel. For this reason, you should probably be calling
     * loadPersistedMessages() at least once so that the max message Id is known.
     *
     * If false is returned, there are unsent messages or pending deletions that must be sent before retrieving.
     *
     * @param channelId The id of the channel for which more messages should be obtained
     * @param handler The handler that will deliver the result of this request
     * @return return false if unsent messages exist and we are unable to retrieve more, true otherwise.
     *
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#loadPersistedMessages(String, int)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendUnsentMessages(String)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendPendingDeletions(String, com.alwaysallthetime.messagebeast.manager.MessageManager.MessageDeletionResponseHandler)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendAllUnsent(String)
     */
    public synchronized boolean retrieveNewestMessages(String channelId, MessageManagerResponseHandler handler) {
        return retrieveNewestMessages(channelId, null, handler);
    }

    /**
     * Retrieve the more (older) messages in the specified channel, using a MessageFilter to exclude messages
     * in the response. Excluded Messages will not be persisted and will not be returned by the
     * provided response handler.
     *
     * The before_id used in this request is based off the ids of the messages
     * currently loaded into memory for this channel. For this reason, you should probably be
     * exhausting the results of loadPersistedMessages() before calling this method.
     *
     * If false is returned, there are unsent messages or pending deletions that must be sent before retrieving.
     *
     * @param channelId The id of the channel for which more messages should be obtained
     * @param filter The MessageFilter to be used when retrieving messages.
     * @param handler The handler that will deliver the result of this request
     * @return return false if unsent messages exist and we are unable to retrieve more, true otherwise.
     *
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#loadPersistedMessages(String, int)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendUnsentMessages(String)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendPendingDeletions(String, com.alwaysallthetime.messagebeast.manager.MessageManager.MessageDeletionResponseHandler)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendAllUnsent(String)
     */
    public synchronized boolean retrieveMoreMessages(String channelId, MessageFilter filter, MessageManagerResponseHandler handler) {
        return retrieveMessages(channelId, null, getMinMaxPair(channelId).minId, filter, handler);
    }

    /**
     * Retrieve the more (older) messages in the specified channel.
     *
     * The before_id used in this request is based off the ids of the messages
     * currently loaded into memory for this channel. For this reason, you should probably be exhausting
     * the results of loadPersistedMessages() before calling this method.
     *
     * If false is returned, there are unsent messages or pending deletions that must be sent before retrieving.
     *
     * @param channelId The id of the channel for which more messages should be obtained
     * @param handler The handler that will deliver the result of this request
     * @return return false if unsent messages exist and we are unable to retrieve more, true otherwise.
     *
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#loadPersistedMessages(String, int)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendUnsentMessages(String)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendPendingDeletions(String, com.alwaysallthetime.messagebeast.manager.MessageManager.MessageDeletionResponseHandler)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendAllUnsent(String)
     */
    public synchronized boolean retrieveMoreMessages(String channelId, MessageManagerResponseHandler handler) {
        return retrieveMoreMessages(channelId, null, handler);
    }

    /**
     * Create a new Message in the Channel with the specified id.
     *
     * A runtime exception will be thrown if you have any unsent messages that need to be sent. If
     * this is the case, you should probably be calling createUnsentMessageAndAttemptSend() anyway/instead.
     *
     * @param channelId the id of the Channel in which the Message should be created.
     * @param message The Message to be created.
     * @param handler The handler that will deliver the result of this request
     */
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

    /**
     * Create a new unsent Message in the channel with the specified id and attempt to send.
     *
     * If the Message cannot be sent (e.g. no internet connection), it will still be stored in the
     * sqlite database as if the Message exists in the Channel, but with an unsent flag set on it.
     * Any number of unsent messages can exist, but no more messages can be retrieved until all
     * unsent messages have been successfully sent (or deleted).
     *
     * Upon completion of the send request, a broadcast will be sent with either the action
     * INTENT_ACTION_UNSENT_MESSAGES_SENT or INTENT_ACTION_UNSENT_MESSAGE_SEND_FAILURE.
     *
     * @param channelId the id of the Channel in which the Message should be created.
     * @param message The Message to be created.
     *
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendUnsentMessages(String)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendPendingDeletions(String, com.alwaysallthetime.messagebeast.manager.MessageManager.MessageDeletionResponseHandler)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendAllUnsent(String)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#INTENT_ACTION_UNSENT_MESSAGES_SENT
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#INTENT_ACTION_UNSENT_MESSAGE_SEND_FAILURE
     */
    public synchronized MessagePlus createUnsentMessageAndAttemptSend(final String channelId, Message message) {
        return createUnsentMessageAndAttemptSend(channelId, message, new ArrayList<PendingFileAttachment>(0));
    }

    /**
     * Create a new unsent Message that requires pending files to be uploaded prior to creation.
     *
     * If the Message cannot be sent (e.g. no internet connection), it will still be stored in the
     * sqlite database as if the Message exists in the Channel, but with an unsent flag set on it.
     * Any number of unsent messages can exist, but no more messages can be retrieved until all
     * unsent messages have been successfully sent (or deleted).
     *
     * Upon completion of the send request, a broadcast will be sent with either the action
     * INTENT_ACTION_UNSENT_MESSAGES_SENT or INTENT_ACTION_UNSENT_MESSAGE_SEND_FAILURE.
     *
     * @param channelId the id of the Channel in which the Message should be created.
     * @param message The Message to be created.
     * @param pendingFileAttachments The pending files that need to be sent before this Message can
     *                               be sent to the server.
     *
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendUnsentMessages(String)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendPendingDeletions(String, com.alwaysallthetime.messagebeast.manager.MessageManager.MessageDeletionResponseHandler)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#sendAllUnsent(String)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#INTENT_ACTION_UNSENT_MESSAGES_SENT
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#INTENT_ACTION_UNSENT_MESSAGE_SEND_FAILURE
     */
    public synchronized MessagePlus createUnsentMessageAndAttemptSend(final String channelId, Message message, List<PendingFileAttachment> pendingFileAttachments) {
        //An unsent message id is always set to the max id + 1.
        //
        //This will work because we will never allow message retrieval to happen
        //until unsent messages are sent to the server and they get their "real"
        //message id. After they reach the server, we will delete them from existence
        //on the client and retrieve them from the server.
        //
        TreeMap<Long, MessagePlus> channelMessages = getChannelMessages(channelId);
        if(channelMessages.size() == 0) {
            //we do this so that the current max date for this channel is known.
            loadPersistedMessages(channelId, 1);
        }

        Integer maxInteger = mDatabase.getMaxMessageId();
        Integer newMessageId = maxInteger != null ? maxInteger + 1 : 1;
        String newMessageIdString = String.valueOf(newMessageId);

        MessagePlus.UnsentMessagePlusBuilder unsentBuilder = MessagePlus.UnsentMessagePlusBuilder.newBuilder(channelId, newMessageIdString, message);
        for(PendingFileAttachment attachment : pendingFileAttachments) {
            unsentBuilder.addPendingFileAttachment(attachment);
        }
        final MessagePlus messagePlus = unsentBuilder.build();
        mDatabase.insertOrReplaceMessage(messagePlus);

        if(mConfiguration.isLocationLookupEnabled) {
            ArrayList<MessagePlus> mp = new ArrayList<MessagePlus>(1);
            mp.add(messagePlus);
            lookupLocation(mp, true);
        }

        TreeMap<Long, MessagePlus> channelUnsentMessages = getUnsentMessages(channelId);
        channelUnsentMessages.put(messagePlus.getDisplayDate().getTime(), messagePlus);

        TreeMap<Long, MessagePlus> newChannelMessages = new TreeMap<Long, MessagePlus>(new ReverseChronologicalComparator());
        newChannelMessages.put(messagePlus.getDisplayDate().getTime(), messagePlus);
        newChannelMessages.putAll(channelMessages);
        mMessages.put(channelId, newChannelMessages);

        //update the MinMaxPair
        //we can assume the new id is the max (that's how we generated it)
        //but we have to check to see if the time is min or max
        MinMaxPair minMaxPair = getMinMaxPair(channelId);
        minMaxPair.expandDateIfMinOrMax(messagePlus.getDisplayDate().getTime());
        minMaxPair.maxId = newMessageIdString;

        Log.d(TAG, "Created and stored unsent message with id " + newMessageIdString);

        sendUnsentMessages(channelId);

        return messagePlus;
    }

    /**
     * Delete a Message. If the specified Message is unsent, it will simply be deleted from the local
     * sqlite database and no server request is required.
     *
     * @param messagePlus The MessagePlus associated with the Message to be deleted
     */
    public synchronized void deleteMessage(final MessagePlus messagePlus) {
        deleteMessage(messagePlus, null);
    }

    /**
     * Delete a Message. If the specified Message is unsent, it will simply be deleted from the local
     * sqlite database and no server request is required.
     *
     * @param messagePlus The MessagePlus associated with the Message to be deleted
     * @param handler The handler that will act as a callback upon deletion.
     */
    public synchronized void deleteMessage(final MessagePlus messagePlus, final MessageDeletionResponseHandler handler) {
        deleteMessage(messagePlus, false, handler);
    }

    /**
     * Delete a Message. If the specified Message is unsent, it will simply be deleted from the local
     * sqlite database and no server request is required.
     *
     * @param messagePlus The MessagePlus associated with the Message to be deleted
     * @param deleteAssociatedFiles true if all files from OEmbed and attachment annotations should be deleted from
     *                              the server, false otherwise. This will never affect local files.
     * @param handler The handler that will act as a callback upon deletion.
     */
    public synchronized void deleteMessage(final MessagePlus messagePlus, boolean deleteAssociatedFiles, final MessageDeletionResponseHandler handler) {
        if(messagePlus.isUnsent()) {
            Message message = messagePlus.getMessage();
            String channelId = message.getChannelId();

            mDatabase.deleteMessage(messagePlus);
            getUnsentMessages(channelId).remove(messagePlus.getDisplayDate().getTime());

            deleteMessageFromChannelMapAndUpdateMinMaxPair(messagePlus);

            if(handler != null) {
                handler.onSuccess();
            }
        } else {
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    mClient.deleteMessage(messagePlus.getMessage(), new MessageResponseHandler() {
                        //note: if the message was previously deleted, then we get a 200 and
                        //onSuccess() is called. this differs from file deletion behavior -
                        //the same scenario with a file deletion would result in a 403.
                        @Override
                        public void onSuccess(Message responseData) {
                            delete();
                            mDatabase.deletePendingMessageDeletion(responseData.getId());
                            if(handler != null) {
                                handler.onSuccess();
                            }
                        }

                        @Override
                        public void onError(Exception error) {
                            super.onError(error);
                            delete();
                            mDatabase.insertOrReplacePendingMessageDeletion(messagePlus, false);
                            if(handler != null) {
                                handler.onError(error);
                            }
                        }

                        private void delete() {
                            mDatabase.deleteMessage(messagePlus); //this one because the deleted one doesn't have the entities.
                            deleteMessageFromChannelMapAndUpdateMinMaxPair(messagePlus);
                        }
                    });
                }
            };

            if(deleteAssociatedFiles) {
                List<Annotation> oEmbeds = messagePlus.getMessage().getAnnotationsOfType(Annotations.OEMBED);
                deleteOEmbed(0, oEmbeds, new Runnable() {
                    @Override
                    public void run() {
                        List<Annotation> attachments = messagePlus.getMessage().getAnnotationsOfType(Annotations.ATTACHMENTS);
                        deleteAttachmentsLists(0, attachments, runnable);
                    }
                });
            } else {
                runnable.run();
            }
        }
    }

    private synchronized void deleteOEmbed(final int index, final List<Annotation> oEmbedAnnotations, final Runnable completionRunnable) {
        if(index >= oEmbedAnnotations.size()) {
            completionRunnable.run();
        } else {
            Annotation oEmbed = oEmbedAnnotations.get(index);
            final String fileId = (String) oEmbed.getValue().get("file_id");
            if(fileId != null) {
                mClient.deleteFile(fileId, new FileResponseHandler() {
                    @Override
                    public void onSuccess(File responseData) {
                        deleteOEmbed(index + 1, oEmbedAnnotations, completionRunnable);
                    }

                    @Override
                    public void onError(Exception error) {
                        super.onError(error);
                        //statusCode == null probably means no internet
                        //statusCode 403 means it's already deleted
                        Integer statusCode = getStatusCode();
                        if(statusCode == null || statusCode != 403) {
                            mDatabase.insertOrReplacePendingFileDeletion(fileId);
                        }
                        deleteOEmbed(index + 1, oEmbedAnnotations, completionRunnable);
                    }
                });
            } else {
                deleteOEmbed(index + 1, oEmbedAnnotations, completionRunnable);
            }
        }
    }

    private synchronized void deleteAttachmentsLists(final int index, final List<Annotation> attachmentsAnnotations, final Runnable completionRunnable) {
        if(index >= attachmentsAnnotations.size()) {
            completionRunnable.run();
        } else {
            Annotation attachment = attachmentsAnnotations.get(index);
            deleteFileInAttachmentsAnnotation(0, (List<Map<String, Object>>) attachment.getValueForKey(Annotations.FILE_LIST), new Runnable() {
                @Override
                public void run() {
                    deleteAttachmentsLists(index+1, attachmentsAnnotations, completionRunnable);
                }
            });
        }
    }

    private synchronized void deleteFileInAttachmentsAnnotation(final int index, final List<Map<String, Object>> fileList, final Runnable completionRunnable) {
        if(index >= fileList.size()) {
            completionRunnable.run();
        } else {
            Map<String, Object> file = fileList.get(index);
            final String fileId = (String) file.get("file_id");
            mClient.deleteFile(fileId, new FileResponseHandler() {
                @Override
                public void onSuccess(File responseData) {
                    deleteFileInAttachmentsAnnotation(index + 1, fileList, completionRunnable);
                }

                @Override
                public void onError(Exception error) {
                    super.onError(error);
                    //statusCode == null probably means no internet
                    //statusCode 403 means it's already deleted
                    Integer statusCode = getStatusCode();
                    if(statusCode == null || statusCode != 403) {
                        mDatabase.insertOrReplacePendingFileDeletion(fileId);
                    }
                    deleteFileInAttachmentsAnnotation(index + 1, fileList, completionRunnable);
                }
            });
        }
    }

    private synchronized void deleteMessageFromChannelMapAndUpdateMinMaxPair(MessagePlus messagePlus) {
        Long removedTime = messagePlus.getDisplayDate().getTime();
        String channelId = messagePlus.getMessage().getChannelId();
        TreeMap<Long, MessagePlus> channelMessages = getChannelMessages(channelId);

        if(channelMessages.containsKey(removedTime)) {
            //
            //modify the MinMaxPair if the removed message was at the min or max date/id.
            //we know the channel messages are ordered by date, but the ids are not necessarily ordered.
            //

            MinMaxPair minMaxPair = getMinMaxPair(channelId);

            String deletedMessageId = messagePlus.getMessage().getId();
            boolean adjustMax = deletedMessageId.equals(minMaxPair.maxId);
            boolean adjustMin = deletedMessageId.equals(minMaxPair.minId);
            Integer maxIdAsInteger = minMaxPair.getMaxIdAsInteger();
            Integer minIdAsInteger = minMaxPair.getMinIdAsInteger();
            Integer newMaxId = null;
            Integer newMinId = null;

            //we have to iterate for these reasons:
            //1. ids are not in order in map, need to find new min/max
            //2. need to get to the second to last date (if the deleted message was the last)

            Iterator<Long> timeIterator = channelMessages.keySet().iterator();
            Long secondToLastDate = null;
            Long lastDate = null;

            while(timeIterator.hasNext()) {
                Long nextTime = timeIterator.next();

                //so this is the second date, and the first date was the one that was removed
                //the new max date is the second key.
                if(lastDate != null && secondToLastDate == null && lastDate.equals(removedTime) ) {
                    minMaxPair.maxDate = nextTime;
                }

                Integer nextId = Integer.parseInt(channelMessages.get(nextTime).getMessage().getId());
                if(adjustMax && maxIdAsInteger > nextId && (newMaxId == null || nextId > newMaxId)) {
                    newMaxId = nextId;
                }
                if(adjustMin && minIdAsInteger < nextId && (newMinId == null || nextId < newMinId)) {
                    newMinId = nextId;
                }
                secondToLastDate = lastDate;
                lastDate = nextTime;
            }

            //the last date was the removed one, so the new min date is the second to last date.
            if(removedTime.equals(lastDate)) {
                minMaxPair.minDate = secondToLastDate;
            }
            if(newMaxId != null) {
                minMaxPair.maxId = String.valueOf(newMaxId);
            }
            if(newMinId != null) {
                minMaxPair.minId = String.valueOf(newMinId);
            }

            //handle the edge case where there is only one item in the map, about to get removed
            if(channelMessages.size() == 1) {
                minMaxPair.maxId = null;
                minMaxPair.minId = null;
                minMaxPair.maxDate = null;
                minMaxPair.minDate = null;
            }

            channelMessages.remove(removedTime);
        }
    }

    /**
     * Obtain fresh copy of the specified Message.
     *
     * This is most useful in cases where fields have expired (e.g. file urls).
     *
     * @param message the Message to refresh.
     * @param handler The handler that will act as a callback upon refresh completion.
     */
    public synchronized void refreshMessage(final Message message, final MessageManagerResponseHandler handler) {
        final String channelId = message.getChannelId();
        mClient.retrieveMessage(channelId, message.getId(), mParameters.get(channelId), new MessageResponseHandler() {
            @Override
            public void onSuccess(Message responseData) {
                MessagePlus mPlus = new MessagePlus(responseData);
                adjustDate(mPlus);
                insertIntoDatabase(mPlus);

                TreeMap<Long, MessagePlus> channelMessages = mMessages.get(channelId);
                if(channelMessages != null) { //could be null of channel messages weren't loaded first, etc.
                    channelMessages.put(mPlus.getDisplayDate().getTime(), mPlus);
                }

                ArrayList<MessagePlus> messagePlusList = new ArrayList<MessagePlus>(1);
                messagePlusList.add(mPlus);
                performLookups(messagePlusList, true);

                handler.onSuccess(messagePlusList);
            }

            @Override
            public void onError(Exception error) {
                super.onError(error);
                handler.onError(error);
            }
        });
    }

    public synchronized void refreshMessages(Collection<String> messageIds, final String channelId, final MessageManagerResponseHandler handler) {
        mClient.retrieveMessagesById(messageIds, mParameters.get(channelId), new MessageListResponseHandler() {
            @Override
            public void onSuccess(MessageList responseData) {
                TreeMap<Long, MessagePlus> messagePlusMap = new TreeMap<Long, MessagePlus>(new ReverseChronologicalComparator());
                TreeMap<Long, MessagePlus> channelMessages = mMessages.get(channelId);

                for(Message message : responseData) {
                    MessagePlus mPlus = new MessagePlus(message);
                    Date date = adjustDate(mPlus);

                    insertIntoDatabase(mPlus);

                    if(channelMessages != null) { //could be null of channel messages weren't loaded first, etc.
                        channelMessages.put(mPlus.getDisplayDate().getTime(), mPlus);
                    }

                    messagePlusMap.put(mPlus.getDisplayDate().getTime(), mPlus);
                }

                ArrayList<MessagePlus> messagePlusses = new ArrayList<MessagePlus>(messagePlusMap.values());
                performLookups(messagePlusses, true);
                handler.onSuccess(messagePlusses);
            }

            @Override
            public void onError(Exception error) {
                super.onError(error);
                handler.onError(error);
            }
        });
    }

    /**
     * Get the FullSyncState for the Channel with the specified id.
     *
     * @param channelId the channel id
     * @return a FullSyncState corresponding to the sync state of the Channel
     *         with the specified id
     */
    public FullSyncState getFullSyncState(String channelId) {
        return ADNSharedPreferences.getFullSyncState(channelId);
    }

    /**
     * Set the FullSyncState for the Channel with the specified id.
     *
     * @param channelId the channel id
     * @param state the FullSyncState to associate with the Channel.
     */
    public void setFullSyncState(String channelId, FullSyncState state) {
        ADNSharedPreferences.setFullSyncState(channelId, state);
    }

    /**
     * Get a FullSyncState representing the sync state of multiple channels.
     * In some cases, we might need several channels to be synced, and one
     * or more of them may be in a NOT_STARTED or STARTED state. Since we typically
     * would not need to disclose granular details about the sync state of
     * many channels to a user, this method will return STARTED if *any* channel
     * in the provided array is in the STARTED state. Otherwise, NOT_STARTED will
     * be returned if any of the channels is not COMPLETE.
     *
     * @param channels
     * @return A FullSyncState representing the sync state of the provided group of Channels.
     */
    public synchronized FullSyncState getFullSyncState(Channel[] channels) {
        FullSyncState state = FullSyncState.COMPLETE;
        for(Channel channel : channels) {
            if(getFullSyncState(channel.getId()) == FullSyncState.STARTED) {
                return FullSyncState.STARTED;
            } else if(getFullSyncState(channel.getId()) == FullSyncState.NOT_STARTED) {
                state = FullSyncState.NOT_STARTED;
            }
        }
        return state;
    }


    /**
     * Sync and persist all Messages for every provided Channel.
     *
     * Each Channel is synced - one at a time. If an Action Channel is encountered, the
     * ActionMessageManager is used to sync the Messages in that Channel. Unlike the
     * other retrieveAndPersistAllMessages method that accepts a single Channel id, this
     * method examines the FullSyncState for a Channel and skips it if it is marked COMPLETE.
     *
     * @param channels
     * @param responseHandler
     */
    public synchronized void retrieveAndPersistAllMessages(Channel[] channels, MessageManagerMultiChannelSyncResponseHandler responseHandler) {
        int i = 0;
        while(i < channels.length && getFullSyncState(channels[i].getId()) == FullSyncState.COMPLETE) {
            i++;
        }
        if(i == channels.length) {
            responseHandler.onSuccess();
        } else {
            retrieveAndPersistAllMessages(channels, i, responseHandler);
        }
    }

    private synchronized void retrieveAndPersistAllMessages(final Channel[] channels, final int currentChannelIndex, final MessageManagerMultiChannelSyncResponseHandler responseHandler) {
        MessageManagerSyncResponseHandler currentChannelSyncHandler = new MessageManagerSyncResponseHandler() {
            @Override
            public void onSuccess(List<MessagePlus> responseData) {
                int i = currentChannelIndex + 1;
                while(i < channels.length && getFullSyncState(channels[i].getId()) == FullSyncState.COMPLETE) {
                    i++;
                }
                if(i == channels.length) {
                    responseHandler.onSuccess();
                } else {
                    retrieveAndPersistAllMessages(channels, i, responseHandler);
                }
            }

            @Override
            public void onError(Exception exception) {
                Log.e(TAG, exception.getMessage(), exception);
                responseHandler.onError(exception);
            }
        };

        Channel nextChannel = channels[currentChannelIndex];
        String type = nextChannel.getType();
        if(PrivateChannelUtility.CHANNEL_TYPE_ACTION.equals(type)) {
            ActionMessageManager.getInstance(MessageManager.this).retrieveAndPersistAllActionMessages(nextChannel.getId(), currentChannelSyncHandler);
        } else {
            retrieveAndPersistAllMessages(channels[currentChannelIndex].getId(), currentChannelSyncHandler);
        }
    }

    /**
     * Sync and persist all Messages in a Channel.
     *
     * This is intended to be used as a one-time sync, e.g. after a user signs in. Typically, you
     * should call getFullSyncState() to check whether this channel has already been synced before
     * calling this method.
     *
     * Because this could potentially result in a very large amount of Messages being obtained,
     * the provided MessageManagerResponseHandler will only be passed the first 100 Messages that are
     * obtained, while the others will be persisted to the sqlite database, but not kept in memory.
     * However, these can easily be loaded into memory afterwards by calling loadPersistedMessages().
     *
     * @param channelId The id of the Channel from which to obtain Messages.
     * @param responseHandler MessageManagerResponseHandler
     *
     * @see MessageManager#loadPersistedMessages(String, int)
     */
    public synchronized void retrieveAndPersistAllMessages(String channelId, MessageManagerSyncResponseHandler responseHandler) {
        ADNSharedPreferences.setFullSyncState(channelId, FullSyncState.STARTED);
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

        boolean keepInMemory = messages.size() == 0;
        retrieveMessages(params, null, channelId, keepInMemory, new MessageManagerResponseHandler() {
            @Override
            public void onSuccess(List<MessagePlus> responseData) {
                if(messages.size() == 0) {
                    messages.addAll(responseData);
                }
                responseHandler.setNumMessagesSynced(responseHandler.getNumMessagesSynced() + responseData.size());
                responseHandler.onBatchSynced(responseData);

                if(isMore()) {
                    //never rely on MinMaxPair for min id here because
                    //when keepInMemory = false, the MinMaxPair will not change
                    //(and this would keep requesting the same batch over and over).
                    MessagePlus minMessage = responseData.get(responseData.size() - 1);
                    retrieveAllMessages(messages, null, minMessage.getMessage().getId(), channelId, responseHandler);
                } else {
                    ADNSharedPreferences.setFullSyncState(channelId, FullSyncState.COMPLETE);
                    Log.d(TAG, "Num messages synced: " + responseHandler.getNumMessagesSynced());
                    responseHandler.onSuccess(messages);
                }
            }

            @Override
            public void onError(Exception exception) {
                Log.e(TAG, exception.getMessage(), exception);
                responseHandler.onError(exception);
            }
        });
    }

    private synchronized boolean retrieveMessages(final String channelId, final String sinceId, final String beforeId, final MessageFilter messageFilter, final MessageManagerResponseHandler handler) {
        QueryParameters params = (QueryParameters) mParameters.get(channelId).clone();
        params.put("since_id", sinceId);
        params.put("before_id", beforeId);
        return retrieveMessages(params, messageFilter, channelId, true, handler);
    }

    private synchronized void sendUnsentMessages(final TreeMap<Long, MessagePlus> unsentMessages, final ArrayList<String> sentMessageIds) {
        final MessagePlus messagePlus = unsentMessages.get(unsentMessages.keySet().iterator().next());
        if(messagePlus.hasPendingFileAttachments()) {
            String pendingFileId = messagePlus.getPendingFileAttachments().keySet().iterator().next();
            Set<String> messagesNeedingPendingFile = getMessageIdsNeedingPendingFile(pendingFileId);
            messagesNeedingPendingFile.add(messagePlus.getMessage().getId());
            FileManager.getInstance(mClient).startPendingFileUpload(pendingFileId, messagePlus.getMessage().getChannelId());
            return;
        }
        Message theMessage = messagePlus.getMessage();
        final Message message = (Message) AppDotNetObjectCloner.getClone(theMessage);

        //we had them set for display locally, but we should
        //let the server generate the "real" entities.
        message.setEntities(null);

        mClient.createMessage(message.getChannelId(), message, new MessageResponseHandler() {
            @Override
            public void onSuccess(Message responseData) {
                Log.d(TAG, "Successfully sent unsent message with id " + message.getId());

                long sentMessageTime = messagePlus.getDisplayDate().getTime();

                unsentMessages.remove(sentMessageTime);
                sentMessageIds.add(message.getId());

                mDatabase.deleteMessage(messagePlus);

                deleteMessageFromChannelMapAndUpdateMinMaxPair(messagePlus);

                if(unsentMessages.size() > 0) {
                    sendUnsentMessages(unsentMessages, sentMessageIds);
                } else {
                    Intent i = new Intent(INTENT_ACTION_UNSENT_MESSAGES_SENT);
                    i.putExtra(EXTRA_CHANNEL_ID, message.getChannelId());
                    i.putStringArrayListExtra(EXTRA_SENT_MESSAGE_IDS, sentMessageIds);
                    mContext.sendBroadcast(i);
                }
            }

            @Override
            public void onError(Exception exception) {
                super.onError(exception);
                int sendAttempts = messagePlus.incrementSendAttempts();
                mDatabase.insertOrReplaceMessage(messagePlus);

                Intent i = new Intent(INTENT_ACTION_UNSENT_MESSAGE_SEND_FAILURE);
                i.putExtra(EXTRA_CHANNEL_ID, message.getChannelId());
                i.putExtra(EXTRA_MESSAGE_ID, message.getId());
                i.putExtra(EXTRA_SEND_ATTEMPTS, sendAttempts);
                mContext.sendBroadcast(i);
            }
        });
    }

    /**
     * Return true if the Channel with the specified id has unsent Messages.
     *
     * @param channelId the Channel id
     * @return true if the Channel has unsent Messages, false otherwise
     */
    public synchronized boolean hasUnsentMessages(String channelId) {
        return getUnsentMessages(channelId).size() > 0;
    }

    /**
     * Send all pending deletions and unsent Messages in a Channel.
     *
     * The pending deletions will be sent first.
     *
     * @param channelId the Channel id
     */
    public synchronized void sendAllUnsent(final String channelId) {
        FileManager.getInstance(mClient).sendPendingFileDeletions();
        sendPendingDeletions(channelId, new MessageDeletionResponseHandler() {
            @Override
            public void onSuccess() {
                sendUnsentMessages(channelId);
            }

            @Override
            public void onError(Exception exception) {
                Log.e(TAG, exception.getMessage(), exception);

                //try this anyway.
                sendUnsentMessages(channelId);
            }
        });
    }

    /**
     * Send all unsent Messages in a Channel.
     *
     * @param channelId the the Channel id
     * @return true if unsent Messages are being sent, false if none exist
     */
    public synchronized boolean sendUnsentMessages(final String channelId) {
        TreeMap<Long, MessagePlus> unsentMessages = getUnsentMessages(channelId);
        if(unsentMessages.size() > 0) {
            TreeMap<Long, MessagePlus> channelMessages = getChannelMessages(channelId);
            if(channelMessages.size() == 0) {
                //we do this so that the max id for this channel is known.
                loadPersistedMessages(channelId, unsentMessages.size() + 1);
            }
            ArrayList<String> sentMessageIds = new ArrayList<String>(unsentMessages.size());
            sendUnsentMessages(unsentMessages, sentMessageIds);
            return true;
        }
        return false;
    }

    /**
     * Send all pending Message deletions in a Channel.
     *
     * @param channelId the Channel id
     * @param responseHandler MessageDeletionResponseHandler
     */
    public synchronized void sendPendingDeletions(final String channelId, MessageDeletionResponseHandler responseHandler) {
        HashMap<String, PendingMessageDeletion> pendingMessageDeletions = mDatabase.getPendingMessageDeletions(channelId);
        if(pendingMessageDeletions.size() > 0) {
            ArrayList<PendingMessageDeletion> deletions = new ArrayList<PendingMessageDeletion>(pendingMessageDeletions.values());
            sendPendingDeletion(0, deletions, responseHandler);
        } else if(responseHandler != null) {
            responseHandler.onSuccess();
        }
    }

    private synchronized void sendPendingDeletion(final int index, final List<PendingMessageDeletion> pendingMessageDeletions, final MessageDeletionResponseHandler responseHandler) {
        if(index >= pendingMessageDeletions.size()) {
            if(responseHandler != null) {
                responseHandler.onSuccess();
            }
        } else {
            PendingMessageDeletion deletion = pendingMessageDeletions.get(index);
            final MessagePlus messagePlus = mDatabase.getMessage(deletion.getMessageId());
            if(messagePlus != null) {
                deleteMessage(messagePlus, deletion.deleteAssociatedFiles(), new MessageDeletionResponseHandler() {
                    @Override
                    public void onSuccess() {
                        mDatabase.deletePendingMessageDeletion(messagePlus.getMessage().getId());
                        sendPendingDeletion(index + 1, pendingMessageDeletions, responseHandler);
                    }

                    @Override
                    public void onError(Exception error) {
                        Log.e(TAG, "failed to delete message " + messagePlus.getMessage().getId() + "; " + error.getMessage(), error);
                        if(responseHandler != null) {
                            responseHandler.onError(error);
                        }
                    }
                });
            } else {
                Log.e(TAG, "no persisted Message was found for PendingMessageDeletion with id " + deletion.getMessageId());
                mDatabase.deletePendingMessageDeletion(messagePlus.getMessage().getId());
                sendPendingDeletion(index + 1, pendingMessageDeletions, responseHandler);
            }
        }
    }

    private synchronized boolean retrieveMessages(final QueryParameters queryParameters,
                                                  final MessageFilter filter,
                                                  final String channelId,
                                                  final boolean keepInMemory,
                                                  final MessageManagerResponseHandler handler) {
        TreeMap<Long, MessagePlus> unsentMessages = getUnsentMessages(channelId);
        HashMap<String, PendingMessageDeletion> pendingMessageDeletions = mDatabase.getPendingMessageDeletions(channelId);
        if(unsentMessages.size() > 0 || pendingMessageDeletions.size() > 0) {
            return false;
        }
        mClient.retrieveMessagesInChannel(channelId, queryParameters, new MessageListResponseHandler() {
            @Override
            public void onSuccess(final MessageList responseData) {
                TreeMap<Long, MessagePlus> channelMessages = getChannelMessages(channelId);
                TreeMap<Long, MessagePlus> newestMessagesMap = new TreeMap<Long, MessagePlus>(new ReverseChronologicalComparator());
                TreeMap<Long, MessagePlus> newFullChannelMessagesMap = new TreeMap<Long, MessagePlus>(new ReverseChronologicalComparator());

                newFullChannelMessagesMap.putAll(channelMessages);

                for(Message m : responseData) {
                    MessagePlus messagePlus = new MessagePlus(m);
                    Date date = adjustDate(messagePlus);
                    newestMessagesMap.put(date.getTime(), messagePlus);
                    newFullChannelMessagesMap.put(date.getTime(), messagePlus);
                }

                if(filter != null) {
                    TreeMap<Long, MessagePlus> excludedResults = filter.getExcludedResults(newestMessagesMap);
                    removeExcludedMessages(newFullChannelMessagesMap, excludedResults);
                    if(handler != null) {
                        handler.setExcludedResults(excludedResults);
                    }
                }

                Long minDate = null, maxDate = null;

                //this needs to happen after filtering.
                //damn. not as efficient as doing it in the loop above.
                for(MessagePlus messagePlus : newestMessagesMap.values()) {
                    insertIntoDatabase(messagePlus);

                    Long time = messagePlus.getDisplayDate().getTime();
                    if(minDate == null || time < minDate) {
                        minDate = time;
                    }
                    if(maxDate == null || time > maxDate) {
                        maxDate = time;
                    }
                }

                if(keepInMemory) {
                    mMessages.put(channelId, newFullChannelMessagesMap);

                    MinMaxPair minMaxPair = getMinMaxPair(channelId);
                    minMaxPair.updateWithCombinedValues(new MinMaxPair(getMinId(), getMaxId(), minDate, maxDate));
                }

                ArrayList<MessagePlus> newestMessages = new ArrayList<MessagePlus>(responseData.size());
                newestMessages.addAll(newestMessagesMap.values());
                performLookups(newestMessages, true);

                if(handler != null) {
                    handler.setIsMore(isMore());
                    handler.onSuccess(newestMessages);
                }
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, error.getMessage(), error);

                if(handler != null) {
                    handler.onError(error);
                }
            }
        });
        return true;
    }

    private Date adjustDate(MessagePlus messagePlus) {
        Date adjustedDate = getAdjustedDate(messagePlus.getMessage());
        messagePlus.setDisplayDate(adjustedDate);
        return adjustedDate;
    }

    private void insertIntoDatabase(MessagePlus messagePlus) {
        mDatabase.insertOrReplaceMessage(messagePlus);
        mDatabase.insertOrReplaceHashtagInstances(messagePlus);

        if(mConfiguration.annotationsToExtract != null) {
            for(String annotationType : mConfiguration.annotationsToExtract) {
                mDatabase.insertOrReplaceAnnotationInstances(annotationType, messagePlus);
            }
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

    private void performLookups(Collection<MessagePlus> messages, boolean persist) {
        if(mConfiguration.isLocationLookupEnabled) {
            lookupLocation(messages, persist);
        }
    }

    private void removeExcludedMessages(TreeMap<Long, MessagePlus> fromMap, TreeMap<Long, MessagePlus> removedEntries) {
        Iterator<Long> filteredTimeIterator = removedEntries.keySet().iterator();
        while(filteredTimeIterator.hasNext()) {
            fromMap.remove(filteredTimeIterator.next());
        }
    }

    private final BroadcastReceiver fileUploadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(FileUploadService.INTENT_ACTION_FILE_UPLOAD_COMPLETE.equals(intent.getAction())) {
                String pendingFileId = intent.getStringExtra(FileUploadService.EXTRA_PENDING_FILE_ID);
                String associatedChannelId = intent.getStringExtra(FileUploadService.EXTRA_ASSOCIATED_CHANNEL_ID);
                if(pendingFileId != null) {
                    boolean success = intent.getBooleanExtra(FileUploadService.EXTRA_SUCCESS, false);
                    if(success) {
                        Log.d(TAG, "Successfully uploaded pending file with id " + pendingFileId);

                        Set<String> messagesIdsNeedingFile = getMessageIdsNeedingPendingFile(pendingFileId);
                        if(messagesIdsNeedingFile != null) {
                            TreeMap<Long, MessagePlus> messagesNeedingFile = mDatabase.getMessages(messagesIdsNeedingFile).getMessages();

                            //always add the associated channel Id so that we can finish the
                            //sending of the unsent message in the channel that triggered the upload.
                            HashSet<String> channelIdsWithMessagesToSend = new HashSet<String>();
                            channelIdsWithMessagesToSend.add(associatedChannelId);

                            String fileJson = intent.getStringExtra(FileUploadService.EXTRA_FILE);
                            File file = AppDotNetGson.getPersistenceInstance().fromJson(fileJson, File.class);

                            for(MessagePlus messagePlus : messagesNeedingFile.values()) {
                                Message message = messagePlus.getMessage();
                                messagePlus.replacePendingFileAttachmentWithAnnotation(pendingFileId, file);

                                //TODO: this is kind of crappy, but needs to be done
                                //modify the in-memory message plusses to use this new copy
                                //in the future, we might want to change the way unsent messages
                                //are held on to in memory.
                                String messageId = message.getId();
                                String channelId = message.getChannelId();
                                long time = messagePlus.getDisplayDate().getTime();
                                TreeMap<Long, MessagePlus> channelMessages = getChannelMessages(channelId);
                                if(channelMessages.containsKey(time)) {
                                    channelMessages.put(time, messagePlus);
                                }
                                TreeMap<Long, MessagePlus> unsentMessages = getUnsentMessages(channelId);
                                if(unsentMessages.containsKey(time)) {
                                    unsentMessages.put(time, messagePlus);
                                }

                                mDatabase.insertOrReplaceMessage(messagePlus);
                                mDatabase.deletePendingFileAttachment(pendingFileId, message.getId());

                                if(messagePlus.getPendingFileAttachments().size() == 0) {
                                    channelIdsWithMessagesToSend.add(channelId);
                                }
                            }

                            mMessagesNeedingPendingFiles.remove(pendingFileId);

                            for(String channelId : channelIdsWithMessagesToSend) {
                                sendUnsentMessages(channelId);
                                Log.d(TAG, "Now retrying send for unsent messages in channel " + channelId);
                            }
                        }
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

        boolean isLocationLookupEnabled;
        MessageDisplayDateAdapter dateAdapter;
        MessageLocationLookupHandler locationLookupHandler;
        Set<String> annotationsToExtract;

        /**
         * Set a MessageDisplayDateAdapter.
         *
         * @param adapter
         * @see com.alwaysallthetime.messagebeast.manager.MessageManager.MessageDisplayDateAdapter
         */
        public void setMessageDisplayDateAdapter(MessageDisplayDateAdapter adapter) {
            this.dateAdapter = adapter;
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
         * @see com.alwaysallthetime.messagebeast.model.MessagePlus#getDisplayLocation()
         * @see com.alwaysallthetime.messagebeast.model.MessagePlus#hasDisplayLocation()
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

        /**
         * Tell the MessageManager to examine the Annotations on all Messages to see if
         * Annotations with the specified type exist. If so, a reference to the Message will be
         * persisted to the sqlite database for lookup at a later time. For example, if you
         * want to be able to find all Messages with OEmbeds at a later time, then
         * you might call this method with the annotation type net.app.core.oembed.
         *
         * @param annotationType The Annotation type of interest.
         */
        public void addAnnotationExtraction(String annotationType) {
            if(annotationsToExtract == null) {
                annotationsToExtract = new HashSet<String>();
            }
            annotationsToExtract.add(annotationType);
        }
    }
}
