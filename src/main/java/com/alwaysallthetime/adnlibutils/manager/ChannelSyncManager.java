package com.alwaysallthetime.adnlibutils.manager;

import android.content.Intent;
import android.util.Log;

import com.alwaysallthetime.adnlib.data.Channel;
import com.alwaysallthetime.adnlibutils.ADNApplication;
import com.alwaysallthetime.adnlibutils.FullSyncState;
import com.alwaysallthetime.adnlibutils.PrivateChannelUtility;
import com.alwaysallthetime.adnlibutils.model.ChannelSpec;
import com.alwaysallthetime.adnlibutils.model.MessagePlus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChannelSyncManager {

    public static final String INTENT_ACTION_CHANNELS_INITIALIZED = "com.alwaysallthetime.adnlibutils.manager.ChannelSyncManager.intent.channelsInitialized";

    private static final String TAG = "ADNLibUtils_ChannelSyncManager";

    private MessageManager mMessageManager;
    private ActionMessageManager mActionMessageManager;

    private ChannelSpec mTargetChannelSpec;
    private List<String> mActionChannelActionTypes;
    private Map<String, Channel> mActionChannels;

    private Channel mTargetChannel;

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

    public ChannelSyncManager(ActionMessageManager actionMessageManager, ChannelSpec channelSpec) {
        this(actionMessageManager, channelSpec, null);
    }

    public ChannelSyncManager(ActionMessageManager actionMessageManager, ChannelSpec targetChannelSpec, String... actionChannelActionTypes) {
        mActionMessageManager = actionMessageManager;
        mMessageManager = mActionMessageManager.getMessageManager();
        mTargetChannelSpec = targetChannelSpec;

        mActionChannelActionTypes = new ArrayList<String>(actionChannelActionTypes != null ? actionChannelActionTypes.length : 0);
        mActionChannels = new HashMap<String, Channel>(mActionChannelActionTypes.size());
        if(actionChannelActionTypes != null) {
            for(String type : actionChannelActionTypes) {
                mActionChannelActionTypes.add(type);
            }
        }
    }

    public void initChannels(final ChannelsInitializedHandler initializedHandler) {
        initChannel(mTargetChannelSpec, new Runnable() {
            @Override
            public void run() {
                if(mTargetChannel != null) {
                    initActionChannels(0, initializedHandler);
                } else {
                    initializedHandler.onException();
                }
            }
        });
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
        if(index >= mActionChannelActionTypes.size()) {
            completionRunnable.run();
        } else {
            Channel actionChannel = mActionChannels.get(mActionChannelActionTypes.get(index));
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

    private void initActionChannels(final int index, final ChannelsInitializedHandler initializedHandler) {
        if(index >= mActionChannelActionTypes.size()) {
            initializedHandler.onChannelsInitialized();
            ADNApplication.getContext().sendBroadcast(new Intent(INTENT_ACTION_CHANNELS_INITIALIZED));
        } else {
            final String actionType = mActionChannelActionTypes.get(index);
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

    private void initChannel(final ChannelSpec channelSpec, final Runnable completionRunnable) {
        PrivateChannelUtility.getOrCreateChannel(mMessageManager.getClient(), channelSpec.getType(), new PrivateChannelUtility.PrivateChannelGetOrCreateHandler() {
            @Override
            public void onResponse(Channel channel, boolean createdNewChannel) {
                mTargetChannel = channel;
                mMessageManager.setParameters(mTargetChannel.getId(), channelSpec.getQueryParameters());
                completionRunnable.run();
            }

            @Override
            public void onError(Exception error) {
                Log.d(TAG, error.getMessage(), error);
                completionRunnable.run();
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