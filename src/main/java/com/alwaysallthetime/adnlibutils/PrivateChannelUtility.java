package com.alwaysallthetime.adnlibutils;

import android.util.Log;

import com.alwaysallthetime.adnlib.AppDotNetClient;
import com.alwaysallthetime.adnlib.GeneralParameter;
import com.alwaysallthetime.adnlib.QueryParameters;
import com.alwaysallthetime.adnlib.data.Annotation;
import com.alwaysallthetime.adnlib.data.Channel;
import com.alwaysallthetime.adnlib.data.ChannelList;
import com.alwaysallthetime.adnlib.data.User;
import com.alwaysallthetime.adnlib.response.ChannelListResponseHandler;
import com.alwaysallthetime.adnlib.response.ChannelResponseHandler;
import com.alwaysallthetime.adnlibutils.model.FullSyncState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A utility for creating and retrieving channels that are intended to be used
 * privately, by a single user - based on how Ohai does it.<br><br>
 *
 * Additionally, this can be used to create new, or retrieve existing Action Channels to be used
 * with the ActionMessageManager.
 *
 * Often, you won't need to interface with this class directly, but instead you can rely on
 * channel initialization methods in the ChannelSyncManager and ActionMessageManager.
 *
 * @see <a href="https://github.com/appdotnet/object-metadata/blob/master/channel-types/net.app.ohai.journal.md#finding-the-journal">https://github.com/appdotnet/object-metadata/blob/master/channel-types/net.app.ohai.journal.md#finding-the-journal</a>
 * @see com.alwaysallthetime.adnlibutils.manager.ActionMessageManager
 * @see com.alwaysallthetime.adnlibutils.manager.ChannelSyncManager
 */
public class PrivateChannelUtility {

    private static final String TAG = "ADNLibUtils_PrivateChannelUtility";

    public static final String CHANNEL_TYPE_ACTION = "com.alwaysallthetime.action";

    public static final String CHANNEL_ANNOTATION_TYPE_METADATA = "com.alwaysallthetime.action.metadata";
    public static final String ACTION_METADATA_KEY_TARGET_CHANNEL_ID = "channel_id";
    public static final String ACTION_METADATA_KEY_ACTION_TYPE = "action_type";

    public static final String MESSAGE_ANNOTATION_TARGET_MESSAGE = "com.alwaysallthetime.action.target_message";
    public static final String TARGET_MESSAGE_KEY_ID = "id";

    private static HashMap<String, Channel> sChannels = new HashMap<String, Channel>();
    private static HashMap<String, HashMap<String, Channel>> sActionChannels = new HashMap<String, HashMap<String, Channel>>();

    public interface PrivateChannelHandler {
        public void onResponse(Channel channel);
        public void onError(Exception error);
    }

    public interface PrivateChannelGetOrCreateHandler {
        public void onResponse(Channel channel, boolean createdNewChannel);
        public void onError(Exception error);
    }

    /**
     * Get the existing Channel of the provided type, or create and return a new one if one hasn't already
     * been created.<br><br>
     *
     * In the event that the user has more than one channel of the specified type, the algorithm
     * used to return the Channel is the same as that which is described in the Ohai Channel documentation.<br><br>
     *
     * @param client the AppDotNetClient to use for the request
     * @param channelType the Channel type
     * @param handler the response handler
     *
     * @see <a href="https://github.com/appdotnet/object-metadata/blob/master/channel-types/net.app.ohai.journal.md#finding-the-journal">https://github.com/appdotnet/object-metadata/blob/master/channel-types/net.app.ohai.journal.md#finding-the-journal</a>
     */
    public static void getOrCreateChannel(final AppDotNetClient client, final String channelType, final PrivateChannelGetOrCreateHandler handler) {
        Channel channel = getChannel(channelType);
        if(channel == null) {
            retrieveChannel(client, channelType, new PrivateChannelHandler() {
                @Override
                public void onResponse(Channel channel) {
                    if(channel == null) {
                        createChannel(client, channelType, new PrivateChannelHandler() {
                            @Override
                            public void onResponse(Channel channel) {
                                handler.onResponse(channel, true);
                            }

                            @Override
                            public void onError(Exception error) {
                                Log.e(TAG, error.getMessage(), error);
                                handler.onError(error);
                            }
                        });
                    } else {
                        handler.onResponse(channel, false);
                    }
                }

                @Override
                public void onError(Exception error) {
                    Log.e(TAG, error.getMessage(), error);
                    handler.onError(error);
                }
            });
        } else {
            handler.onResponse(channel, false);
        }
    }

    /**
     * Get the existing Action Channel of the specified action type for specified target Channel.
     * If one doesn't already exist, then create a new one and return it. Rather than calling this
     * method directly, you will probably want to use the ActionMessageManager's initActionChannel
     * method.<br><br>
     *
     * An Action Channel is a Channel of type com.alwaysallthetime.action. It contains a
     * com.alwaysallthetime.action.metadata Annotation with the keys channel_id and action_type,
     * whose values are the target Channel id and action type, respectively.<br><br>
     *
     * In the event that the user has more than one Channel of the specified action type, pointing
     * to the specified target Channel, the algorithm used to return the Channel is the same as that
     * which is described in the Ohai Channel documentation.<br><br>
     *
     * @param client the AppDotNetClient to use for the request
     * @param actionType the action type associated with the Channel.
     * @param targetChannel the target Channel for the Action Channel
     * @param handler the response handler to deliver the response
     *
     * @see <a href="https://github.com/appdotnet/object-metadata/blob/master/channel-types/net.app.ohai.journal.md#finding-the-journal">https://github.com/appdotnet/object-metadata/blob/master/channel-types/net.app.ohai.journal.md#finding-the-journal</a>
     * @see com.alwaysallthetime.adnlibutils.manager.ActionMessageManager
     */
    public static void getOrCreateActionChannel(final AppDotNetClient client, final String actionType, final Channel targetChannel, final PrivateChannelGetOrCreateHandler handler) {
        Channel actionChannel = getActionChannel(actionType, targetChannel.getId());
        if(actionChannel == null) {
            retrieveActionChannel(client, actionType, targetChannel.getId(), new PrivateChannelHandler() {
                @Override
                public void onResponse(Channel channel) {
                    if(channel == null) {
                        createActionChannel(client, actionType, targetChannel.getId(), new PrivateChannelHandler() {
                            @Override
                            public void onResponse(Channel channel) {
                                handler.onResponse(channel, true);
                            }

                            @Override
                            public void onError(Exception error) {
                                Log.d(TAG, error.getMessage(), error);
                                handler.onError(error);
                            }
                        });
                    } else {
                        handler.onResponse(channel, false);
                    }
                }

                @Override
                public void onError(Exception error) {
                    Log.e(TAG, error.getMessage(), error);
                    handler.onError(error);
                }
            });
        } else {
            handler.onResponse(actionChannel, false);
        }
    }

    /**
     * Unsubscribe from a Channel and remove the persisted copy.
     *
     * @param client the AppDotNetClient to use for the request
     * @param channel the Channel to unsubscribe from
     * @param handler the response handler
     */
    public static void unsubscribe(final AppDotNetClient client, final Channel channel, final PrivateChannelHandler handler) {
        client.unsubscribeChannel(channel, new ChannelResponseHandler() {
            @Override
            public void onSuccess(Channel responseData) {
                sChannels.remove(channel.getType());
                ADNSharedPreferences.deletePrivateChannel(channel);
                handler.onResponse(responseData);
            }

            @Override
            public void onError(Exception error) {
                super.onError(error);
                handler.onError(error);
            }
        });
    }

    private static Channel getChannel(String channelType) {
        Channel c = sChannels.get(channelType);
        if(c == null) {
            c = ADNSharedPreferences.getPrivateChannel(channelType);
            sChannels.put(channelType, c);
        }
        return c;
    }

    private static Channel getActionChannel(String actionType, String targetChannelId) {
        HashMap<String, Channel> actionChannels = getOrCreateActionChannelsMap(targetChannelId);
        if(actionChannels.get(actionType) == null) {
            Channel actionChannel = ADNSharedPreferences.getActionChannel(actionType, targetChannelId);
            actionChannels.put(actionType, actionChannel);
        }
        return actionChannels.get(actionType);
    }

    private static void retrieveChannel(AppDotNetClient client, final String channelType, final PrivateChannelHandler handler) {
        QueryParameters params = new QueryParameters();
        params.put("channel_types", channelType);
        client.retrieveCurrentUserSubscribedChannels(params, new ChannelListResponseHandler() {
            @Override
            public void onSuccess(ChannelList responseData) {
                Channel theChannel = getOldestPrivateChannel(responseData);

                if(theChannel != null) {
                    ADNSharedPreferences.savePrivateChannel(theChannel);
                }
                sChannels.put(channelType, theChannel);
                handler.onResponse(theChannel);
            }

            @Override
            public void onError(Exception error) {
                handler.onError(error);
            }
        });
    }

    private static void retrieveActionChannel(AppDotNetClient client, final String actionType, final String targetChannelId, final PrivateChannelHandler handler) {
        QueryParameters params = new QueryParameters(GeneralParameter.INCLUDE_CHANNEL_ANNOTATIONS);
        params.put("channel_types", CHANNEL_TYPE_ACTION);
        client.retrieveCurrentUserSubscribedChannels(params, new ChannelListResponseHandler() {
            @Override
            public void onSuccess(ChannelList responseData) {
                Channel theChannel = getOldestActionChannel(responseData, actionType, targetChannelId);

                if(theChannel != null) {
                    ADNSharedPreferences.saveActionChannel(theChannel, actionType, targetChannelId);
                    getOrCreateActionChannelsMap(targetChannelId).put(actionType, theChannel);
                }
                handler.onResponse(theChannel);
            }

            @Override
            public void onError(Exception error) {
                handler.onError(error);
            }
        });
    }

    private static void createChannel(final AppDotNetClient client, final String channelType, final PrivateChannelHandler handler) {
        createChannel(client, channelType, new ArrayList<Annotation>(0), new PrivateChannelHandler() {
            @Override
            public void onResponse(Channel channel) {
                sChannels.put(channelType, channel);
                ADNSharedPreferences.savePrivateChannel(channel);
                handler.onResponse(channel);
            }

            @Override
            public void onError(Exception error) {
                handler.onError(error);
            }
        });
    }

    private static void createActionChannel(final AppDotNetClient client, final String actionType, final String targetChannelId, final PrivateChannelHandler handler) {
        ArrayList<Annotation> channelAnnotations = new ArrayList<Annotation>(1);
        Annotation metadata = new Annotation(CHANNEL_ANNOTATION_TYPE_METADATA);
        HashMap<String, Object> value = new HashMap<String, Object>(2);
        value.put(ACTION_METADATA_KEY_ACTION_TYPE, actionType);
        value.put(ACTION_METADATA_KEY_TARGET_CHANNEL_ID, targetChannelId);
        metadata.setValue(value);
        channelAnnotations.add(metadata);

        createChannel(client, CHANNEL_TYPE_ACTION, channelAnnotations, new PrivateChannelHandler() {
            @Override
            public void onResponse(Channel channel) {
                getOrCreateActionChannelsMap(targetChannelId).put(actionType, channel);
                ADNSharedPreferences.saveActionChannel(channel, actionType, targetChannelId);
                handler.onResponse(channel);
            }

            @Override
            public void onError(Exception error) {
                handler.onError(error);
            }
        });
    }

    private static void createChannel(final AppDotNetClient client, final String channelType, List<Annotation> channelAnnotations, final PrivateChannelHandler handler) {
        User currentUser = ADNSharedPreferences.getToken().getUser();

        Channel c = new Channel();
        c.setType(channelType);

        Channel.Acl writer = new Channel.Acl(true);
        writer.setUserIds(new String[] { currentUser.getId() });
        writer.setAnyUser(false);

        Channel.Acl reader = new Channel.Acl(false);

        c.setWriters(writer);
        c.setReaders(reader);

        for(Annotation a : channelAnnotations) {
            c.addAnnotation(a);
        }

        final QueryParameters params = new QueryParameters(GeneralParameter.INCLUDE_CHANNEL_ANNOTATIONS);
        client.createChannel(c, params, new ChannelResponseHandler() {
            @Override
            public void onSuccess(final Channel responseData) {
                client.subscribeChannel(responseData, params, new ChannelResponseHandler() {
                    @Override
                    public void onSuccess(Channel responseData) {
                        ADNSharedPreferences.setFullSyncState(responseData.getId(), FullSyncState.COMPLETE);
                        handler.onResponse(responseData);
                    }
                });
            }

            @Override
            public void onError(Exception error) {
                handler.onError(error);
            }
        });
    }

    private static Channel getOldestPrivateChannel(ChannelList responseData) {
        Channel theChannel = null;
        if(responseData.size() == 1) {
            theChannel = responseData.get(0);
        } else if(responseData.size() > 1) {
            User currentUser = ADNSharedPreferences.getToken().getUser();
            for(Channel channel : responseData) {
                Channel.Acl writers = channel.getWriters();

                if(channel.getOwner().getId().equals(currentUser.getId()) &&
                        writers.isImmutable() && writers.isCurrentUserAuthorized() && !writers.isAnyUser() && !channel.getReaders().isImmutable()) {
                    if(theChannel == null || Integer.valueOf(channel.getId()) < Integer.valueOf(theChannel.getId())) {
                        theChannel = channel;
                    }
                }
            }
        }
        return theChannel;
    }

    private static Channel getOldestActionChannel(ChannelList responseData, String actionType, String targetChannelId) {
        Channel theChannel = null;
        if(responseData.size() == 1) {
            Channel channel = responseData.get(0);
            if(isMatchingActionChannel(channel, actionType, targetChannelId)) {
                theChannel = channel;
            }
        } else if(responseData.size() > 1) {
            User currentUser = ADNSharedPreferences.getToken().getUser();
            for(Channel channel : responseData) {
                if(isMatchingActionChannel(channel, actionType, targetChannelId)) {
                    Channel.Acl writers = channel.getWriters();

                    if(channel.getOwner().getId().equals(currentUser.getId()) &&
                            writers.isImmutable() && writers.isCurrentUserAuthorized() && !writers.isAnyUser() && !channel.getReaders().isImmutable()) {
                        if(theChannel == null || Integer.valueOf(channel.getId()) < Integer.valueOf(theChannel.getId())) {
                            theChannel = channel;
                        }
                    }
                }
            }
        }
        return theChannel;
    }

    private static boolean isMatchingActionChannel(Channel channel, String actionType, String targetChannelId) {
        Annotation metadata = channel.getFirstAnnotationOfType(CHANNEL_ANNOTATION_TYPE_METADATA);
        if(metadata != null) {
            String metadataActionType = (String) metadata.getValue().get(ACTION_METADATA_KEY_ACTION_TYPE);
            String metadataChannelId = (String) metadata.getValue().get(ACTION_METADATA_KEY_TARGET_CHANNEL_ID);
            if(actionType.equals(metadataActionType) && targetChannelId.equals(metadataChannelId)) {
                return true;
            }
        }
        return false;
    }

    private static HashMap<String, Channel> getOrCreateActionChannelsMap(String targetChannelId) {
        HashMap<String, Channel> actionChannels = sActionChannels.get(targetChannelId);
        if(actionChannels == null) {
            actionChannels = new HashMap<String, Channel>(1);
            sActionChannels.put(targetChannelId, actionChannels);
        }
        return actionChannels;
    }
}
