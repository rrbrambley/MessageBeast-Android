package com.alwaysallthetime.adnlibutils;

import com.alwaysallthetime.adnlib.AppDotNetClient;
import com.alwaysallthetime.adnlib.QueryParameters;
import com.alwaysallthetime.adnlib.data.Channel;
import com.alwaysallthetime.adnlib.data.ChannelList;
import com.alwaysallthetime.adnlib.data.User;
import com.alwaysallthetime.adnlib.response.ChannelListResponseHandler;
import com.alwaysallthetime.adnlib.response.ChannelResponseHandler;

import java.util.HashMap;

/**
 * A utility for creating and retrieving channels that are intended to be used
 * privately, by a single user - based on how Ohai does it.
 *
 * See: https://github.com/appdotnet/object-metadata/blob/master/channel-types/net.app.ohai.journal.md
 */
public class PrivateChannelUtility {
    private static HashMap<String, Channel> sChannels = new HashMap<String, Channel>();

    public interface PrivateChannelHandler {
        public void onResponse(Channel channel);
        public void onError(Exception error);
    }

    public static Channel getChannel(String channelType) {
        Channel c = sChannels.get(channelType);
        if(c == null) {
            c = ADNSharedPreferences.getPrivateChannel(channelType);
            sChannels.put(channelType, c);
        }
        return c;
    }

    public static void retrieveChannel(AppDotNetClient client, final String channelType, final PrivateChannelHandler handler) {
        QueryParameters params = new QueryParameters();
        params.put("channel_types", channelType);
        client.retrieveCurrentUserSubscribedChannels(params, new ChannelListResponseHandler() {
            @Override
            public void onSuccess(ChannelList responseData) {
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

    public static void createChannel(final AppDotNetClient client, final String channelType, final PrivateChannelHandler handler) {
        User currentUser = ADNSharedPreferences.getToken().getUser();

        Channel c = new Channel();
        c.setType(channelType);

        Channel.Acl writer = new Channel.Acl(true);
        writer.setUserIds(new String[] { currentUser.getId() });
        writer.setAnyUser(false);

        Channel.Acl reader = new Channel.Acl(false);

        c.setWriters(writer);
        c.setReaders(reader);

        client.createChannel(c, new ChannelResponseHandler() {
            @Override
            public void onSuccess(final Channel responseData) {
                client.subscribeChannel(responseData, new ChannelResponseHandler() {
                    @Override
                    public void onSuccess(Channel responseData) {
                        ADNSharedPreferences.savePrivateChannel(responseData);
                        sChannels.put(channelType, responseData);
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
}
