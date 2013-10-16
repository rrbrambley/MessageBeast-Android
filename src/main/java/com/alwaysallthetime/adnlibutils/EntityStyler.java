package com.alwaysallthetime.adnlibutils;

import android.text.SpannableString;
import android.text.style.CharacterStyle;

import com.alwaysallthetime.adnlib.data.AbstractPost;
import com.alwaysallthetime.adnlib.data.Entities;

import java.util.List;

public class EntityStyler {

    public static SpannableString getStyledText(AbstractPost post, List<CharacterStyle> mentionStyles, List<CharacterStyle> hashtagStyles, List<CharacterStyle> linkStyles) {
        SpannableString string = new SpannableString(post.getText());
        Entities entities = post.getEntities();

        if(mentionStyles != null) {
            applyStylesToEntities(string, entities.getMentions(), mentionStyles);
        }
        if(hashtagStyles != null) {
            applyStylesToEntities(string, entities.getHashtags(), hashtagStyles);
        }
        if(linkStyles != null) {
            applyStylesToEntities(string, entities.getLinks(), linkStyles);
        }

        return string;
    }

    private static void applyStylesToEntities(SpannableString spannableString, List<? extends Entities.Entity> entities, List<CharacterStyle> styles) {
        for(Entities.Entity e : entities) {
            for(CharacterStyle s : styles) {
                spannableString.setSpan(s, e.getPos(), e.getPos() + e.getLen(), 0);
            }
        }
    }
}
