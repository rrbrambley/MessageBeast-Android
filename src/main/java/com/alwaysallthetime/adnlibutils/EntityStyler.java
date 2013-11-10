package com.alwaysallthetime.adnlibutils;

import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;

import com.alwaysallthetime.adnlib.data.AbstractPost;
import com.alwaysallthetime.adnlib.data.Entities;

import java.util.List;

public class EntityStyler {

    public static CharSequence getStyledText(AbstractPost post, List<CharacterStyle> mentionStyles, List<CharacterStyle> hashtagStyles, List<CharacterStyle> linkStyles) {
        SpannableStringBuilder string = new SpannableStringBuilder(post.getText());

        Entities entities = post.getEntities();
        if(entities != null) {
            if(mentionStyles != null) {
                applyStylesToEntities(string, entities.getMentions(), mentionStyles);
            }
            if(hashtagStyles != null) {
                applyStylesToEntities(string, entities.getHashtags(), hashtagStyles);
            }
            if(linkStyles != null) {
                applyStylesToEntities(string, entities.getLinks(), linkStyles);
            }
        }

        return string;
    }

    private static void applyStylesToEntities(SpannableStringBuilder spannableString, List<? extends Entities.Entity> entities, List<CharacterStyle> styles) {
        for(Entities.Entity e : entities) {
            for(CharacterStyle s : styles) {
                spannableString.setSpan(CharacterStyle.wrap(s), e.getPos(), e.getPos() + e.getLen(), 0);
            }
        }
    }
}
