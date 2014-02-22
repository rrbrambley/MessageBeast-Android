package com.alwaysallthetime.messagebeast.manager;

import android.util.Log;

import com.alwaysallthetime.adnlib.GeneralParameter;
import com.alwaysallthetime.adnlib.QueryParameters;
import com.alwaysallthetime.adnlib.data.Annotation;
import com.alwaysallthetime.adnlib.data.Channel;
import com.alwaysallthetime.adnlib.data.Message;
import com.alwaysallthetime.messagebeast.ADNApplication;
import com.alwaysallthetime.messagebeast.AnnotationFactory;
import com.alwaysallthetime.messagebeast.AnnotationUtility;
import com.alwaysallthetime.messagebeast.PrivateChannelUtility;
import com.alwaysallthetime.messagebeast.db.ADNDatabase;
import com.alwaysallthetime.messagebeast.db.ActionMessageSpec;
import com.alwaysallthetime.messagebeast.model.MessagePlus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * The ActionMessageManager is used to perform mutable actions on Messages.<br><br>
 *
 * Since Annotations are not mutable, user-invoked actions on individual Messages (e.g.
 * marking a Message as a favorite, as read/unread, etc.) are not manageable with the App.net API.
 * This Manager class is used to hack around this limitation.<br><br>
 *
 * We use Action Channels (Channels of type com.alwaysallthetime.action)
 * with machine-only Messages to perform Actions on another Channel's Messages. These Action Messages
 * have metadata annotations (type com.alwaysallthetime.action.metadata) that point to their associated
 * "target" message in another Channel. All Messages in an Action Channel correspond to the same
 * action (i.e. there is only one action per Action Channel). Since Messages can be deleted, deleting
 * an Action Message effectively undoes a performed Action on the target Message.<br><br>
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

    private MessageManager mMessageManager;
    private ADNDatabase mDatabase;

    private HashMap<String, Channel> mActionChannels;

    public interface ActionChannelInitializedHandler {
        public void onInitialized(Channel channel);
        public void onException(Exception exception);
    }

    public ActionMessageManager(MessageManager messageManager) {
        mMessageManager = messageManager;
        mMessageManager.attachActionMessageManager(this);
        mActionChannels = new HashMap<String, Channel>(1);
        mDatabase = ADNDatabase.getInstance(ADNApplication.getContext());
    }

    /**
     * Get the MessageManager used by this ActionMessageManager
     *
     * @return MessageManager
     */
    public MessageManager getMessageManager() {
        return mMessageManager;
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
     * @param responseHandler MessageManagerSyncResponseHandler
     *
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#retrieveAndPersistAllMessages(String, com.alwaysallthetime.messagebeast.manager.MessageManager.MessageManagerSyncResponseHandler)
     */
    public void retrieveAndPersistAllActionMessages(final String actionChannelId, final MessageManager.MessageManagerSyncResponseHandler responseHandler) {
        mMessageManager.retrieveAndPersistAllMessages(actionChannelId, new MessageManager.MessageManagerSyncResponseHandler() {
            @Override
            public void onSuccess(List<MessagePlus> responseData) {
                responseHandler.onSuccess(responseData);
                Log.d(TAG, "Synced " + getNumMessagesSynced() + " messages for action channel " + actionChannelId);
            }

            @Override
            public void onBatchSynced(List<MessagePlus> messages) {
                super.onBatchSynced(messages);
                processNewActionMessages(messages);
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
        return mDatabase.hasActionMessageSpec(actionChannelId, targetMessageId);
    }

    /**
     * Given a Collection of message Ids, return those which are associated with Messages that
     * have had an action performed on them (the action associated with the provided Action Channel id).
     *
     * This is more efficient than looping through message ids and calling isActioned() on them one
     * at a time because a single database query is used in this case.
     *
     * @param actionChannelId the id of the Action Channel
     * @param messageIds the ids of the target messages
     * @return a subset of the messageIds Collection, containing the ids associated with messages to
     * which the action has been applied.
     */
    public Set<String> getActionedMessageIds(String actionChannelId, Collection<String> messageIds) {
        return mDatabase.getTargetMessageIdsWithSpecs(actionChannelId, messageIds);
    }

    /**
     * Return true if there are any ActionMessageSpecs persisted for the provided Action Channel id.
     *
     * @param actionChannelId the id of the Action Channel
     * @return true if there are any ActionMessageSpecs persisted for the provided Action Channel id,
     * false otherwise.
     */
    public boolean hasActionedMessages(String actionChannelId) {
        return mDatabase.getActionMessageSpecCount(actionChannelId) > 0;
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

    /**
     * Get persisted Messages that have an action applied.
     *
     * @param actionChannelId the the id of the Action Channel associated with the action of interest
     * @param limit the maximum number of Messages to get
     * @return a List consisting of MessagePlus Objects corresponding to Messages in the target Channel
     * that have had the action applied.
     */
    public synchronized List<MessagePlus> getActionedMessages(String actionChannelId, int limit) {
        return getActionedMessages(actionChannelId, null, limit);
    }

    /**
     * Get persisted Messages that have an action applied.
     *
     * @param actionChannelId the the id of the Action Channel associated with the action of interest
     * @param beforeDate a date before the display date of all returned messages. Can be null.
     * @param limit the maximum number of Messages to get
     * @return a List consisting of MessagePlus Objects corresponding to Messages in the target Channel
     * that have had the action applied.
     */
    public synchronized List<MessagePlus> getActionedMessages(String actionChannelId, Date beforeDate, int limit) {
        List<ActionMessageSpec> actionMessageSpecs = mDatabase.getActionMessageSpecsOrderedByTargetMessageDisplayDate(actionChannelId, beforeDate, limit);
        HashSet<String> targetMessageIds = new HashSet<String>(actionMessageSpecs.size());
        for(ActionMessageSpec spec : actionMessageSpecs) {
            targetMessageIds.add(spec.getTargetMessageId());
        }
        TreeMap<Long, MessagePlus> targetMessages = mMessageManager.getMessages(targetMessageIds);
        return new ArrayList<MessagePlus>(targetMessages.values());
    }

    /**
     * Retrieve the newest Messages in an Action Channel.
     *
     * @param actionChannelId the id of the Action Channel
     * @param responseHandler MessageManagerResponseHandler
     * @return false if unsent Messages are preventing more Messages from being retrieved, true otherwise.
     */
    public synchronized boolean retrieveNewestMessages(final String actionChannelId, final MessageManager.MessageManagerResponseHandler responseHandler) {
        TreeMap<Long, MessagePlus> channelMessages = mMessageManager.getMessageMap(actionChannelId);
        if(channelMessages == null || channelMessages.size() == 0) {
            //we do this so that the max id is known.
            mMessageManager.loadPersistedMessages(actionChannelId, 1);
        }
        boolean canRetrieve = mMessageManager.retrieveNewestMessages(actionChannelId, new MessageManager.MessageManagerResponseHandler() {
            @Override
            public void onSuccess(List<MessagePlus> responseData) {
                processNewActionMessages(responseData);
                responseHandler.onSuccess(responseData);
            }

            @Override
            public void onError(Exception exception) {
                Log.e(TAG, exception.getMessage(), exception);
                responseHandler.onError(exception);
            }
        });
        return canRetrieve;
    }

    /**
     * Apply an Action Channel action to a target Message.
     *
     * Nothing happens if the provided target Message already has the action applied.
     *
     * @param actionChannelId the id of the Action Channel
     * @param targetMessagePlus the Message to have the action applied.
     */
    public synchronized void applyChannelAction(String actionChannelId, MessagePlus targetMessagePlus) {
        if(!isActioned(actionChannelId, targetMessagePlus.getMessage().getId())) {
            Message message = targetMessagePlus.getMessage();
            String targetMessageId = message.getId();
            //create machine only message in action channel that points to the target message id.
            Message m = new Message(true);
            Annotation a = AnnotationFactory.getSingleValueAnnotation(PrivateChannelUtility.MESSAGE_ANNOTATION_TARGET_MESSAGE, PrivateChannelUtility.TARGET_MESSAGE_KEY_ID, targetMessageId);
            m.addAnnotation(a);

            MessagePlus unsentActionMessage = mMessageManager.createUnsentMessage(actionChannelId, m, !targetMessagePlus.isUnsent());
            mDatabase.insertOrReplaceActionMessageSpec(unsentActionMessage, targetMessageId, message.getChannelId(), targetMessagePlus.getDisplayDate());
        }
    }

    /**
     * Remove an Action Channel action from a target Message.
     *
     * Nothing happens if the provided target Message does not already have the action applied.
     *
     * @param actionChannelId the id of the Action Channel
     * @param targetMessageId the id of the target Message
     */
    public synchronized void removeChannelAction(final String actionChannelId, final String targetMessageId) {
        ArrayList<String> targetMessageIds = new ArrayList<String>(1);
        targetMessageIds.add(targetMessageId);
        final List<ActionMessageSpec> actionMessageSpecs = mDatabase.getActionMessageSpecsForTargetMessages(actionChannelId, targetMessageIds);

        if(actionMessageSpecs.size() > 0) {
            mDatabase.deleteActionMessageSpec(actionChannelId, targetMessageId);

            deleteActionMessages(actionMessageSpecs, 0, new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Finished deleting " + actionMessageSpecs.size() + " action messages in channel " + actionChannelId);
                }
            });
        } else {
            Log.e(TAG, "Calling removeChannelAction, but actionChannelId " + actionChannelId + " and targetMessageId " + targetMessageId + " yielded 0 db results. wtf.");
        }
    }

    private synchronized void deleteActionMessages(final List<ActionMessageSpec> actionMessageSpecs, final int currentIndex, final Runnable completionRunnable) {
        final ActionMessageSpec actionMessageSpec = actionMessageSpecs.get(0);
        MessagePlus actionMessagePlus = mDatabase.getMessage(actionMessageSpec.getActionMessageId());

        if(actionMessagePlus != null) {
            //the success/failure of this should not matter - on failure, it will be a pending deletion
            mMessageManager.deleteMessage(actionMessagePlus, new MessageManager.MessageDeletionResponseHandler() {
                @Override
                public void onSuccess() {
                    deleteNext();
                }

                @Override
                public void onError(Exception exception) {
                    Log.e(TAG, "Failed to delete action message " + actionMessageSpec.getActionMessageId() + " for target message " + actionMessageSpec.getTargetMessageId());
                    deleteNext();
                }

                private void deleteNext() {
                    int nextIndex = currentIndex + 1;
                    if(nextIndex < actionMessageSpecs.size()) {
                        deleteActionMessages(actionMessageSpecs, nextIndex, completionRunnable);
                    } else {
                        completionRunnable.run();
                    }
                }
            });
        } else {
            Log.e(TAG, "could not delete action message " + actionMessageSpec.getActionMessageId() + "; no persisted MessagePlus exists");
        }
    }

    private synchronized void processNewActionMessages(List<MessagePlus> actionMessages) {
        HashMap<String, MessagePlus> targetMessageIdToActionMessage = new HashMap<String, MessagePlus>(actionMessages.size());

        for(MessagePlus actionMessage : actionMessages) {
            String targetMessageId = AnnotationUtility.getTargetMessageId(actionMessage.getMessage());
            if(targetMessageId != null) {
                targetMessageIdToActionMessage.put(targetMessageId, actionMessage);
            } else {
                Log.e(TAG, "action message " + actionMessage.getMessage().getId() + " is missing target message metadata!");
            }
        }

        TreeMap<Long, MessagePlus> targetMessages = mMessageManager.getMessages(targetMessageIdToActionMessage.keySet());
        for(MessagePlus targetMessage : targetMessages.values()) {
            String targetMessageId = targetMessage.getMessage().getId();
            String targetChannelId = targetMessage.getMessage().getChannelId();
            MessagePlus actionMessage = targetMessageIdToActionMessage.get(targetMessageId);
            Date targetMessageDisplayDate = targetMessage.getDisplayDate();
            mDatabase.insertOrReplaceActionMessageSpec(actionMessage, targetMessageId, targetChannelId, targetMessageDisplayDate);
        }
    }

    /**
     * This is intended to be used by MessageManager only.
     *
     * ActionMessageManager needs to do processing when unsent messages are sent BEFORE the client
     * application has a chance to do stuff (to avoid race conditions).
     *
     * @param channelId
     * @param sentMessageIds
     * @param replacementMessageIds
     */
    synchronized void onUnsentMessagesSentPrivate(final String channelId, final ArrayList<String> sentMessageIds, final ArrayList<String> replacementMessageIds) {
        //this is not an action channel.
        //it might be a target channel of one of our action channels though.
        if(!mActionChannels.containsKey(channelId)) {

            //for any action messages that targeted the unsent message,
            //we now need to create a new action message spec that points to the NEW message id
            //to replace the former one.
            //
            //additionally, we need to make sure to send unsent action messages now that their
            //associated target messages have been sent.
            //
            HashSet<String> actionChannelIds = new HashSet<String>();
            List<ActionMessageSpec> actionMessageSpecs = mDatabase.getActionMessageSpecsForTargetMessages(sentMessageIds);
            for(ActionMessageSpec actionMessageSpec : actionMessageSpecs) {
                String actionMessageId = actionMessageSpec.getActionMessageId();
                String actionChannelId = actionMessageSpec.getActionChannelId();
                String oldTargetMessageId = actionMessageSpec.getTargetMessageId();
                String newTargetMessageId = replacementMessageIds.get(sentMessageIds.indexOf(oldTargetMessageId));
                String targetMessageChannelId = channelId;
                Date targetMessageDisplayDate = actionMessageSpec.getTargetMessageDisplayDate();
                //TODO: this target message display date should be updated here (?)
                mDatabase.insertOrReplaceActionMessageSpec(actionMessageId, actionChannelId, newTargetMessageId, targetMessageChannelId, targetMessageDisplayDate);
                Log.d(TAG, "Updating action message spec; target id change: " + oldTargetMessageId + " --> " + newTargetMessageId);

                MessagePlus actionMessage = mDatabase.getMessage(actionMessageId);
                if(actionMessage != null) {
                    String formerTargetMessageId = AnnotationUtility.getTargetMessageId(actionMessage.getMessage());
                    actionMessage.replaceTargetMessageAnnotationMessageId(newTargetMessageId);
                    Log.d(TAG, "replaced message's target message id annotation: " + formerTargetMessageId + " --> " + AnnotationUtility.getTargetMessageId(actionMessage.getMessage()));
                    mDatabase.insertOrReplaceMessage(actionMessage);
                    mMessageManager.replaceInMemoryMessage(actionMessage);
                }

                actionChannelIds.add(actionChannelId);
            }

            for(String actionChannelId : actionChannelIds) {
                mMessageManager.sendAllUnsent(actionChannelId);
                Log.d(TAG, "sending all unsent messages for action channel " + actionChannelId);
            }
            mMessageManager.sendUnsentMessagesSentBroadcast(channelId, sentMessageIds, replacementMessageIds);
        } else {
            //it's an action channel
            //replace the old specs' action message ids with the replacement ones

            for(int i = 0; i < sentMessageIds.size(); i++) {
                String actionMessageId = sentMessageIds.get(i);
                ActionMessageSpec oldSpec = mDatabase.getActionMessageSpec(actionMessageId);
                if(oldSpec != null) {
                    String newActionMessageId = replacementMessageIds.get(i);
                    mDatabase.insertOrReplaceActionMessageSpec(newActionMessageId, oldSpec.getActionChannelId(),
                            oldSpec.getTargetMessageId(), oldSpec.getTargetChannelId(), oldSpec.getTargetMessageDisplayDate());
                    mDatabase.deleteActionMessageSpec(oldSpec.getActionMessageId());
                    Log.d(TAG, "replaced action message spec; action message id " + oldSpec.getActionMessageId() + " ---> " + newActionMessageId);
                } else {
                    Log.d(TAG, "no action message spec to update for action message with id " + actionMessageId);
                }
            }

            mMessageManager.sendUnsentMessagesSentBroadcast(channelId, sentMessageIds, replacementMessageIds);
        }
    }
}
