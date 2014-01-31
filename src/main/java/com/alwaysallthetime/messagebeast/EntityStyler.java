package com.alwaysallthetime.messagebeast;

import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;

import com.alwaysallthetime.adnlib.data.AbstractPost;
import com.alwaysallthetime.adnlib.data.Entities;

import java.util.List;

/**
 * A utility for applying Character styles to entities.
 */
public class EntityStyler {

    /**
     * Get styled hashtags. If none exist, then the returned CharSequence has no styled spans.
     *
     * @param post the post or Message whose hashtags should be styled
     * @param hashtagStyles the CharacterStyles to apply to the hashtags
     * @return A CharSequence with styled spans
     */
    public static CharSequence getStyledHashtags(AbstractPost post, List<CharacterStyle> hashtagStyles) {
        SpannableStringBuilder string = new SpannableStringBuilder(post.getText());

        Entities entities = post.getEntities();
        if(entities != null) {
            applyStylesToEntities(string, entities.getHashtags(), hashtagStyles);
        }
        return string;
    }

    /**
     * Get styled links. If none exist, then the returned CharSequence has no styled spans.
     *
     * @param post the post or Message whose links should be styled
     * @param linkStyles the CharacterStyles to apply to the links
     * @return A CharSequence with styled spans
     */
    public static CharSequence getStyledLinks(AbstractPost post, List<CharacterStyle> linkStyles) {
        SpannableStringBuilder string = new SpannableStringBuilder(post.getText());

        Entities entities = post.getEntities();
        if(entities != null) {
            applyStylesToEntities(string, entities.getLinks(), linkStyles);
        }
        return string;
    }

    /**
     * Get styled mentions. If none exist, then the returned CharSequence has no styled spans.
     *
     * @param post the post or Message whose mentions should be styled
     * @param mentionStyles the CharacterStyles to apply to the mentions
     * @return A CharSequence with styled spans
     */
    public static CharSequence getStyledMentions(AbstractPost post, List<CharacterStyle> mentionStyles) {
        SpannableStringBuilder string = new SpannableStringBuilder(post.getText());

        Entities entities = post.getEntities();
        if(entities != null) {
            applyStylesToEntities(string, entities.getMentions(), mentionStyles);
        }
        return string;
    }

    /**
     * Get styled entities. If none exist, then the returned CharSequence has no styled spans.
     *
     * @param post the post or Message whose entities should be styled
     * @param mentionStyles the CharacterStyles to apply to the mentions. may be null.
     * @param hashtagStyles the CharacterStyles to apply to the hashtags. may be null.
     * @param linkStyles the CharacterStyles to apply to the links. may be null.
     * @return A CharSequence with styled spans
     */
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
