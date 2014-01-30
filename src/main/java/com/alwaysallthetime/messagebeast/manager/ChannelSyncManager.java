package com.alwaysallthetime.messagebeast.manager;

import android.content.Intent;
import android.util.Log;

import com.alwaysallthetime.adnlib.AppDotNetClient;
import com.alwaysallthetime.adnlib.data.Channel;
import com.alwaysallthetime.messagebeast.ADNApplication;
import com.alwaysallthetime.messagebeast.PrivateChannelUtility;
import com.alwaysallthetime.messagebeast.filter.MessageFilter;
import com.alwaysallthetime.messagebeast.model.ChannelRefreshResult;
import com.alwaysallthetime.messagebeast.model.ChannelRefreshResultSet;
import com.alwaysallthetime.messagebeast.model.ChannelSpec;
import com.alwaysallthetime.messagebeast.model.ChannelSpecSet;
import com.alwaysallthetime.messagebeast.model.FullSyncState;
import com.alwaysallthetime.messagebeast.model.MessagePlus;
import com.alwaysallthetime.messagebeast.model.TargetWithActionChannelsSpecSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ChannelSyncManager simplifies the syncing of several channels simultaneously.<br><br>
 *
 * This manager is especially useful when one or more Action Channels are being synced
 * for a "target" Channel. For example, a journaling app may choose to have a target Channel
 * for journal entries that are accompanied by "favorite entries" and "locked entries" Action Channels –
 * enabling users to mark entries as favorite entries, and locked entries, respectively. In this scenario,
 * pulling the newest Messages from the server requires performing three requests; this class
 * can be used to perform the three requests in one method call, with one callback.<br><br>
 *
 * ChannelSyncManager can also perform full syncs on multiple channels with one method call..<br><br>
 *
 * To use the functionality in ChannelSyncManager, it is important to first call initChannels() after
 * instantiating it with your channel specs.<br><br>
 *
 * @see com.alwaysallthetime.messagebeast.manager.ActionMessageManager
 * @see com.alwaysallthetime.messagebeast.manager.MessageManager
 */
public class ChannelSyncManager {

    public static final String INTENT_ACTION_CHANNELS_INITIALIZED = "com.alwaysallthetime.messagebeast.manager.ChannelSyncManager.intent.channelsInitialized";

    private static final String TAG = "ADNLibUtils_ChannelSyncManager";

    private MessageManager mMessageManager;
    private ActionMessageManager mActionMessageManager;

    //the constructor you use will determine which of the following fields are used:

    //
    //either these
    //
    private TargetWithActionChannelsSpecSet mTargetWithActionChannelsSpecSet;
    private Channel mTargetChannel;
    private Map<String, Channel> mActionChannels; //indexed by action type

    //
    //OR
    //

    //
    //these
    //
    private ChannelSpecSet mChannelSpecSet;
    private Map<String, Channel> mChannels; //indexed by channel id

    private interface ChannelInitializedHandler {
        public void onChannelInitialized(Channel channel);
        public void onException();
    }

    public interface ChannelsInitializedHandler {
        public void onChannelsInitialized();
        public void onException();
    }

    public static abstract class ChannelSyncStatusHandler {
        public abstract void onSyncComplete();
        public abstract void onSyncStarted();
        public abstract void onSyncException(Exception exception);

        //override this in the case where you pass false value for
        //automaticallyResumeSyncIfPreviouslyStarted to checkFullSyncStatus()
        public void onSyncIncomplete() {

        }
    }

    public static interface ChannelRefreshHandler {
        public void onComplete(ChannelRefreshResultSet resultSet);
    }

    /**
     * Create a ChannelSyncManager for a set of Channels, an AppDotNetClient
     * and a MessageManagerConfiguration. This constructor is convenient for use cases where you don't
     * plan on creating a MessageManager for use outside of the ChannelSyncManager.
     *
     * Note that this constructor is not for use with Action Channels.
     *
     * @param appDotNetClient The AppDotNetClient used to make requests. This will be used to construct
     *                        the MessageManager.
     * @param configuration The MessageManagerConfiguration to be used to construct the MessageManager.
     * @param channelSpecSet The ChannelSpecSet describing the Channels to be used with ChannelSyncManager
     *
     * @see com.alwaysallthetime.messagebeast.manager.ChannelSyncManager#ChannelSyncManager(ActionMessageManager, com.alwaysallthetime.messagebeast.model.TargetWithActionChannelsSpecSet)
     */
    public ChannelSyncManager(AppDotNetClient appDotNetClient, MessageManager.MessageManagerConfiguration configuration, ChannelSpecSet channelSpecSet) {
        mMessageManager = new MessageManager(appDotNetClient, configuration);
        mChannelSpecSet = channelSpecSet;
    }

    /**
     * Create a ChannelSyncManager for a set of Channels.
     *
     * Note that this constructor is not for use with Action Channels.
     *
     * @param messageManager An instance of MessageManager to be used for syncing the Channels.
     * @param channelSpecSet The ChannelSpecSet describing the Channels to be used with ChannelSyncManager
     *
     * @see com.alwaysallthetime.messagebeast.manager.ChannelSyncManager#ChannelSyncManager(ActionMessageManager, com.alwaysallthetime.messagebeast.model.TargetWithActionChannelsSpecSet)
     */
    public ChannelSyncManager(MessageManager messageManager, ChannelSpecSet channelSpecSet) {
        mMessageManager = messageManager;
        mChannelSpecSet = channelSpecSet;
    }

    /**
     * Create a ChannelSyncManager to be used with a Channel and a set of Action Channels. This
     * constructor creates a MessageManager and ActionMessageManager; it is convenient for use
     * cases that don't require the use of these Managers outside of the ChannelSyncManager.
     *
     * @param appDotNetClient The AppDotNetClient used to make requests. This will be used to construct
     *                        the MessageManager.
     * @param configuration The MessageManagerConfiguration to be used to construct the MessageManager.
     * @param channelSpecSet The TargetWithActionChannelsSpecSet describing the Channels to be used with ChannelSyncManager
     *
     * @see com.alwaysallthetime.messagebeast.manager.ChannelSyncManager#ChannelSyncManager(ActionMessageManager, com.alwaysallthetime.messagebeast.model.TargetWithActionChannelsSpecSet)
     */
    public ChannelSyncManager(AppDotNetClient appDotNetClient, MessageManager.MessageManagerConfiguration configuration, TargetWithActionChannelsSpecSet channelSpecSet) {
        mMessageManager = new MessageManager(appDotNetClient, configuration);
        mActionMessageManager = ActionMessageManager.getInstance(mMessageManager);
        mTargetWithActionChannelsSpecSet = channelSpecSet;
    }

    /**
     * Create a ChannelSyncManager to be used with a Channel and a set of Action Channels.
     *
     * @param actionMessageManager An instance of ActionMessageManager.
     * @param channelSpecSet The TargetWithActionChannelsSpecSet describing the Channels to be used with ChannelSyncManager
     */
    public ChannelSyncManager(ActionMessageManager actionMessageManager, TargetWithActionChannelsSpecSet channelSpecSet) {
        mActionMessageManager = actionMessageManager;
        mMessageManager = mActionMessageManager.getMessageManager();
        mTargetWithActionChannelsSpecSet = channelSpecSet;
    }

    /**
     * Initialize the Channels described by the spec(s) passed when constructing this class.
     *
     * This method must be called before any operations can be performed with a ChannelSyncManager.
     *
     * @param initializedHandler a ChannelsInitializedHandler
     */
    public void initChannels(final ChannelsInitializedHandler initializedHandler) {
        if(mTargetWithActionChannelsSpecSet != null) {
            mActionChannels = new HashMap<String, Channel>(mTargetWithActionChannelsSpecSet.getNumActionChannels());
            initChannel(mTargetWithActionChannelsSpecSet.getTargetChannelSpec(), new ChannelInitializedHandler() {
                @Override
                public void onChannelInitialized(Channel channel) {
                    mTargetChannel = channel;
                    initActionChannels(0, initializedHandler);
                }

                @Override
                public void onException() {
                    initializedHandler.onException();
                }
            });
        } else {
            //just multiple "regular" channels
            mChannels = new HashMap<String, Channel>(mChannelSpecSet.getNumChannels());
            initChannels(0, initializedHandler);
        }
    }

    /**
     * Check the FullSyncStatus for the Channels associated with this manager and begin syncing
     * if all Channels do not already have a FullSyncState of COMPLETE.
     *
     * The MessageManager.retrieveAndPersistAllMessages() method is used to perform the sync.
     *
     * @param handler ChannelSyncStatusHandler
     *
     * @see com.alwaysallthetime.messagebeast.model.FullSyncState
     * @see com.alwaysallthetime.messagebeast.manager.ChannelSyncManager#checkFullSyncStatus(boolean, com.alwaysallthetime.messagebeast.manager.ChannelSyncManager.ChannelSyncStatusHandler)
     */
    public void checkFullSyncStatus(ChannelSyncStatusHandler handler) {
        checkFullSyncStatus(true, handler);
    }

    /**
     * Check the FullSyncStatus for the Channels associated with this manager.
     *
     * If the parameter automaticallyResumeSyncIfPreviouslyStarted is false, then you should override the
     * ChannelSyncStatusHandler.onSyncIncomplete() method to handle a FullSyncState of STARTED
     * (e.g. show a dialog "would you like to resume syncing these channels?").
     *
     * The MessageManager.retrieveAndPersistAllMessages() method is used to perform the sync.
     *
     * @param handler ChannelSyncStatusHandler
     *
     * @see com.alwaysallthetime.messagebeast.model.FullSyncState
     * @see com.alwaysallthetime.messagebeast.manager.ChannelSyncManager#checkFullSyncStatus(boolean, com.alwaysallthetime.messagebeast.manager.ChannelSyncManager.ChannelSyncStatusHandler)
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#retrieveAndPersistAllMessages(String, com.alwaysallthetime.messagebeast.manager.MessageManager.MessageManagerSyncResponseHandler)
     */
    public void checkFullSyncStatus(boolean automaticallyResumeSyncIfPreviouslyStarted, ChannelSyncStatusHandler handler) {
        FullSyncState state = mMessageManager.getFullSyncState(getChannelsArray());
        if(state == FullSyncState.COMPLETE) {
            handler.onSyncComplete();
        } else if(state == FullSyncState.NOT_STARTED) {
            startFullSync(handler);
            handler.onSyncStarted();
        } else {
            if(automaticallyResumeSyncIfPreviouslyStarted) {
                startFullSync(handler);
                handler.onSyncStarted();
            } else {
                handler.onSyncIncomplete();
            }
        }
    }

    /**
     * Start a full sync on the channels associated with this manager.
     *
     * The MessageManager.retrieveAndPersistAllMessages() method is used to perform the sync.
     *
     * @param handler ChannelSyncStatusHandler
     *
     * @see com.alwaysallthetime.messagebeast.manager.MessageManager#retrieveAndPersistAllMessages(String, com.alwaysallthetime.messagebeast.manager.MessageManager.MessageManagerSyncResponseHandler)
     */
    public void startFullSync(final ChannelSyncStatusHandler handler) {
        mMessageManager.retrieveAndPersistAllMessages(getChannelsArray(), new MessageManager.MessageManagerMultiChannelSyncResponseHandler() {
            @Override
            public void onSuccess() {
                handler.onSyncComplete();
            }

            @Override
            public void onError(Exception exception) {
                Log.e(TAG, exception.getMessage(), exception);
                handler.onSyncException(exception);
            }
        });
    }

    /**
     * Retrieve the newest messages in all channels associated with this manager.
     *
     * @param refreshHandler ChannelRefreshHandler the handler that will be passed
     *                       a ChannelRefreshResultSet upon completion of this operation.
     */
    public void retrieveNewestMessages(final ChannelRefreshHandler refreshHandler) {
        if(mTargetWithActionChannelsSpecSet != null) {
            final ChannelRefreshResultSet refreshResultSet = new ChannelRefreshResultSet();
            retrieveNewestActionChannelMessages(0, refreshResultSet, new ChannelRefreshHandler() {
                @Override
                public void onComplete(ChannelRefreshResultSet result) {
                    MessageFilter messageFilter = mTargetWithActionChannelsSpecSet.getTargetChannelSpec().getMessageFilter();
                    boolean canRetrieve = mMessageManager.retrieveNewestMessages(mTargetChannel.getId(), messageFilter, new MessageManager.MessageManagerResponseHandler() {
                        @Override
                        public void onSuccess(List<MessagePlus> responseData) {
                            ChannelRefreshResult refreshResult = new ChannelRefreshResult(mTargetChannel, responseData, getExcludedResults());
                            refreshResultSet.addRefreshResult(refreshResult);
                            if(refreshHandler != null) {
                                refreshHandler.onComplete(refreshResultSet);
                            }
                        }

                        @Override
                        public void onError(Exception exception) {
                            Log.e(TAG, exception.getMessage(), exception);

                            ChannelRefreshResult refreshResult = new ChannelRefreshResult(mTargetChannel, exception);
                            refreshResultSet.addRefreshResult(refreshResult);
                            if(refreshHandler != null) {
                                refreshHandler.onComplete(refreshResultSet);
                            }
                        }
                    });
                    if(!canRetrieve) {
                        refreshResultSet.addRefreshResult(new ChannelRefreshResult(mTargetChannel));
                        if(refreshHandler != null) {
                            refreshHandler.onComplete(refreshResultSet);
                        }
                    }
                }
            });
        } else {
            retrieveNewestMessagesInChannelsList(0, new ChannelRefreshResultSet(), refreshHandler);
        }
    }

    private void retrieveNewestMessagesInChannelsList(final int index, final ChannelRefreshResultSet refreshResultSet, final ChannelRefreshHandler refreshHandler) {
        if(index >= mChannelSpecSet.getNumChannels()) {
            refreshHandler.onComplete(refreshResultSet);
        } else {
            final ChannelSpec spec = mChannelSpecSet.getChannelSpecAtIndex(index);
            final Channel channel = mChannels.get(spec);
            boolean canRetrieve = mMessageManager.retrieveNewestMessages(channel.getId(), spec.getMessageFilter(), new MessageManager.MessageManagerResponseHandler() {
                @Override
                public void onSuccess(List<MessagePlus> responseData) {
                    ChannelRefreshResult refreshResult = new ChannelRefreshResult(channel, responseData);
                    refreshResultSet.addRefreshResult(refreshResult);
                    retrieveNewestMessagesInChannelsList(index + 1, refreshResultSet, refreshHandler);
                }

                @Override
                public void onError(Exception exception) {
                    Log.e(TAG, exception.getMessage(), exception);

                    ChannelRefreshResult refreshResult = new ChannelRefreshResult(channel, exception);
                    refreshResultSet.addRefreshResult(refreshResult);
                    retrieveNewestMessagesInChannelsList(index + 1, refreshResultSet, refreshHandler);
                }
            });

            if(!canRetrieve) {
                refreshResultSet.addRefreshResult(new ChannelRefreshResult(channel));
                retrieveNewestMessagesInChannelsList(index + 1, refreshResultSet, refreshHandler);
            }
        }
    }

    private void retrieveNewestActionChannelMessages(final int index, final ChannelRefreshResultSet refreshResultSet, final ChannelRefreshHandler refreshHandler) {
        if(index >= mTargetWithActionChannelsSpecSet.getNumActionChannels()) {
            refreshHandler.onComplete(refreshResultSet);
        } else {
            final Channel actionChannel = mActionChannels.get(mTargetWithActionChannelsSpecSet.getActionChannelActionTypeAtIndex(index));
            boolean canRetrieve = mActionMessageManager.retrieveNewestMessages(actionChannel.getId(), new MessageManager.MessageManagerResponseHandler() {
                @Override
                public void onSuccess(List<MessagePlus> responseData) {
                    ChannelRefreshResult refreshResult = new ChannelRefreshResult(actionChannel, responseData);
                    refreshResultSet.addRefreshResult(refreshResult);
                    retrieveNewestActionChannelMessages(index + 1, refreshResultSet, refreshHandler);
                }

                @Override
                public void onError(Exception exception) {
                    Log.e(TAG, exception.getMessage(), exception);

                    ChannelRefreshResult refreshResult = new ChannelRefreshResult(actionChannel, exception);
                    refreshResultSet.addRefreshResult(refreshResult);
                    retrieveNewestActionChannelMessages(index + 1, refreshResultSet, refreshHandler);
                }
            });

            if(!canRetrieve) {
                refreshResultSet.addRefreshResult(new ChannelRefreshResult(actionChannel));
                retrieveNewestActionChannelMessages(index + 1, refreshResultSet, refreshHandler);
            }
        }
    }

    /**
     * Get the MessageManager used by this ChannelSyncManager.
     *
     * @return the MessageManager used by this ChannelSyncManager
     */
    public MessageManager getMessageManager() {
        return mMessageManager;
    }

    /**
     * Get the ActionMessageManager used by this ChannelSyncManager. Will be null if this
     * ChannelSyncManager was not constructed with a TargetWithActionChannelsSpecSet.
     *
     * @return the ActionMessageManager used by this ChannelSyncManager, or null if it does not
     * use one.
     */
    public ActionMessageManager getActionMessageManager() {
        return mActionMessageManager;
    }

    /**
     * Get the target Channel for this manager's Action Channels. This will be null if you did
     * not construct this class with a TargetWithActionChannelsSpecSet. Note that you should be
     * calling initChannels() before attempting to call this method.
     *
     * @return the target Channel for this manager's Action Channels, or null if none exists.
     */
    public Channel getTargetChannel() {
        return mTargetChannel;
    }

    /**
     * Get an Action Channel initialized by this manager. This will be null if you did
     * not construct this class with a TargetWithActionChannelsSpecSet. Note that you should be
     * calling initChannels() before attempting to call this method.
     *
     * @param actionType the action_type value in the Action Channel's metadata Annotation.
     * @return The Action Channel with the specified action type, or null if none exists.
     */
    public Channel getActionChannel(String actionType) {
        return mActionChannels.get(actionType);
    }

    /**
     * Return a Map of Channels whose keys are Channel ids.
     * This will be null if you constructed this manager with a TargetWithActionChannelsSpecSet.
     * Note that you should be calling initChannels() before attempting to call this method.
     *
     * @return a Map of Channels whose keys are Channel ids, or null if none exists.
     *
     * @see ChannelSyncManager#getTargetChannel()
     * @see ChannelSyncManager#getActionChannel(String)
     */
    public HashMap<String, Channel> getChannels() {
        return new HashMap<String, Channel>(mChannels);
    }

    private void initChannels(final int index, final ChannelsInitializedHandler initializedHandler) {
        if(index >= mChannelSpecSet.getNumChannels()) {
            initializedHandler.onChannelsInitialized();
            ADNApplication.getContext().sendBroadcast(new Intent(INTENT_ACTION_CHANNELS_INITIALIZED));
        } else {
            ChannelSpec spec = mChannelSpecSet.getChannelSpecAtIndex(index);
            initChannel(spec, new ChannelInitializedHandler() {
                @Override
                public void onChannelInitialized(Channel channel) {
                    mChannels.put(channel.getId(), channel);
                    initChannels(index+1, initializedHandler);
                }

                @Override
                public void onException() {
                    initializedHandler.onException();
                }
            });
        }
    }

    private void initActionChannels(final int index, final ChannelsInitializedHandler initializedHandler) {
        if(index >= mTargetWithActionChannelsSpecSet.getNumActionChannels()) {
            initializedHandler.onChannelsInitialized();
            ADNApplication.getContext().sendBroadcast(new Intent(INTENT_ACTION_CHANNELS_INITIALIZED));
        } else {
            final String actionType = mTargetWithActionChannelsSpecSet.getActionChannelActionTypeAtIndex(index);
            initActionChannel(actionType, new Runnable() {
                @Override
                public void run() {
                    if(mActionChannels.get(actionType) != null) {
                        initActionChannels(index + 1, initializedHandler);
                    } else {
                        initializedHandler.onException();
                    }
                }
            });
        }
    }

    private void initChannel(final ChannelSpec channelSpec, final ChannelInitializedHandler channelInitializedHandler) {
        PrivateChannelUtility.getOrCreateChannel(mMessageManager.getClient(), channelSpec.getType(), new PrivateChannelUtility.PrivateChannelGetOrCreateHandler() {
            @Override
            public void onResponse(Channel channel, boolean createdNewChannel) {
                mMessageManager.setParameters(channel.getId(), channelSpec.getQueryParameters());
                channelInitializedHandler.onChannelInitialized(channel);
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, error.getMessage(), error);
                channelInitializedHandler.onException();
            }
        });
    }

    private void initActionChannel(final String actionType, final Runnable completionRunnable) {
        mActionMessageManager.initActionChannel(actionType, new ActionMessageManager.ActionChannelInitializedHandler() {
            @Override
            public void onInitialized(Channel channel) {
                mActionChannels.put(actionType, channel);
                completionRunnable.run();
            }

            @Override
            public void onException(Exception exception) {
                Log.e(TAG, exception.getMessage(), exception);
                completionRunnable.run();
            }
        });
    }

    private Channel[] getChannelsArray() {
        Channel[] channels = new Channel[(mTargetChannel != null ? 1 : 0) + mActionChannels.size()];
        int i = 0;
        if(mTargetChannel != null) {
            channels[0] = mTargetChannel;
            i++;
        }
        for(Channel c : mActionChannels.values()) {
            channels[i] = c;
            i++;
        }

        return channels;
    }
}