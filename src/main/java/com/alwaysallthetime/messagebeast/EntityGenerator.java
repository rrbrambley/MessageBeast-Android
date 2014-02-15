package com.alwaysallthetime.messagebeast;

import android.util.Log;

import com.alwaysallthetime.adnlib.data.Entities;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityGenerator {
    private static final Pattern TAG_PATTERN = Pattern.compile("\\B#\\w*[a-zA-Z]+\\w*");//Pattern.compile("(?:^|\\s|[\\p{Punct}&&[^/]])(#[\\p{L}0-9-_]+)");

    private static final String TAG = "MessageBeast_EntityGenerator";

    public static Entities getEntities(String messageText) {
        if(messageText != null && messageText.length() > 0) {
            Entities entities = new Entities();
            entities.getHashtags().addAll(getHashtags(messageText));
            return entities;
        }
        return null;
    }

    //TODO links and mentions

    public static List<Entities.Hashtag> getHashtags(String messageText) {
        ArrayList<Entities.Hashtag> hashtags = new ArrayList<Entities.Hashtag>(3);

        Matcher matcher = TAG_PATTERN.matcher(messageText);
        while(matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String hashtagName = messageText.substring(start + 1, end);
            Log.d(TAG, "name = " + hashtagName);

            Entities.Hashtag hashtag = new Entities.Hashtag();

            Class<?> hashtagClass = Entities.Hashtag.class;
            try {
                Field name = hashtagClass.getDeclaredField("name");
                Field pos = hashtagClass.getDeclaredField("pos");
                Field len = hashtagClass.getDeclaredField("len");

                name.setAccessible(true);
                name.set(hashtag, hashtagName);

                pos.setAccessible(true);
                pos.set(hashtag, start);

                len.setAccessible(true);
                len.set(hashtag, end-start);

                hashtags.add(hashtag);
            } catch(Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        return hashtags;
    }
}
