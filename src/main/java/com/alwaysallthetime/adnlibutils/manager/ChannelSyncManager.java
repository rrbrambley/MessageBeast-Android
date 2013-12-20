package com.alwaysallthetime.adnlibutils.manager;

import android.util.Log;

import com.alwaysallthetime.adnlib.data.Channel;
import com.alwaysallthetime.adnlibutils.PrivateChannelUtility;
import com.alwaysallthetime.adnlibutils.model.ChannelSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChannelSyncManager {

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

    public Channel getTargetChannel() {
        return mTargetChannel;
    }

    public Channel getActionChannel(String actionType) {
        return mActionChannels.get(actionType);
    }

    private void initActionChannels(final int index, final ChannelsInitializedHandler initializedHandler) {
        if(index >= mActionChannelActionTypes.size()) {
            initializedHandler.onChannelsInitialized();
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
}
