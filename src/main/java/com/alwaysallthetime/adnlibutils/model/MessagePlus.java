package com.alwaysallthetime.adnlibutils.model;

import android.util.Log;

import com.alwaysallthetime.adnlib.data.Annotatable;
import com.alwaysallthetime.adnlib.data.Annotation;
import com.alwaysallthetime.adnlib.data.File;
import com.alwaysallthetime.adnlib.data.Message;
import com.alwaysallthetime.adnlibutils.AnnotationFactory;
import com.alwaysallthetime.adnlibutils.AnnotationUtility;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MessagePlus {

    private static final String TAG = "ADNLibUtils_MessagePlus";

    private Message mMessage;
    private Date mDisplayDate;
    private DisplayLocation mDisplayLocation;
    private boolean mHasSetDisplayLocation;
    private boolean mHasSetOEmbedValues;
    private List<PhotoOEmbed> mPhotoOEmbeds;
    private List<Html5VideoOEmbed> mHtml5VideoOEmbeds;

    private Set<String> mPendingOEmbeds;
    private boolean mIsUnsent;
    private int mSendAttempts;

    private static MessagePlus newUnsentMessagePlus(String channelId, String messageId, Message message) {
        Date date = new Date();
        setMessageIdWithReflection(messageId, message);
        setChannelIdWithReflection(channelId, message);

//        message.setEntities(EntityGenerator.getEntities(message.getText()));
        message.addAnnotation(AnnotationUtility.newDisplayDateAnnotation(date));

        MessagePlus messagePlus = new MessagePlus(message);
        messagePlus.setIsUnsent(true);
        messagePlus.setDisplayDate(date);

        return messagePlus;
    }

    public boolean replacePendingOEmbedWithOEmbedAnnotation(String pendingFileId, File file) {
        boolean removed = mPendingOEmbeds.remove(pendingFileId);
        if(removed) {
            mMessage.addAnnotation(AnnotationFactory.getOEmbedAnnotation(file));
        }
        return removed;
    }

    public void setPendingOEmbeds(Set<String> pendingOEmbeds) {
        mPendingOEmbeds = pendingOEmbeds;
    }

    public MessagePlus(Message message) {
        mMessage = message;
    }

    public Date getDisplayDate() {
        return mDisplayDate != null ? mDisplayDate : mMessage.getCreatedAt();
    }

    public void setDisplayDate(Date displayDate) {
        mDisplayDate = displayDate;
    }

    public boolean hasSetDisplayLocation() {
        return mHasSetDisplayLocation;
    }

    public boolean hasDisplayLocation() {
        return mDisplayLocation != null;
    }

    public DisplayLocation getDisplayLocation() {
        return mDisplayLocation;
    }

    public void setDisplayLocation(DisplayLocation location) {
        mDisplayLocation = location;
        mHasSetDisplayLocation = true;
    }

    public boolean hasSetOEmbedValues() {
        return mHasSetOEmbedValues;
    }

    public boolean hasPhotoOEmbed() {
        return mPhotoOEmbeds != null && mPhotoOEmbeds.size() > 0;
    }

    public List<PhotoOEmbed> getPhotoOEmbeds() {
        return mPhotoOEmbeds;
    }

    public boolean hasHtml5VideoOEmbed() {
        return mHtml5VideoOEmbeds != null && mHtml5VideoOEmbeds.size() > 0;
    }

    public List<Html5VideoOEmbed> getHtml5VideoOEmbeds() {
        return mHtml5VideoOEmbeds;
    }

    public Message getMessage() {
        return mMessage;
    }

    public void setIsUnsent(boolean isUnsent) {
        mIsUnsent = isUnsent;
    }

    public boolean isUnsent() {
        return mIsUnsent;
    }

    public int getNumSendAttempts() {
        return mSendAttempts;
    }

    public void setNumSendAttempts(int numAttempts) {
        mSendAttempts = numAttempts;
    }

    public int incrementSendAttempts() {
        mSendAttempts++;
        return mSendAttempts;
    }

    public boolean hasPendingOEmbeds() {
        return mPendingOEmbeds != null && mPendingOEmbeds.size() > 0;
    }

    public Set<String> getPendingOEmbeds() {
        return mPendingOEmbeds;
    }

    public void addOEmbedsFromAnnotations(List<Annotation> annotations) {
        mHasSetOEmbedValues = true;
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
                Log.d(TAG, "Annotation type " + type + " is unsupported by MessagePlus");
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

    public static class UnsentMessagePlusBuilder {
        private String channelId;
        private String messageId;
        private Message message;
        private LinkedHashSet<String> pendingOEmbeds;

        public static UnsentMessagePlusBuilder newBuilder(String channelId, String messageId, Message message) {
            UnsentMessagePlusBuilder builder = new UnsentMessagePlusBuilder();
            builder.channelId = channelId;
            builder.messageId = messageId;
            builder.message = message;
            return builder;
        }

        public UnsentMessagePlusBuilder addPendingOEmbed(String pendingFileId) {
            if(pendingOEmbeds == null) {
                pendingOEmbeds = new LinkedHashSet<String>();
            }
            pendingOEmbeds.add(pendingFileId);
            return this;
        }

        public MessagePlus build() {
            MessagePlus messagePlus = newUnsentMessagePlus(channelId, messageId, message);
            messagePlus.setPendingOEmbeds(pendingOEmbeds);
            return messagePlus;
        }
    }
}
