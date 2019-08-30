package com.songoda.core.settingsv2;

import com.songoda.core.settingsv2.ConfigFormattingRules.CommentStyle;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A comment for a configuration key
 *
 * @since 2019-08-28
 * @author jascotty2
 */
public class Comment {

    final List<String> lines = new ArrayList();
    CommentStyle commentStyle = null;

    public Comment() {
    }

    public Comment(String... lines) {
        this.lines.addAll(Arrays.asList(lines));
    }

    public Comment(List<String> lines) {
        this.lines.addAll(lines);
    }

    public Comment(CommentStyle commentStyle, String... lines) {
        this.commentStyle = commentStyle;
        this.lines.addAll(Arrays.asList(lines));
    }

    public Comment(CommentStyle commentStyle, List<String> lines) {
        this.commentStyle = commentStyle;
        this.lines.addAll(lines);
    }

    public CommentStyle getCommentStyle() {
        return commentStyle;
    }

    public void setCommentStyle(CommentStyle commentStyle) {
        this.commentStyle = commentStyle;
    }

    public List<String> getLines() {
        return lines;
    }

    public void writeComment(Writer output, int offset, CommentStyle defaultStyle) throws IOException {
        CommentStyle style = commentStyle != null ? commentStyle : defaultStyle;
        int minSpacing = 0, borderSpacing = 0;
        // first draw the top of the comment
        if (style.drawBorder) {
            // grab the longest line in the list of lines
            minSpacing = lines.stream().max((s1, s2) -> s1.length() - s2.length()).get().length();
            borderSpacing = minSpacing + style.commentPrefix.length() + style.commentSuffix.length();
            // draw the first line
            output.write((new String(new char[offset])).replace('\0', ' ') + (new String(new char[borderSpacing + 2])).replace('\0', '#') + "\n");
            if (style.drawSpace) {
                output.write((new String(new char[offset])).replace('\0', ' ')
                        + "#" + style.spacePrefixTop
                        + (new String(new char[borderSpacing - style.spacePrefixTop.length() - style.spaceSuffixTop.length()])).replace('\0', style.spaceCharTop)
                        + style.spaceSuffixTop + "#\n");
            }
        } else if (style.drawSpace) {
            output.write((new String(new char[offset])).replace('\0', ' ') + "#\n");
        }
        // then the actual comment lines
        for (String line : lines) {
            // todo? should we auto-wrap comment lines that are longer than 80 characters?
            output.write((new String(new char[offset])).replace('\0', ' ') + "#" + style.commentPrefix
                    + (minSpacing == 0 ? line : line + (new String(new char[minSpacing - line.length()])).replace('\0', ' ')) + style.commentSuffix + (style.drawBorder ? "#\n" : "\n"));
        }
        // now draw the bottom of the comment border
        if (style.drawBorder) {
            if (style.drawSpace) {
                output.write((new String(new char[offset])).replace('\0', ' ')
                        + "#" + style.spacePrefixBottom
                        + (new String(new char[borderSpacing - style.spacePrefixBottom.length() - style.spaceSuffixBottom.length()])).replace('\0', style.spaceCharBottom)
                        + style.spaceSuffixBottom + "#\n");
            }
            output.write((new String(new char[offset])).replace('\0', ' ') + (new String(new char[borderSpacing + 2])).replace('\0', '#') + "\n");
        } else if (style.drawSpace) {
            output.write((new String(new char[offset])).replace('\0', ' ') + "#\n");
        }
    }
}
