package com.alwaysallthetime.adnlibutils.manager;

import android.content.Intent;
import android.util.Log;

import com.alwaysallthetime.adnlib.data.Channel;
import com.alwaysallthetime.adnlibutils.ADNApplication;
import com.alwaysallthetime.adnlibutils.PrivateChannelUtility;
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
 * enabling users to mark entries as favorites, or locked entries, respectively. In this scenario,
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

    private TargetWithActionChannelsSpecSet mTargetWithActionChannelsSpecSet;
    private Channel mTargetChannel;
    private Map<String, Channel> mActionChannels;

    private ChannelSpecSet mChannelSpecSet;
    private List<Channel> mChannels;

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

    public ChannelSyncManager(MessageManager messageManager, ChannelSpecSet channelSpecSet) {
        mMessageManager = messageManager;
        mChannelSpecSet = channelSpecSet;
    }

    public ChannelSyncManager(ActionMessageManager actionMessageManager, TargetWithActionChannelsSpecSet channelSpecSet) {
        mActionMessageManager = actionMessageManager;
        mMessageManager = mActionMessageManager.getMessageManager();
        mTargetWithActionChannelsSpecSet = channelSpecSet;
    }

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
            mChannels = new ArrayList<Channel>(mChannelSpecSet.getNumChannels());
            initChannels(0, initializedHandler);
        }
    }

    public void checkFullSyncStatus(ChannelSyncStatusHandler handler) {
        checkFullSyncStatus(true, handler);
    }

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

    public boolean retrieveNewestMessages(final MessageManager.MessageManagerResponseHandler responseHandler) {
        boolean canRetrieve = mMessageManager.retrieveNewestMessages(mTargetChannel.getId(), new MessageManager.MessageManagerResponseHandler() {
            @Override
            public void onSuccess(final List<MessagePlus> responseData, final boolean appended) {
                retrieveNewestActionChannelMessages(0, new Runnable() {
                    @Override
                    public void run() {
                        responseHandler.onSuccess(responseData, appended);
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                Log.e(TAG, exception.getMessage(), exception);
                responseHandler.onError(exception);
            }
        });
        return canRetrieve;
    }

    private void retrieveNewestActionChannelMessages(final int index, final Runnable completionRunnable) {
        if(index >= mTargetWithActionChannelsSpecSet.getNumActionChannels()) {
            completionRunnable.run();
        } else {
            Channel actionChannel = mActionChannels.get(mTargetWithActionChannelsSpecSet.getActionChannelActionTypeAtIndex(index));
            boolean canRetrieve = mActionMessageManager.retrieveNewestMessages(actionChannel.getId(), mTargetChannel.getId(), new MessageManager.MessageManagerResponseHandler() {
                @Override
                public void onSuccess(List<MessagePlus> responseData, boolean appended) {
                    retrieveNewestActionChannelMessages(index + 1, completionRunnable);
                }

                @Override
                public void onError(Exception exception) {
                    Log.e(TAG, exception.getMessage(), exception);
                    completionRunnable.run();
                }
            });

            //TODO: fix this? is this right?
            if(!canRetrieve) {
                mMessageManager.sendAllUnsent(actionChannel.getId());
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
                    mChannels.add(channel);
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
                mMessageManager.setParameters(mTargetChannel.getId(), channelSpec.getQueryParameters());
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