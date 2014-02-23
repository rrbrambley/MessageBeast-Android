package com.alwaysallthetime.messagebeast.model;

import android.util.Log;

import com.alwaysallthetime.adnlib.Annotations;
import com.alwaysallthetime.adnlib.data.Annotatable;
import com.alwaysallthetime.adnlib.data.Annotation;
import com.alwaysallthetime.adnlib.data.File;
import com.alwaysallthetime.adnlib.data.Message;
import com.alwaysallthetime.messagebeast.AnnotationFactory;
import com.alwaysallthetime.messagebeast.AnnotationUtility;
import com.alwaysallthetime.messagebeast.EntityGenerator;
import com.alwaysallthetime.messagebeast.PrivateChannelUtility;
import com.alwaysallthetime.messagebeast.db.PendingFileAttachment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A MessagePlus is a Message with extra metadata attached to it. These are usually constructed
 * by the MessageManager.
 *
 * @see com.alwaysallthetime.messagebeast.manager.MessageManager
 */
public class MessagePlus {

    private static final String TAG = "MessageBeast_MessagePlus";

    private Message mMessage;
    private Date mDisplayDate;
    private DisplayLocation mDisplayLocation;
    private List<PhotoOEmbed> mPhotoOEmbeds;
    private List<Html5VideoOEmbed> mHtml5VideoOEmbeds;

    private HashMap<String, PendingFileAttachment> mPendingFileAttachments;
    private boolean mIsUnsent;
    private int mSendAttempts;

    /**
     * After a pending file has been uploaded, take the returned file and add the OEmbed
     * or file attachments annotation to the Message. If an attachments annotation already exist,
     * this file is appended to the end of the existing file list.
     *
     * @param pendingFileId the pending file id that the provided File is meant to replace.
     * @param file The File returned from the App.net server upon successful upload of the File.
     * @return true if a pending file with the provided id exists, false if it does not and no
     * annotation will be added.
     */
    public boolean replacePendingFileAttachmentWithAnnotation(String pendingFileId, File file) {
        PendingFileAttachment attachment = mPendingFileAttachments.remove(pendingFileId);
        if(attachment != null) {
            if(attachment.isOEmbed()) {
                mMessage.addAnnotation(AnnotationFactory.getOEmbedAnnotation(file));
            } else {
                //if there's an existing file list, append to it.
                List<Annotation> annotations = mMessage.getAnnotations();
                if(annotations != null && annotations.size() > 0) {
                    int index = -1;
                    for(int i = 0; i < annotations.size(); i++) {
                        if(annotations.get(i).getType().equals(Annotations.ATTACHMENTS)) {
                            index = i;
                            break;
                        }
                    }
                    if(index != -1) {
                        Annotation attachmentsAnnotation = annotations.get(index);
                        AnnotationUtility.appendFileToAttachmentsFileList(attachmentsAnnotation, file);
                    } else {
                        mMessage.addAnnotation(AnnotationFactory.getAttachmentsAnnotation(file));
                    }
                } else {
                    mMessage.addAnnotation(AnnotationFactory.getAttachmentsAnnotation(file));
                }
            }
        }
        return attachment != null;
    }

    public void replaceTargetMessageAnnotationMessageId(String newMessageId) {
        Annotation targetMessageAnnotation = mMessage.getFirstAnnotationOfType(PrivateChannelUtility.MESSAGE_ANNOTATION_TARGET_MESSAGE);
        if(targetMessageAnnotation != null) {
            targetMessageAnnotation.getValue().put(PrivateChannelUtility.TARGET_MESSAGE_KEY_ID, newMessageId);
        }
    }

    /**
     * Set the List of PendingFileAttachments for this MessagePlus.
     *
     * @param pendingFileAttachments
     */
    public void setPendingFileAttachments(List<PendingFileAttachment> pendingFileAttachments) {
        if(pendingFileAttachments != null) {
            mPendingFileAttachments = new HashMap<String, PendingFileAttachment>(pendingFileAttachments.size());
            for(PendingFileAttachment attachment : pendingFileAttachments) {
                mPendingFileAttachments.put(attachment.getPendingFileId(), attachment);
            }
        } else {
            mPendingFileAttachments = null;
        }
    }

    /**
     * Construct a new MessagePlus.
     *
     * @param message the Message
     */
    public MessagePlus(Message message) {
        mMessage = message;
        addOEmbedsFromAnnotations(message.getAnnotationsOfType(Annotations.OEMBED));
    }

    /**
     * Get the display date associated with this MessagePlus. If no display date has been
     * set, then the Message's getCreatedAt() value is returned.
     *
     * @return the display date associated with this MessagePlus, or the value of Message.getCreatedAt()
     * if no display date has been explicitly set.
     */
    public Date getDisplayDate() {
        return mDisplayDate != null ? mDisplayDate : mMessage.getCreatedAt();
    }

    /**
     * Set a display date on this MessagePlus. This is useful when the Message's getCreatedAt()
     * value is different than the date that should be associated with this MessagePlus in the
     * UI.
     *
     * @param displayDate a date that should be associated with this MessagePlus in the UI
     */
    public void setDisplayDate(Date displayDate) {
        mDisplayDate = displayDate;
    }

    /**
     * @return true if this MessagePlus has a DisplayLocation associated with it, false otherwise.
     */
    public boolean hasDisplayLocation() {
        return mDisplayLocation != null;
    }

    /**
     * Get the DisplayLocation associated with this MessagePlus, or null if none exists.
     *
     * @return the DisplayLocation associated with this MessagePlus, or null if none exists.
     */
    public DisplayLocation getDisplayLocation() {
        return mDisplayLocation;
    }

    /**
     * Set the DisplayLocation to be associated with this MessagePlus.
     *
     * @param location the DisplayLocation to be associated with this MessagePlus.
     */
    public void setDisplayLocation(DisplayLocation location) {
        mDisplayLocation = location;
    }

    /**
     * @return true if the associated Message has one or more photo OEmbeds, false otherwise.
     */
    public boolean hasPhotoOEmbed() {
        return mPhotoOEmbeds != null && mPhotoOEmbeds.size() > 0;
    }

    /**
     * Get the List of PhotoOEmbeds as obtained by examining the annotations of the Message.
     *
     * @return the List of PhotoOEmbeds
     */
    public List<PhotoOEmbed> getPhotoOEmbeds() {
        return mPhotoOEmbeds;
    }

    /**
     * @return true if the associated Message has one or more html5video OEmbeds, false otherwise.
     */
    public boolean hasHtml5VideoOEmbed() {
        return mHtml5VideoOEmbeds != null && mHtml5VideoOEmbeds.size() > 0;
    }

    /**
     * Get the List of Html5VideoOEmbeds as obtained by examining the annotations of the Message.
     *
     * @return the List of Html5VideoOEmbeds
     */
    public List<Html5VideoOEmbed> getHtml5VideoOEmbeds() {
        return mHtml5VideoOEmbeds;
    }

    /**
     * Get the backing Message for this MessagePlus.
     *
     * @return the backing Message for this MessagePlus.
     */
    public Message getMessage() {
        return mMessage;
    }

    /**
     * Set the unsent flag on this MessagPlus, indicating whether the backing Message has yet to be
     * sent to the server
     *
     * @param isUnsent true if the backing Message has not been sent to the server, false otherwise.
     */
    public void setIsUnsent(boolean isUnsent) {
        mIsUnsent = isUnsent;
    }

    /**
     * @return true if the backing Message has not been sent to the server, false otherwise.
     */
    public boolean isUnsent() {
        return mIsUnsent;
    }

    /**
     * Get the number of times that we have already attempted and failed to send the backing
     * Message to the server.
     *
     * @return the number of times that we have already attempted and failed to send the backing
     * Message to the server.
     */
    public int getNumSendAttempts() {
        return mSendAttempts;
    }

    /**
     * Set the number of times that we have already attempted and failed to send the backing
     * Message to the server.
     */
    public void setNumSendAttempts(int numAttempts) {
        mSendAttempts = numAttempts;
    }

    /**
     * Increment the number of times that we have already attempted and failed to send the backing
     * Message to the server.
     * @return the number of send attempts (post-increment)
     */
    public int incrementSendAttempts() {
        mSendAttempts++;
        return mSendAttempts;
    }

    /**
     * @return true if this represents and unsent Message that is dependent on one or more Files
     * being uploaded prior to being sent to the server, false otherwise.
     */
    public boolean hasPendingFileAttachments() {
        return mPendingFileAttachments != null && mPendingFileAttachments.size() > 0;
    }

    /**
     * Get a Map whose keys are PendingFile ids mapped to PendingFileAttachment objects.
     * This may be null.
     *
     * @return the Map of PendingFileAttachments.
     */
    public Map<String, PendingFileAttachment> getPendingFileAttachments() {
        return mPendingFileAttachments;
    }

    private void addOEmbedsFromAnnotations(List<Annotation> annotations) {
        for(Annotation a : annotations) {
            HashMap<String, Object> value = a.getValue();
            String type = (String) value.get("type");

            if(AnnotationUtility.OEMBED_TYPE_PHOTO.equals(type)) {
                if(mPhotoOEmbeds == null) {
                    mPhotoOEmbeds = new ArrayList<PhotoOEmbed>();
                }
                mPhotoOEmbeds.add(new PhotoOEmbed(value));
            } else if(AnnotationUtility.OEMBED_TYPE_HTML5VIDEO.equals(type)) {
                if(mHtml5VideoOEmbeds == null) {
                    mHtml5VideoOEmbeds = new ArrayList<Html5VideoOEmbed>();
                }
                mHtml5VideoOEmbeds.add(new Html5VideoOEmbed(value));
            } else {
                Log.e(TAG, "Annotation type " + type + " is unsupported by MessagePlus");
            }
        }
    }

    private static void setChannelIdWithReflection(String channelId, Message message) {
        Class<?> messageClass = Message.class;
        try {
            Field channelIdField = messageClass.getDeclaredField("channelId");
            channelIdField.setAccessible(true);
            channelIdField.set(message, channelId);
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private static void setMessageIdWithReflection(String messageId, Message message) {
        Class<?> annotatableClass = Annotatable.class;
        try {
            Field messageIdField = annotatableClass.getDeclaredField("id");
            messageIdField.setAccessible(true);
            messageIdField.set(message, messageId);
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public abstract static class OEmbed {
        String type;
        int width;
        int height;
        String embeddableUrl;
        String title;

        String authorName;
        String authorUrl;

        String providerName;
        String providerUrl;
        int cacheAge;

        String thumbnailUrl;
        int thumbnailWidth;
        int thumbnailHeight;
        Date thumbnailUrlExpires;

        String thumbnailLargeUrl;
        int thumbnailLargeWidth;
        int thumbnailLargeHeight;
        Date thumbnailLargeUrlExpires;

        String fileTokenRead;
        String fileId;

        String version;

        public OEmbed(Map<String, Object> annotationValue) {
            this.type = (String) annotationValue.get("type");
            this.width = ((Double) annotationValue.get("width")).intValue();
            this.height = ((Double) annotationValue.get("height")).intValue();
            this.embeddableUrl = (String) annotationValue.get("embeddable_url");
            this.title = (String) annotationValue.get("title");
            this.authorName = (String) annotationValue.get("author_name");
            this.authorUrl = (String) annotationValue.get("author_url");
            this.providerName = (String) annotationValue.get("provider_name");
            this.providerUrl = (String) annotationValue.get("provider_url");
            this.fileId = (String) annotationValue.get("file_id");
            this.fileTokenRead = (String) annotationValue.get("file_token_read");

            this.thumbnailUrl = (String) annotationValue.get("thumbnail_url");
            if(this.thumbnailUrl != null) {
                this.thumbnailWidth = ((Double) annotationValue.get("thumbnail_width")).intValue();
                this.thumbnailHeight = ((Double) annotationValue.get("thumbnail_height")).intValue();
            }

            this.thumbnailLargeUrl = (String) annotationValue.get("thumbnail_large_url");
            if(this.thumbnailLargeUrl != null) {
                this.thumbnailLargeWidth = ((Double) annotationValue.get("thumbnail_large_width")).intValue();
                this.thumbnailLargeHeight = ((Double) annotationValue.get("thumbnail_large_height")).intValue();
            }

            Double cacheAgeDouble = (Double) annotationValue.get("cache_age");
            if(cacheAgeDouble != null) {
                this.cacheAge =  cacheAgeDouble.intValue();
            }

            String thumbnailUrlExpiresString = (String) annotationValue.get("thumbnail_url_expires");
            if(thumbnailUrlExpiresString != null) {
                this.thumbnailUrlExpires = AnnotationUtility.getDateFromIso8601String(thumbnailUrlExpiresString);
            }

            String thumbnailLargeUrlExpiresString = (String) annotationValue.get("thumbnail_large_url_expires");
            if(thumbnailLargeUrlExpiresString != null) {
                this.thumbnailLargeUrlExpires = AnnotationUtility.getDateFromIso8601String(thumbnailLargeUrlExpiresString);
            }
        }

        public String getType() {
            return type;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public String getEmbeddableUrl() {
            return embeddableUrl;
        }

        public String getTitle() {
            return title;
        }

        public String getAuthorName() {
            return authorName;
        }

        public String getAuthorUrl() {
            return authorUrl;
        }

        public String getProviderName() {
            return providerName;
        }

        public String getProviderUrl() {
            return providerUrl;
        }

        public int getCacheAge() {
            return cacheAge;
        }

        public String getThumbnailUrl() {
            return thumbnailUrl;
        }

        public int getThumbnailWidth() {
            return thumbnailWidth;
        }

        public int getThumbnailHeight() {
            return thumbnailHeight;
        }

        public Date getThumbnailUrlExpires() {
            return thumbnailUrlExpires;
        }

        public String getThumbnailLargeUrl() {
            return thumbnailLargeUrl;
        }

        public int getThumbnailLargeWidth() {
            return thumbnailLargeWidth;
        }

        public int getThumbnailLargeHeight() {
            return thumbnailLargeHeight;
        }

        public Date getThumbnailLargeUrlExpires() {
            return thumbnailLargeUrlExpires;
        }

        public String getFileTokenRead() {
            return fileTokenRead;
        }

        public String getFileId() {
            return fileId;
        }

        public String getVersion() {
            return version;
        }

        public boolean isThumbnailUrlExpired() {
            return thumbnailUrlExpires != null && thumbnailUrlExpires.before(new Date());
        }

        public boolean isThumbnailLargeUrlExpired() {
            return thumbnailLargeUrlExpires != null && thumbnailLargeUrlExpires.before(new Date());
        }
    }

    public static class PhotoOEmbed extends OEmbed {
        String url;
        Date urlExpires;

        public PhotoOEmbed(Map<String, Object> annotationValue) {
            super(annotationValue);

            this.url = (String) annotationValue.get("url");
            setUrlExpires((String) annotationValue.get("url_expires"));
        }

        public void setUrlExpires(String dateString) {
            if(dateString != null) {
                this.urlExpires = AnnotationUtility.getDateFromIso8601String(dateString);
            } else {
                this.urlExpires = null;
            }
        }

        public String getUrl() {
            return url;
        }

        public Date getUrlExpires() {
            return urlExpires;
        }
    }

    public static class Html5VideoOEmbed extends OEmbed {
        String posterUrl;
        ArrayList<OEmbedSource> sources;

        public Html5VideoOEmbed(Map<String, Object> annotationValue) {
            super(annotationValue);

            List<Map<String, Object>> sources = (List<Map<String, Object>>) annotationValue.get("sources");
            if(sources != null) {
                this.sources = new ArrayList<OEmbedSource>(sources.size());
                addSources(sources);
            } else {
                this.sources = new ArrayList<OEmbedSource>(0);
            }

            this.posterUrl = (String) annotationValue.get("post_url");
        }

        public void addSources(List<Map<String, Object>> sources) {
            for(Map<String, Object> source : sources) {
                this.sources.add(new OEmbedSource(source));
            }
        }

        public List<OEmbedSource> getSources() {
            return sources;
        }

        public OEmbedSource getFirstSource() {
            return sources.size() > 0 ? sources.get(0) : null;
        }

        public String getPosterUrl() {
            return posterUrl;
        }
    }

    public static class OEmbedSource {
        String url;
        String type;
        Date urlExpires;

        public OEmbedSource(Map<String, Object> sourceData) {
            this.url = (String) sourceData.get("url");
            this.type = (String) sourceData.get("type");

            String dateString = (String) sourceData.get("url_expires");
            if(dateString != null) {
                this.urlExpires = AnnotationUtility.getDateFromIso8601String(dateString);
            }
        }
    }

    /**
     * UnsentMessagePlusBuilder is used to construct unsent MessagePlus objects.
     */
    public static class UnsentMessagePlusBuilder {
        private String channelId;
        private String messageId;
        private Message message;
        private List<PendingFileAttachment> pendingFileAttachments;

        public static UnsentMessagePlusBuilder newBuilder(String channelId, String messageId, Message message) {
            UnsentMessagePlusBuilder builder = new UnsentMessagePlusBuilder();
            builder.channelId = channelId;
            builder.messageId = messageId;
            builder.message = message;
            return builder;
        }

        public UnsentMessagePlusBuilder addPendingFileAttachment(PendingFileAttachment attachment) {
            if(pendingFileAttachments == null) {
                pendingFileAttachments = new ArrayList<PendingFileAttachment>();
            }
            pendingFileAttachments.add(attachment);
            return this;
        }

        public MessagePlus build() {
            MessagePlus messagePlus = newUnsentMessagePlus(channelId, messageId, message);
            messagePlus.setPendingFileAttachments(pendingFileAttachments);
            return messagePlus;
        }
    }

    private static MessagePlus newUnsentMessagePlus(String channelId, String messageId, Message message) {
        Date date = new Date();
        setMessageIdWithReflection(messageId, message);
        setChannelIdWithReflection(channelId, message);

        message.setEntities(EntityGenerator.getEntities(message.getText()));
        
        if(message.getFirstAnnotationOfType(Annotations.OHAI_DISPLAY_DATE) == null) {
            message.addAnnotation(AnnotationFactory.getDisplayDateAnnotation(date));
        }

        MessagePlus messagePlus = new MessagePlus(message);
        messagePlus.setIsUnsent(true);
        messagePlus.setDisplayDate(date);

        return messagePlus;
    }
}
