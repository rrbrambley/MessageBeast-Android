package com.alwaysallthetime.adnlibutils.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.alwaysallthetime.adnlib.GeneralParameter;
import com.alwaysallthetime.adnlib.QueryParameters;
import com.alwaysallthetime.adnlib.data.Annotation;
import com.alwaysallthetime.adnlib.data.Channel;
import com.alwaysallthetime.adnlib.data.Message;
import com.alwaysallthetime.adnlibutils.ADNApplication;
import com.alwaysallthetime.adnlibutils.AnnotationFactory;
import com.alwaysallthetime.adnlibutils.AnnotationUtility;
import com.alwaysallthetime.adnlibutils.PrivateChannelUtility;
import com.alwaysallthetime.adnlibutils.db.ADNDatabase;
import com.alwaysallthetime.adnlibutils.db.ActionMessageSpec;
import com.alwaysallthetime.adnlibutils.model.MessagePlus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * The ActionMessageManager is used to perform mutable actions on Messages.
 *
 * Since Annotations are not mutable, user-invoked actions on individual Messages (e.g.
 * marking a Message as a favorite, as read/unread, etc.) are not manageable with the App.net API.
 * This Manager class is used to hack around this limitation.
 *
 * We use Action Channels (Channels of type com.alwaysallthetime.action)
 * with machine-only Messages to perform Actions on another Channel's Messages. These Action Messages
 * have metadata annotations (type com.alwaysallthetime.action.metadata) that point to their associated
 * "target" message in another Channel. All Messages in an Action Channel correspond to the same
 * action (i.e. there is only one action per Action Channel). Since Messages can be deleted, deleting
 * an Action Message effectively undoes a performed Action on the target Message.
 *
 * The ActionMessageManager abstracts away this hack by providing simple applyChannelAction()
 * and removeChannelAction() methods. Before performing either of these actions, you must call
 * initActionChannel() to create or get an existing Channel to host the Action Messages.
 * To check if an action has been performed on a specific target message, use the isActioned() method.
 */
public class ActionMessageManager {
    private static final String TAG = "ADNLibUtils_ActionMessageManager";

    public static final QueryParameters ACTION_MESSAGE_QUERY_PARAMETERS = new QueryParameters(GeneralParameter.INCLUDE_MACHINE,
            GeneralParameter.INCLUDE_MESSAGE_ANNOTATIONS, GeneralParameter.EXCLUDE_DELETED);

    private static final int MAX_BATCH_LOAD_FROM_DISK = 40;

    private MessageManager mMessageManager;
    private ADNDatabase mDatabase;

    //action channel id : (target message id : target message plus)
    private HashMap<String, TreeMap<String, MessagePlus>> mActionedMessages;

    private HashMap<String, Channel> mActionChannels;

    public interface ActionChannelInitializedHandler {
        public void onInitialized(Channel channel);
        public void onException(Exception exception);
    }

    private static ActionMessageManager sActionMessageManager;
    public static ActionMessageManager getInstance(MessageManager messageManager) {
        if(sActionMessageManager == null) {
            sActionMessageManager = new ActionMessageManager(messageManager);
        }
        return sActionMessageManager;
    }

    private ActionMessageManager(MessageManager messageManager) {
        mMessageManager = messageManager;
        mActionChannels = new HashMap<String, Channel>(1);
        mActionedMessages = new HashMap<String, TreeMap<String, MessagePlus>>(1);

        Context context = ADNApplication.getContext();
        mDatabase = ADNDatabase.getInstance(context);
        context.registerReceiver(sentMessageReceiver, new IntentFilter(MessageManager.INTENT_ACTION_UNSENT_MESSAGES_SENT));
    }

    /**
     * Sync and persist all Action Messages in a Channel.
     *
     * Instead of using the similar MessageManager method, this method should be used
     * to sync messages for an Action Channel. As batches of Messages are obtained, the Target Message id
     * for each Action Message will be extracted from annotations and stored to the sqlite database
     * for lookup at a later time.
     *
     * @param actionChannelId The id of the Action Channel for which messages will be synced.
     * @param targetChannelId The id of the Target Channel
     * @param responseHandler MessageManagerSyncResponseHandler
     *
     * @see com.alwaysallthetime.adnlibutils.manager.MessageManager#retrieveAndPersistAllMessages(String, com.alwaysallthetime.adnlibutils.manager.MessageManager.MessageManagerSyncResponseHandler)  
     */
    public void retrieveAndPersistAllActionMessages(final String actionChannelId, final String targetChannelId, final MessageManager.MessageManagerSyncResponseHandler responseHandler) {
        mMessageManager.retrieveAndPersistAllMessages(actionChannelId, new MessageManager.MessageManagerSyncResponseHandler() {
            @Override
            public void onSuccess(List<MessagePlus> responseData, boolean appended) {
                responseHandler.onSuccess(responseData, appended);
                Log.d(TAG, "Synced " + getNumMessagesSynced() + " messages for action channel " + actionChannelId);
            }

            @Override
            public void onBatchSynced(List<MessagePlus> messages) {
                super.onBatchSynced(messages);

                for(MessagePlus actionMessage : messages) {
                    String targetMessageId = AnnotationUtility.getTargetMessageId(actionMessage.getMessage());
                    mDatabase.insertOrReplaceActionMessageSpec(actionMessage, targetMessageId, targetChannelId);
                }
            }

            @Override
            public void onError(Exception exception) {
                Log.e(TAG, exception.getMessage(), exception);
                responseHandler.onError(exception);
            }
        });
    }

    /**
     * Return true if the specified target Message has had an action performed on it (the action
     * whose id is actionChannelId).
     *
     * @param actionChannelId the id of the Action Channel
     * @param targetMessageId The id of the target Message
     *
     * @return true if the specified target Message has had an action performed on it.
     */
    public boolean isActioned(String actionChannelId, String targetMessageId) {
        return getOrCreateActionedMessagesMap(actionChannelId).get(targetMessageId) != null ||
                mDatabase.hasActionMessageSpec(actionChannelId, targetMessageId);
    }

    /**
     * Initialize an Action Channel. This is typically done at app startup and must be done before
     * any other ActionMessageManager methods are used on the channel.
     *
     * @param actionType The identifier for the Action Channel (e.g. com.alwaysallthetime.pizzaparty)
     * @param targetChannel The Channel whose messages will have actions performed.
     * @param handler ActionChannelInitializedHandler
     */
    public synchronized void initActionChannel(final String actionType, final Channel targetChannel, final ActionChannelInitializedHandler handler) {
        PrivateChannelUtility.getOrCreateActionChannel(mMessageManager.getClient(), actionType, targetChannel, new PrivateChannelUtility.PrivateChannelGetOrCreateHandler() {
            @Override
            public void onResponse(Channel channel, boolean createdNewChannel) {
                mActionChannels.put(channel.getId(), channel);
                mMessageManager.setParameters(channel.getId(), ACTION_MESSAGE_QUERY_PARAMETERS);
                handler.onInitialized(channel);
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, error.getMessage(), error);
                handler.onException(error);
            }
        });
    }

    private TreeMap<String, MessagePlus> getOrCreateActionedMessagesMap(String channelId) {
        TreeMap<String, MessagePlus> channelMap = mActionedMessages.get(channelId);
        if(channelMap == null) {
            channelMap = new TreeMap<String, MessagePlus>(sIdComparator);
            mActionedMessages.put(channelId, channelMap);
        }
        return channelMap;
    }

    private Set<String> getTargetMessageIds(Collection<MessagePlus> messagePlusses) {
        HashSet<String> newTargetMessageIds = new HashSet<String>(messagePlusses.size());
        for(MessagePlus mp : messagePlusses) {
            newTargetMessageIds.add(AnnotationUtility.getTargetMessageId(mp.getMessage()));
        }
        return newTargetMessageIds;
    }

    public synchronized List<MessagePlus> getActionedMessages(String actionChannelId, String targetChannelId) {
        TreeMap<String, MessagePlus> channelActionedMessages = mActionedMessages.get(actionChannelId);
        if(channelActionedMessages == null || channelActionedMessages.size() == 0) {
            LinkedHashMap<String, MessagePlus> loadedMessagesFromActionChannel = mMessageManager.getMessageMap(actionChannelId);
            if(loadedMessagesFromActionChannel == null || loadedMessagesFromActionChannel.size() == 0) {
                loadedMessagesFromActionChannel = mMessageManager.loadPersistedMessages(actionChannelId, MAX_BATCH_LOAD_FROM_DISK);
            }
            return getTargetMessages(loadedMessagesFromActionChannel.values(), actionChannelId, targetChannelId);
        } else {
            return new ArrayList<MessagePlus>(channelActionedMessages.values());
        }
    }

    private synchronized List<MessagePlus> getTargetMessages(Collection<MessagePlus> actionMessages, String actionChannelId, String targetChannelId) {
        Set<String> newTargetMessageIds = getTargetMessageIds(actionMessages);
        LinkedHashMap<String, MessagePlus> targetMessages = mMessageManager.loadAndConfigureTemporaryMessages(targetChannelId, newTargetMessageIds);
        return new ArrayList<MessagePlus>(targetMessages.values());
    }

    public synchronized void getMoreActionedMessages(final String actionChannelId, final String targetChannelId, final MessageManager.MessageManagerResponseHandler responseHandler) {
        LinkedHashMap<String, MessagePlus> more = mMessageManager.loadPersistedMessages(actionChannelId, MAX_BATCH_LOAD_FROM_DISK);
        if(more.size() > 0) {
            Set<String> newTargetMessageIds = getTargetMessageIds(more.values());
            LinkedHashMap<String, MessagePlus> moreTargetMessages = mMessageManager.loadAndConfigureTemporaryMessages(targetChannelId, newTargetMessageIds);

            //save them to the in-memory map
            TreeMap<String, MessagePlus> channelActionMessages = getOrCreateActionedMessagesMap(actionChannelId);
            channelActionMessages.putAll(moreTargetMessages);

            responseHandler.setIsMore(more.size() == MAX_BATCH_LOAD_FROM_DISK);
            responseHandler.onSuccess(new ArrayList(moreTargetMessages.values()), true);
        } else {
            //
            //load more action messages, then get the target messages
            //
            mMessageManager.retrieveMoreMessages(actionChannelId, new MessageManager.MessageManagerResponseHandler() {
                @Override
                public void onSuccess(List<MessagePlus> responseData, boolean appended) {
                    Set<String> newTargetMessageIds = getTargetMessageIds(responseData);
                    LinkedHashMap<String, MessagePlus> moreTargetMessages = mMessageManager.loadAndConfigureTemporaryMessages(targetChannelId, newTargetMessageIds);

                    //save them to the in-memory map
                    TreeMap<String, MessagePlus> channelActionMessages = getOrCreateActionedMessagesMap(actionChannelId);
                    channelActionMessages.putAll(moreTargetMessages);

                    responseHandler.setIsMore(isMore());
                    responseHandler.onSuccess(new ArrayList(moreTargetMessages.values()), true);
                }

                @Override
                public void onError(Exception exception) {
                    Log.d(TAG, exception.getMessage(), exception);
                    responseHandler.onError(exception);
                }
            });
        }
    }

    public synchronized void applyChannelAction(String actionChannelId, MessagePlus targetMessagePlus) {
        if(!isActioned(actionChannelId, targetMessagePlus.getMessage().getId())) {
            TreeMap<String, MessagePlus> actionedMessages = getOrCreateActionedMessagesMap(actionChannelId);
            Message message = targetMessagePlus.getMessage();
            String targetMessageId = message.getId();
            if(actionedMessages.get(targetMessageId) == null) {
                //create machine only message in action channel that points to the target message id.
                Message m = new Message(true);
                Annotation a = AnnotationFactory.getSingleValueAnnotation(PrivateChannelUtility.MESSAGE_ANNOTATION_TARGET_MESSAGE, PrivateChannelUtility.TARGET_MESSAGE_KEY_ID, targetMessageId);
                m.addAnnotation(a);

                MessagePlus unsentActionMessage = mMessageManager.createUnsentMessageAndAttemptSend(actionChannelId, m);
                actionedMessages.put(targetMessageId, targetMessagePlus);
                mDatabase.insertOrReplaceActionMessageSpec(unsentActionMessage, targetMessageId, message.getChannelId());
            }
        }
    }

    public synchronized void removeChannelAction(String actionChannelId, final String targetMessageId) {
        ArrayList<String> targetMessageIds = new ArrayList<String>(1);
        targetMessageIds.add(targetMessageId);
        List<ActionMessageSpec> actionMessageSpecs = mDatabase.getActionMessageSpecsForTargetMessages(actionChannelId, targetMessageIds);

        if(actionMessageSpecs.size() == 1) {
            mDatabase.deleteActionMessageSpec(actionChannelId, targetMessageId);
            TreeMap<String, MessagePlus> actionedMessages = getOrCreateActionedMessagesMap(actionChannelId);
            actionedMessages.remove(targetMessageId);

            final ActionMessageSpec actionMessageSpec = actionMessageSpecs.get(0);
            MessagePlus actionMessagePlus = mDatabase.getMessage(actionChannelId, actionMessageSpec.getActionMessageId());

            //the success/failure of this should not matter - on failure, it will be a pending deletion
            mMessageManager.deleteMessage(actionMessagePlus, new MessageManager.MessageDeletionResponseHandler() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Successfully deleted action message " + actionMessageSpec.getActionMessageId() + " for target message " + targetMessageId);
                }

                @Override
                public void onError(Exception exception) {
                    Log.d(TAG, "Failed to delete action message " + actionMessageSpec.getActionMessageId() + " for target message " + targetMessageId);
                }
            });
        } else {
            Log.e(TAG, "Calling removeChannelAction, but actionChannelId " + actionChannelId + " and targetMessageId " + targetMessageId + " yielded 0 db results. wtf.");
        }
    }

    private final BroadcastReceiver sentMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(MessageManager.INTENT_ACTION_UNSENT_MESSAGES_SENT.equals(intent.getAction())) {
                final String channelId = intent.getStringExtra(MessageManager.EXTRA_CHANNEL_ID);
                final ArrayList<String> sentMessageIds = intent.getStringArrayListExtra(MessageManager.EXTRA_SENT_MESSAGE_IDS);

                //this is not an action channel.
                //it might be a target channel of one of our action channels though.
                if(mActionChannels.get(channelId) == null) {
                    //some messages were sent, instead of just removing the faked messages,
                    //just remove the whole channel's map. we will have to reload them later, but
                    //this way we can assure that they'll be all in the right order, etc.
                    mActionedMessages.remove(channelId);

                    //remove all action messages that point to this now nonexistent target message id
                    List<ActionMessageSpec> sentTargetMessages = mDatabase.getActionMessageSpecsForTargetMessages(sentMessageIds);
                    for(ActionMessageSpec actionMessageSpec : sentTargetMessages) {
                        String actionChannelId = actionMessageSpec.getActionChannelId();
                        mDatabase.deleteActionMessageSpec(actionChannelId, actionMessageSpec.getTargetMessageId());
                    }
                } else {
                    //it's an action channel
                    //delete the action messages in the database with the sent message ids,
                    //retrieve the new ones

                    Channel actionChannel = mActionChannels.get(channelId);
                    final String targetChannelId = AnnotationUtility.getTargetChannelId(actionChannel);
                    mMessageManager.retrieveNewestMessages(channelId, new MessageManager.MessageManagerResponseHandler() {
                        @Override
                        public void onSuccess(List<MessagePlus> responseData, boolean appended) {
                            for(String sentMessageId : sentMessageIds) {
                                mDatabase.deleteActionMessageSpec(sentMessageId);
                            }
                            for(MessagePlus mp : responseData) {
                                String targetMessageId = AnnotationUtility.getTargetMessageId(mp.getMessage());
                                mDatabase.insertOrReplaceActionMessageSpec(mp, targetMessageId, targetChannelId);
                            }
                        }

                        @Override
                        public void onError(Exception exception) {
                            Log.e(TAG, exception.getMessage(), exception);
                        }
                    });
                }
            }
        }
    };

    private static Comparator<String> sIdComparator = new Comparator<String>() {
        @Override
        public int compare(String lhs, String rhs) {
            Integer lhsInteger = new Integer(Integer.parseInt(lhs));
            Integer rhsInteger = new Integer(Integer.parseInt(rhs));
            //we want desc order (reverse chronological order)
            return rhsInteger.compareTo(lhsInteger);
        }
    };
}
