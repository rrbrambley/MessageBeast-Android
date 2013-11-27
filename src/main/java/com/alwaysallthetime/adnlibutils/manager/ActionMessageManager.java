package com.alwaysallthetime.adnlibutils.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.alwaysallthetime.adnlib.AppDotNetClient;
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
import com.alwaysallthetime.adnlibutils.db.ActionMessage;
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

    public void retrieveAndPersistAllActionMessages(final String actionChannelId, final String targetChannelId, final MessageManager.MessageManagerSyncResponseHandler responseHandler) {
        mMessageManager.retrieveAndPersistAllMessages(actionChannelId, new MessageManager.MessageManagerSyncResponseHandler() {
            @Override
            public void onSuccess(List<MessagePlus> responseData, boolean appended) {
                extractAndStoreTargetMessagesInMemory(responseData, actionChannelId, targetChannelId);
                responseHandler.onSuccess(responseData, appended);
                Log.d(TAG, "Synced " + getNumMessagesSynced() + " messages for action channel " + actionChannelId);
            }

            @Override
            public void onBatchSynced(List<MessagePlus> messages) {
                super.onBatchSynced(messages);

                for(MessagePlus actionMessage : messages) {
                    String targetMessageId = AnnotationUtility.getTargetMessageId(actionMessage.getMessage());
                    mDatabase.insertOrReplaceActionMessage(actionMessage, targetMessageId, targetChannelId);
                }
            }

            @Override
            public void onError(Exception exception) {
                Log.e(TAG, exception.getMessage(), exception);
                //TODO
            }
        });
    }

    public boolean isActioned(String actionChannelId, String targetMessageId) {
        return getOrCreateActionedMessagesMap(actionChannelId).get(targetMessageId) != null ||
                mDatabase.hasActionMessage(actionChannelId, targetMessageId);
    }

    public synchronized void initActionChannel(final String actionType, final Channel targetChannel, final ActionChannelInitializedHandler handler) {
        Channel actionChannel = PrivateChannelUtility.getActionChannel(actionType, targetChannel.getId());
        if(actionChannel == null) {
            final AppDotNetClient client = mMessageManager.getClient();
            PrivateChannelUtility.retrieveActionChannel(client, actionType, targetChannel.getId(), new PrivateChannelUtility.PrivateChannelHandler() {
                @Override
                public void onResponse(Channel channel) {
                    if(channel == null) {
                        PrivateChannelUtility.createActionChannel(client, actionType, targetChannel.getId(), new PrivateChannelUtility.PrivateChannelHandler() {
                            @Override
                            public void onResponse(Channel channel) {
                                mActionChannels.put(channel.getId(), channel);
                                mMessageManager.setParameters(channel.getId(), ACTION_MESSAGE_QUERY_PARAMETERS);
                                handler.onInitialized(channel);
                            }

                            @Override
                            public void onError(Exception error) {
                                Log.d(TAG, error.getMessage(), error);
                                handler.onException(error);
                            }
                        });
                    } else {
                        mActionChannels.put(channel.getId(), channel);
                        mMessageManager.setParameters(channel.getId(), ACTION_MESSAGE_QUERY_PARAMETERS);
                        handler.onInitialized(channel);
                    }
                }

                @Override
                public void onError(Exception error) {
                    Log.d(TAG, error.getMessage(), error);
                    handler.onException(error);
                }
            });
        } else {
            mActionChannels.put(actionChannel.getId(), actionChannel);
            mMessageManager.setParameters(actionChannel.getId(), ACTION_MESSAGE_QUERY_PARAMETERS);
            handler.onInitialized(actionChannel);
        }
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
            return extractAndStoreTargetMessagesInMemory(loadedMessagesFromActionChannel.values(), actionChannelId, targetChannelId);
        } else {
            return new ArrayList<MessagePlus>(channelActionedMessages.values());
        }
    }

    private synchronized List<MessagePlus> extractAndStoreTargetMessagesInMemory(Collection<MessagePlus> actionMessages, String actionChannelId, String targetChannelId) {
        Set<String> newTargetMessageIds = getTargetMessageIds(actionMessages);
        LinkedHashMap<String, MessagePlus> newTargetMessages = mMessageManager.loadAndConfigureTemporaryMessages(targetChannelId, newTargetMessageIds);
        TreeMap<String, MessagePlus> newSortedTargetMessages = new TreeMap<String, MessagePlus>(sIdComparator);
        newSortedTargetMessages.putAll(newTargetMessages);
        mActionedMessages.put(actionChannelId, newSortedTargetMessages);
        return new ArrayList<MessagePlus>(newSortedTargetMessages.values());
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
                mDatabase.insertOrReplaceActionMessage(unsentActionMessage, targetMessageId, message.getChannelId());
            }
        }
    }

    public synchronized void removeChannelAction(String actionChannelId, final String targetMessageId) {
        ArrayList<String> targetMessageIds = new ArrayList<String>(1);
        targetMessageIds.add(targetMessageId);
        List<ActionMessage> actionMessages = mDatabase.getActionMessagesForTargetMessages(targetMessageIds);

        if(actionMessages.size() == 1) {
            mDatabase.deleteActionMessage(actionChannelId, targetMessageId);
            TreeMap<String, MessagePlus> actionedMessages = getOrCreateActionedMessagesMap(actionChannelId);
            actionedMessages.remove(targetMessageId);

            final ActionMessage actionMessage = actionMessages.get(0);
            MessagePlus actionMessagePlus = mDatabase.getMessage(actionChannelId, actionMessage.getActionMessageId());

            //the success/failure of this should not matter - on failure, it will be a pending deletion
            mMessageManager.deleteMessage(actionMessagePlus, new MessageManager.MessageDeletionResponseHandler() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Successfully deleted action message " + actionMessage.getActionMessageId() + " for target message " + targetMessageId);
                }

                @Override
                public void onError(Exception exception) {
                    Log.d(TAG, "Failed to delete action message " + actionMessage.getActionMessageId() + " for target message " + targetMessageId);
                }
            });
        }
    }

    private final BroadcastReceiver sentMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(MessageManager.INTENT_ACTION_UNSENT_MESSAGES_SENT.equals(intent.getAction())) {
                String channelId = intent.getStringExtra(MessageManager.EXTRA_CHANNEL_ID);
                ArrayList<String> sentMessageIds = intent.getStringArrayListExtra(MessageManager.EXTRA_SENT_MESSAGE_IDS);

                //this is not an action channel.
                //it might be a target channel of one of our action channels though.
                if(mActionChannels.get(channelId) == null) {
                    //some messages were sent, instead of just removing the faked messages,
                    //just remove the whole channel's map. we will have to reload them later, but
                    //this way we can assure that they'll be all in the right order, etc.
                    mActionedMessages.remove(channelId);

                    //remove all action messages that point to this now nonexistent target message id
                    List<ActionMessage> sentTargetMessages = mDatabase.getActionMessagesForTargetMessages(sentMessageIds);
                    for(ActionMessage actionMessage : sentTargetMessages) {
                        String actionChannelId = actionMessage.getActionChannelId();
                        mDatabase.deleteActionMessage(actionChannelId, actionMessage.getTargetMessageId());
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
                            for(MessagePlus mp : responseData) {
                                String targetMessageId = AnnotationUtility.getTargetMessageId(mp.getMessage());
                                mDatabase.insertOrReplaceActionMessage(mp, targetMessageId, targetChannelId);
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
