Message Beast
===========
![alt tag](https://raw.github.com/rrbrambley/MessageBeast-Android/master/Images/yeti-Message-Beast-with-Shadow-smallish.png)

*Note: Documentation still incomplete. Code is highly functional, but small implementation changes still to come. Sorry, no maven support yet.*

Message Beast is a robust app engine geared towards building single-user, non-social applications that rely on App.net [Messages](http://developers.app.net/docs/resources/message/) as a means of personal cloud storage. It is available for both Android and [Objective-C](https://github.com/rrbrambley/MessageBeast-ObjC). Some simple applications that could be built with Message Beast include:
* a to-do list, 
* a personal journal,
* an expense tracker,
* a time-tracking app (e.g. for contracters to track time to bill clients)

... and really any type of single-user utility app that would benefit from having data backed up to the cloud.

Typically, an application built with Message Beast might rely on one or more private App.net Channels as a means of storing Messages. In this context, try to think of a Message as a unit of data more akin to a row in a database table – not in the traditional way you hear the word "messages," i.e., like in a chat room.

Some key features of Message Beast are:

1. **Full Channel syncing**. Since Channels of data will be owned and accessed by a single user, there will typically be a relatively small amount of Messages (maybe between a few dozen and a few hundred). All Messages in a Channel can thus be synced and persisted to a sqlite database on your device, upon first launch. For new users of your application, this would more or less be a no-op, but for users switching between devices or platforms, you can ensure their data is easily synced and accessible.
2. **Offline Message support**. Messages can be created and used when offline. If no internet connection is available, then the unsent Messages can live happily alongside the sent Messages in the user interface. When an internet connection is eventually available, then the unsent Messages will be sent to the proper App.net Channel. No more progress spinners or long waits after hitting the "post" button in an app. This works for all types of Messages, including those with OEmbeds and file attachments.
3. **Mutable actions can be performed on Messages**. App.net supports the use of [Annotations](developers.app.net/docs/meta/annotations/) on Messages, but unfortunately they are not mutable. Message Beast introduces the concept of **Action Messages**, which work around this limitation. For example, in a journaling application, you might want to be able to mark a journal entry as a "favorite." And later, you might want to say it is no longer a "favorite." This can be achieved with Action Messages in Message Beast.
4. **Full text search**. All Messages stored in the sqlite database are candidates for full-text search. This means you can build features that let users easily find old Messages in an instant.
5. **Loads of other data lookups**. Other than full-text search, you can lookup messages by location, hashtag, date, or by occurrence of any Annotation that you wish.

Core Architecture
---------
Depending on your needs, you will then want to interface with one or more of the following:

* **MessageManager**: This class provides the main Message lifecycle functionality, including retrieving, deleting, and creating new Messages. It wraps ADNLib's base functionality to perform these tasks, and seamlessly persists Messages and Message metadata as new Messages are encountered/created. It also provides the functionality associated with creating offline Messages and sending them at a later time. Furthermore, it interfaces with the SQLite database to provide simple methods for doing things like performing full-text searches, and obtaining instances of Messages in which specific hashtags, locations, other types of Annotations were used.
* **ActionMessageManager**: This class wraps the MessageManager to support performing mutable actions via what Message Beast calls *Action Channels*. An Action Channel is a channel of type ``com.alwaysallthetime.action`` in which all Messages are [machine-only Messages](http://developers.app.net/docs/resources/message/#machine-only-messages), each with an Annotation that points to a *target* Message in your "main" Channel. An *Action Message* thus serves as a flag, indicating that the user performed a specific action on a Message (e.g. marked an entry as a favorite). The deletion of an Action Message corresponds to the undoing of the action on a Message. The ActionMessageManager is used to create Action Messages with the simple methods ``applyChannelAtion()`` and ``removeChannelAction()``.
* **ChannelSyncManager**: The ChannelSyncManager was created to compensate for the fact that you may end up using several Channels for any given application while working with this library (especially when working with Action Channels). To avoid having to make many method calls to retrieve the newest Messages in all these Channels simultaneously, you can use the ChannelSyncManager and make a single method call to achieve this.

<p align="center">
  <img src="https://raw.github.com/rrbrambley/MessageBeast-Android/master/Images/ArchitectureDependency.png"/>
</p>

<h3>MessagePlus</h3>
When working with these manager classes, you will most often be using **MessagePlus** objects. MessagePlus is a wrapper around ADNLib's Message class that adds extra functionality – including stuff for display locations, display dates, OEmbed getters, and features required to support unsent Messages. You will generally never need to construct MessagePlus objects directly, as they will be given to you via the managers.

Example Code
------------
Begin by modifying your AndroidManifest.xml's ``application`` tag to use ADNApplication:

```xml
<application
  android:name="com.alwaysallthetime.messagebeast.ADNApplication"
  ...>
```

The point of this is simply to allow a convenient way for Message Beast features to get an Application Context via ``ADNApplication.getContext()`` globally. If you don't want to use ADNApplication as your Application type, simply call ``ADNApplication.setApplicationContext(getApplicationContext());`` in your main Activity on startup so that the Context is set.

<h3>ChannelSyncManager</h3>
The easiest way to work with one or more Channels is to rely on a ChannelSyncManager. This will do all the heavy lifting  associated with creating and initializing your private Channels, as well as performing full syncs on these Channels. Here's an example in which we will work with an [Ohai Journal Channel](https://github.com/appdotnet/object-metadata/blob/master/channel-types/net.app.ohai.journal.md):

```java

//set up the query parameters to be used when making requests for my channel.
private QueryParameters queryParameters = 
            new QueryParameters(GeneralParameter.INCLUDE_MESSAGE_ANNOTATIONS,
            GeneralParameter.INCLUDE_MACHINE, GeneralParameter.EXCLUDE_DELETED);

//create a ChannelSpec for an Ohai Journal Channel. 
ChannelSpec channelSpec = new ChannelSpec("net.app.ohai.journal", queryParameters);

//construct an AppDotNetClient
AppDotNetClient client = AppDotNetClient(ADNApplication.getContext(), myClientId, myPasswordGrantSecret);

//you can configure this all you want; read the docs for this.
MessageManager.MessageManagerConfiguration config = new MessageManager.MessageManagerConfiguration();

ChannelSyncManager channelSyncManager = new ChannelSyncManager(client, config, new ChannelSpecSet(channelSpec));
channelSyncManager.initChannels(new ChannelSyncManager.ChannelsInitializedHandler() {
    @Override
    public void onChannelsInitialized() {
        //we're now ready to call channelSyncManager.retrieveNewestMessages() whenevs.
    }

    @Override
    public void onException() {
        //Log.e("app", "something went wrong");
    }
});
```

<h3>MessageManager</h3>
The above code creates a new MessageManager when the ChannelSyncManager is constructed. In more advanced use cases, you may wish to have a MessageManager available without the use of a ChannelSyncManager. Regardless, you will only need one instance of a MessageManager, so you may choose to create a singleton instance by doing something like this:

```java
public class MessageManagerInstance {
    
    private static MessageManager sMessageManager;
    
    public static MessageManager getInstance() {
        if(sMessageManager == null) {
            MessageManager.MessageManagerConfiguration config = new MessageManager.MessageManagerConfiguration();
            
            //location annotations will be examined and DisplayLocations will be assigned to Messages
            config.setLocationLookupEnabled(true);
              
            //a reference to all Messages with OEmbed Annotations will be stored in the sqlite database
            config.addAnnotationExtraction(Annotations.OEMBED);
              
            //instead of relying only on Message.getCreatedAt(), use the Ohai display date annotation
            config.setMessageDisplayDateAdapter(new MessageManager.MessageDisplayDateAdapter() {
                @Override
                public Date getDisplayDate(Message message) {
                    return AnnotationUtility.getOhaiDisplayDate(message);
                }
            });
              
            //Pass your instance of an AppDotNetClient. Voila. a MessageManager.
            sMessageManager = new MessageManager(myAppDotNetClient, config);
        }
        return sMessageManager;
    }
}
```

And then you could choose to use this singleton instance to construct a ChannelSyncManager as well, if you wanted.

<h3>ActionMessageManager</h3>
If you'd like to build an app that supports mutable actions on Messages in your Channel, you should use the ActionMessageManager. Let's suppose you're working on a to-do list app that allows users to mark entries as "high-priority." Here's an example of how you might use the above MessageManager singleton code to construct an ActionMessageManager that uses one Action Channel:

```java
ActionMessageManager myActionMessageManager = ActionMessageManager.getInstance(MessageManagerInstance.getInstance());
myActionMessageManager.initActionChannel("com.myapp.action.highpriority", myTodoChannel, new ActionMessageManager.ActionChannelInitializedHandler() {
    @Override
    public void onInitialized(Channel channel) {
        //now we're ready to apply actions to myTodoChannel
        //let's stash this newly initialized Action Channel to be used later...
        mHighPriorityChannel = channel;
    }
    
    @Override
    public void onException(Exception exception) {
        //whoops
        Log.e(TAG, exception.getMessage(), exception);
    }
});
```

And later on you could allow the user to perform the high priority action on a Message by doing something like:

```java
myActionMessageManager.applyChannelAction(mHighPriorityChannel.getId(), myMessage);
```

And remove the action with:

```java
myActionMessageManager.removeChannelAction(mHighPriorityChannel.getId(), myMessage.getId());
```

Here's an example of how you could more easily work with your main to-do list Channel and your high-priority Action Channel by using the ChannelSyncManager:

```java

//set up the query parameters to be used when making requests for my channel.
private QueryParameters queryParameters = 
    new QueryParameters(GeneralParameter.INCLUDE_MESSAGE_ANNOTATIONS,
    GeneralParameter.INCLUDE_MACHINE, GeneralParameter.EXCLUDE_DELETED);

ChannelSpec todoChannelSpec = new ChannelSpec("com.myapp.todolist", queryParameters);
TargetWithActionChannelsSpecSet spec = new TargetWithActionChannelsSpecSet(todoChannelSpec,
        "com.myapp.action.highpriority");
ActionMessageManager amm = ActionMessageManagerInstance.getInstance(MessageManagerInstance.getInstance());
ChannelSyncManager channelSyncManager = new ChannelSyncManager(amm, spec);
            
channelSyncManager.initChannels(new ChannelSyncManager.ChannelsInitializedHandler() {
    @Override
    public void onChannelsInitialized() {
        //we can now work with our Channels!
        
        mMyTodoChannel = mSyncManager.getTargetChannel();
        mMyHighPriorityActionChannel = mSyncManager.getActionChannel("com.myapp.action.highpriority");
    }

    @Override
    public void onException() {
        Log.e("app", "something went wrong");
    }
});
```

<h3>Loading Persisted Messages</h3>
The MessageManager's retrieve methods will always only retrieve Messages that it does not currently have persisted. If you want to load persisted Messages, e.g. on app launch, you should:

```java
//load up to 50 Messages in my channel.
TreeMap<Long, MessagePlus> messages = messageManager.loadPersistedMessages(myChannel.getId(), 50);
```

When you load persisted Messages, the Message's stay available in the MessageManager's internal Message map. This means that subsequent calls to loadPersistedMessages() will load *more* Messages (e.g. Mesasges 0-49 in first call above, then 50-99 in second call). If you don't need the Messages to be kept in memory, you should use one of the ``getMessages()`` methods.

<h3>Full Channel Sync</h3>
Having all a user's data available on their device (versus in the cloud) might be necessary to make your app function properly. If this is the case, you might want to use one of the following methods of syncing the user's data.

With ChannelSyncManager:
```java

//assume we have instantiated ChannelSyncManager as it was in the last example, above

channelSyncManager.initChannels(new ChannelSyncManager.ChannelsInitializedHandler() {
    @Override
    public void onChannelsInitialized() {
        //now that we've initialized our channels, we can perform a sync.
        
        channelSyncManager.checkFullSyncStatus(new ChannelSyncManager.ChannelSyncStatusHandler() {
            @Override
            public void onSyncStarted() {
                //show progress or something (e.g. "Retrieving your data...")
                //this will only be called if your channel hasn't already been synced once before
            }
            
            @Override
            public void onSyncComplete() {
                //this will be called instantly if the channel has already been synced once.
                //
                //otherwise, onSyncStarted() will be called first, and this will eventually be called
                //after all Messages have been downloaded and persisted.
            }
    }

    @Override
    public void onException() {
        Log.e("app", "something went wrong");
    }
});

```

Using the ChannelSyncManager to perform the full sync is especially convenient when you are syncing multiple Channels (e.g. three Action Channels along with your single target Channel) – all of the Channels will be synced with a single method call. Alternatively, if you can use the MessageManager's methods to directly check the sync state of your Channel and start the sync if necessary (this is what ChannelSyncManager does under the hood):

```java
FullSyncState state = messageManager.getFullSyncState(myChannel.getId());
if(state == FullSyncState.NOT_STARTED || state == FullSyncState.INCOMPLETE) {
    //maybe prompt the user before executing this
    messageManager.retrieveAndPersistAllMessages(myChannel.getId(), new MessageManager.MessageManagerMultiChannelSyncResponseHandler() {
        @Override
        public void onSuccess() {
            //done!
        }
        
        @Override
        public void onError(Exception exception) {
            Log.e(TAG, exception.getMessage(), exception);
            //sad face
        }
    });
} else {
  //we're already done, carry on by launching the app normally.
}
```
It's worth noting that ``retrieveAndPersistAllMessages()`` actually will sync multiple Channels at once, just like the ChannelSyncManager, but the main difference is that the ChannelSyncManager provides feedback via the ChannelSyncStatusHandler after it internally checks the sync state.

<h3>Message Creation and Lifecycle</h3>
The MessageManager provides a few different ways of creating Messages. The simplest way is:

```java
Message m = new Message("Check out my awesome Message!");
myMessageManager.createMessage(myChannel.getId(), m, new MessageManager.MessageManagerResponseHandler() {
    @Override
    public void onSuccess(List<MessagePlus> messages) {
        //messages includes our new Message, and any other Messages that may have not already been
        //synced prior to creating this new one.
    }

    @Override
    public void onError(Exception exception) {
        Log.e(TAG, exception.getMessage(), exception);
    }
});
```

This a thin wrapper around ADNLib's createMessage() method that performs database insertion and extraction of other Message data (just as would happen when calling the retrieve methods).

For applications that should enable users to create Messages regardless of having an internet connection, you can use a different method:

```java
Message m = new Message("Check out my awesome Message!");
myMessageManager.createUnsentMessageAndAttemptSend(myChannel.getId(), m);
```

The first obvious difference between this method of creating a Message and the previous is that you do not pass a response handler when creating an unsent Message. Instead, because the Message could be sent at a later time, you should set up a BroadcastReceiver somewhere in your application to be notified when your app successfully sends a Message.

```java
//somewhere in my Activity
registerReceiver(sentMessageReceiver, new IntentFilter(MessageManager.INTENT_ACTION_UNSENT_MESSAGES_SENT));

...

//elsewhere in my Activity
private final BroadcastReceiver sentMessageReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        String channelId = intent.getStringExtra(MessageManager.EXTRA_CHANNEL_ID);
        ArrayList<String> sentMessageIds = intent.getStringArrayListExtra(MessageManager.EXTRA_SENT_MESSAGE_IDS);
        
        //after the unsent messages are successfully sent, the local copies are deleted.
        //do this to retrieve the newly sent messages:
        myMessageManager.retrieveNewestMessages(channelId, new MessageManager.MessageManagerResponseHandler() {
            @Override
            public void onSuccess(List<MessagePlus> responseData) {
                //we got our new messages.
            }
    
            @Override
            public void onError(Exception exception) {
                Log.e(TAG, exception.getMessage(), exception);
            }
        });
    }
};
```

Additionally, you can register for the ``MessageManager.INTENT_ACTION_UNSENT_MESSAGE_SEND_FAILURE`` broadcast if you want to be notified of send failures. The Broadcast Intent will contain the following extras: ``MessageManager.EXTRA_CHANNEL_ID``, ``MessageManager.EXTRA_MESSAGE_ID``, and ``MessageManager.EXTRA_SEND_ATTEMPTS``. Every time the message is failed to be sent for any reason, the number of send attempts is incremented. Looking at this value is helpful if you want to eventually give up on sending a Message and delete it.

If your Message depends on the existence of [File](developers.app.net/docs/resources/file/) objects for [OEmbeds](https://github.com/appdotnet/object-metadata/blob/master/annotations/net.app.core.oembed.md) or [attachments](https://github.com/appdotnet/object-metadata/blob/master/annotations/net.app.core.attachments.md), you can also create unsent Messages with pending file uploads. Pending files are created by the ``FileManager`` class and passed to an alternate version of the createUnsentMessageAndAttemptSend() method:

```java
FileManager fileManager = FileManager.getInstance(myAppDotNetClient);

//create the pending file using a Uri pointing to the file location 
//and the standard object fields.
PendingFile pendingFile = fileManager.createPendingFile(photoUri, fileType, 
                          filename, mimeType, kind, false);

//create a new PendingFileAttachment, with 'true' for isOEmbed
//false would mean it goes in a file attachment list
ArrayList<PendingFileAttachment> attachments = new ArrayList<PendingFileAttachment>(1);
attachments.add(new PendingFileAttachment(pendingFile.getId(), true));

myMessageManager.createUnsentMessageAndAttemptSend(myChannel.getId(), message, attachments);
```

When Messages fail to send on their first attempt, you need to trigger another send attempt before any of MessageManager's retrieve methods can be executed. For example, ``retrieveNewestMessages()`` will return false if unsent Messages are blocking the newest Messages from being retrieved.

```java
//this will send both pending Message deletions and unsent Messages
myMessageManager.sendAllUnsent(myChannel.getId());
```

<h3>Message Search and Lookup</h3>
Full-text search is available for all Messages persisted by the MessageManager. 

```java
//find my Messages containing the string "pizza" in the text field.
OrderedMessageBatch results = messageManager.searchMessagesWithQuery(myChannel.getId(), "pizza")

//Message times (in millis) mapped to MessagePlus objects, in reverse chronological order
TreeMap<Long, MessagePlus> messages = results.getMessages();
```

You can also use the ``searchMessagesWithQuery(String channelId, String query, MessageFilter messageFilter)`` to filter out some of the results. 

Because location annotations are not part of the Message text, the human-readable name of all display locations are indexed separately. To search by human-readable location name, use:

```java
//find MessagePlus objects that have a DisplayLocation name matching "The Mission"
OrderedMessageBatch results = messageManager.searchMessagesWithDisplayLocationQuery(myChannel.getId(), 
                              "The Mission");
```

Other methods available for looking up Messages:

```java
//all messages in my channel that use the oembed Annotation
TreeMap<Long, MessagePlus> messages1 = messageManager.getMessagesWithAnnotation(myChannel.getId(),
                                            "net.app.core.oembed");

//all messages in my channel that have the hashtag "food"
LinkedHashMap<String, MessagePlus> messages2 = messageManager.getHashtagInstances(myChannel.getId(),
                                                   "food");

//all messages in my channel that have a DisplayLocation with the same name as that of myMessagePlus,
//and that lie within ~one hundred meters of that DisplayLocation (e.g. McDonald's in San Francisco is not
//the same McDonald's in Chicago).
TreeMap<Long, MessagePlus> messages3 = messageManager.getMessages(myChannel.getId(),
                                                    myMessagePlus.getDisplayLocation(),
                                                    ADNDatabase.LocationPrecision.ONE_HUNDRED_METERS);
```

<h3>Other Goodies</h3>
Use the ConfigurationUtility on every launch to update the Configuration as per the [App.net Configuration guidelines](http://developers.app.net/docs/resources/config/#how-to-use-the-configuration-object):

```java
//executed somewhere when app launches. This will update at most once per day.
ConfigurationUtility.updateConfiguration(myAppDotNetClient);

...

//elsewhere, when configuration is needed
Configuration configuration = ADNSharedPreferences.getConfiguration();

```

If you're building UI that would benefit from the styling of entities of any type, you can use ``EntityStyler`` to easily apply CharacterStyles to a Message:

```java
ArrayList<CharacterStyle> hashtagStyles = new ArrayList<CharacterStyle>(2);
hashtagStyles.add(new StyleSpan(Typeface.BOLD | Typeface.ITALIC));
hashtagStyles.add(new ForegroundColorSpan(getResources().getColor(R.color.my_hashtag_color)));

myTextView.setText(EntityStyler.getStyledHashtags(myMessage, hashtagStyles));
```

Future Improvements, Additions, Fixes
------
See [Issues](https://github.com/rrbrambley/MessageBeast-Android/issues).


License
-------
The MIT License (MIT)

Copyright (c) 2013 Rob Brambley

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
