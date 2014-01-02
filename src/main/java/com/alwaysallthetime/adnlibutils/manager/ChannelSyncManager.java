package com.alwaysallthetime.adnlibutils.manager;

import android.content.Intent;
import android.util.Log;

import com.alwaysallthetime.adnlib.data.Channel;
import com.alwaysallthetime.adnlibutils.ADNApplication;
import com.alwaysallthetime.adnlibutils.PrivateChannelUtility;
import com.alwaysallthetime.adnlibutils.model.ChannelRefreshResult;
import com.alwaysallthetime.adnlibutils.model.ChannelRefreshResultSet;
import com.alwaysallthetime.adnlibutils.model.ChannelSpec;
import com.alwaysallthetime.adnlibutils.model.ChannelSpecSet;
import com.alwaysallthetime.adnlibutils.model.FullSyncState;
import com.alwaysallthetime.adnlibutils.model.MessagePlus;
import com.alwaysallthetime.adnlibutils.model.TargetWithActionChannelsSpecSet;

import java.util.ArrayList;
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
 * @see com.alwaysallthetime.adnlibutils.manager.ActionMessageManager
 * @see com.alwaysallthetime.adnlibutils.manager.MessageManager
 */
public class ChannelSyncManager {

    public static final String INTENT_ACTION_CHANNELS_INITIALIZED = "com.alwaysallthetime.adnlibutils.manager.ChannelSyncManager.intent.channelsInitialized";

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
        public void onComplete(ChannelRefreshResultSet result);
    }

    /**
     * Create a ChannelSyncManager for a set of Channels.
     *
     * Note that this constructor is not for use with Action Channels.
     *
     * @param messageManager An instance of MessageManager to be used for syncing the Channels.
     * @param channelSpecSet The ChannelSpecSet describing the Channels to be used with ChannelSyncManager
     *
     * @see com.alwaysallthetime.adnlibutils.manager.ChannelSyncManager#ChannelSyncManager(ActionMessageManager, com.alwaysallthetime.adnlibutils.model.TargetWithActionChannelsSpecSet)
     */
    public ChannelSyncManager(MessageManager messageManager, ChannelSpecSet channelSpecSet) {
        mMessageManager = messageManager;
        mChannelSpecSet = channelSpecSet;
    }

    /**
     * Create a ChannelSyncManager to be used with a Channel and a set of Action Channels
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
                    if(mTargetChannel != null) {
                        initActionChannels(0, initializedHandler);
                    } else {
                        initializedHandler.onException();
                    }
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
     * @see com.alwaysallthetime.adnlibutils.model.FullSyncState
     * @see com.alwaysallthetime.adnlibutils.manager.ChannelSyncManager#checkFullSyncStatus(boolean, com.alwaysallthetime.adnlibutils.manager.ChannelSyncManager.ChannelSyncStatusHandler)
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
     * @see com.alwaysallthetime.adnlibutils.model.FullSyncState
     * @see com.alwaysallthetime.adnlibutils.manager.ChannelSyncManager#checkFullSyncStatus(boolean, com.alwaysallthetime.adnlibutils.manager.ChannelSyncManager.ChannelSyncStatusHandler)
     * @see com.alwaysallthetime.adnlibutils.manager.MessageManager#retrieveAndPersistAllMessages(String, com.alwaysallthetime.adnlibutils.manager.MessageManager.MessageManagerSyncResponseHandler)
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
     * @see com.alwaysallthetime.adnlibutils.manager.MessageManager#retrieveAndPersistAllMessages(String, com.alwaysallthetime.adnlibutils.manager.MessageManager.MessageManagerSyncResponseHandler)
     */
    public void startFullSync(final ChannelSyncStatusHandler handler) {
        mMessageManager.retrieveAndPersistAllMessages(getChannelsArray(), new MessageManager.MessageManagerMultiChannelSyncResponseHandler() {
            @Override
            public void onSuccess() {
                handler.onSyncComplete();
            }

            @Override
            public void onError(Exception exception) {
                Log.d(TAG, exception.getMessage(), exception);
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
            boolean canRetrieve = mMessageManager.retrieveNewestMessages(mTargetChannel.getId(), new MessageManager.MessageManagerResponseHandler() {
                @Override
                public void onSuccess(final List<MessagePlus> responseData, final boolean appended) {
                    ChannelRefreshResult refreshResult = new ChannelRefreshResult(mTargetChannel, responseData, appended);
                    refreshResultSet.addRefreshResult(refreshResult);
                    retrieveNewestActionChannelMessages(0, refreshHandler, refreshResultSet);
                }

                @Override
                public void onError(Exception exception) {
                    Log.e(TAG, exception.getMessage(), exception);

                    ChannelRefreshResult refreshResult = new ChannelRefreshResult(mTargetChannel, exception);
                    refreshResultSet.addRefreshResult(refreshResult);
                    retrieveNewestActionChannelMessages(0, refreshHandler, refreshResultSet);
                }
            });

            if(!canRetrieve) {
                refreshResultSet.addRefreshResult(new ChannelRefreshResult(mTargetChannel));
                retrieveNewestActionChannelMessages(0, refreshHandler, refreshResultSet);
            }
        } else {
            retrieveNewestMessagesInChannelsList(0, refreshHandler, new ChannelRefreshResultSet());
        }
    }

    private void retrieveNewestMessagesInChannelsList(final int index, final ChannelRefreshHandler refreshHandler, final ChannelRefreshResultSet refreshResultSet) {
        if(index >= mChannelSpecSet.getNumChannels()) {
            refreshHandler.onComplete(refreshResultSet);
        } else {
            final Channel channel = mChannels.get(mChannelSpecSet.getChannelSpecAtIndex(index));
            boolean canRetrieve = mMessageManager.retrieveNewestMessages(channel.getId(), new MessageManager.MessageManagerResponseHandler() {
                @Override
                public void onSuccess(List<MessagePlus> responseData, boolean appended) {
                    ChannelRefreshResult refreshResult = new ChannelRefreshResult(channel, responseData, appended);
                    refreshResultSet.addRefreshResult(refreshResult);
                    retrieveNewestMessagesInChannelsList(index + 1, refreshHandler, refreshResultSet);
                }

                @Override
                public void onError(Exception exception) {
                    Log.e(TAG, exception.getMessage(), exception);

                    ChannelRefreshResult refreshResult = new ChannelRefreshResult(channel, exception);
                    refreshResultSet.addRefreshResult(refreshResult);
                    retrieveNewestMessagesInChannelsList(index + 1, refreshHandler, refreshResultSet);
                }
            });

            if(!canRetrieve) {
                refreshResultSet.addRefreshResult(new ChannelRefreshResult(channel));
                retrieveNewestMessagesInChannelsList(index + 1, refreshHandler, refreshResultSet);
            }
        }
    }

    private void retrieveNewestActionChannelMessages(final int index, final ChannelRefreshHandler refreshHandler, final ChannelRefreshResultSet refreshResultSet) {
        if(index >= mTargetWithActionChannelsSpecSet.getNumActionChannels()) {
            refreshHandler.onComplete(refreshResultSet);
        } else {
            final Channel actionChannel = mActionChannels.get(mTargetWithActionChannelsSpecSet.getActionChannelActionTypeAtIndex(index));
            boolean canRetrieve = mActionMessageManager.retrieveNewestMessages(actionChannel.getId(), mTargetChannel.getId(), new MessageManager.MessageManagerResponseHandler() {
                @Override
                public void onSuccess(List<MessagePlus> responseData, boolean appended) {
                    ChannelRefreshResult refreshResult = new ChannelRefreshResult(actionChannel, responseData, appended);
                    refreshResultSet.addRefreshResult(refreshResult);
                    retrieveNewestActionChannelMessages(index + 1, refreshHandler, refreshResultSet);
                }

                @Override
                public void onError(Exception exception) {
                    Log.e(TAG, exception.getMessage(), exception);

                    ChannelRefreshResult refreshResult = new ChannelRefreshResult(actionChannel, exception);
                    refreshResultSet.addRefreshResult(refreshResult);
                    retrieveNewestActionChannelMessages(index + 1, refreshHandler, refreshResultSet);
                }
            });

            if(!canRetrieve) {
                refreshResultSet.addRefreshResult(new ChannelRefreshResult(actionChannel));
                retrieveNewestActionChannelMessages(index + 1, refreshHandler, refreshResultSet);
            }
        }
    }

    public Channel getTargetChannel() {
        return mTargetChannel;
    }

    public Channel getActionChannel(String actionType) {
        return mActionChannels.get(actionType);
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
            initActionChannel(actionType, mTargetChannel, new Runnable() {
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
                Log.d(TAG, error.getMessage(), error);
                channelInitializedHandler.onException();
            }
        });
    }

    private void initActionChannel(final String actionType, Channel targetChannel, final Runnable completionRunnable) {
        mActionMessageManager.initActionChannel(actionType, targetChannel, new ActionMessageManager.ActionChannelInitializedHandler() {
            @Override
            public void onInitialized(Channel channel) {
                mActionChannels.put(actionType, channel);
                completionRunnable.run();
            }

            @Override
            public void onException(Exception exception) {
                Log.d(TAG, exception.getMessage(), exception);
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