package project.smartpermits;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

public class MarkdownFormatter {

    public static SpannableStringBuilder format(String raw) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        if (raw == null || raw.isEmpty()) return sb;

        String[] lines = raw.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            boolean isHeader = false;
            float headerScale = 1.0f;
            if (line.startsWith("### ")) {
                line = line.substring(4);
                isHeader = true;
                headerScale = 1.1f;
            } else if (line.startsWith("## ")) {
                line = line.substring(3);
                isHeader = true;
                headerScale = 1.2f;
            } else if (line.startsWith("# ")) {
                line = line.substring(2);
                isHeader = true;
                headerScale = 1.3f;
            }

            if (line.startsWith("- ") || line.startsWith("* ")) {
                line = "  • " + line.substring(2);
            }

            SpannableStringBuilder lineSb = new SpannableStringBuilder();
            int pos = 0;
            while (pos < line.length()) {
                int boldStart = line.indexOf("**", pos);
                if (boldStart == -1) {
                    lineSb.append(processItalic(line.substring(pos)));
                    break;
                }
                if (boldStart > pos) {
                    lineSb.append(processItalic(line.substring(pos, boldStart)));
                }
                int boldEnd = line.indexOf("**", boldStart + 2);
                if (boldEnd == -1) {
                    lineSb.append(processItalic(line.substring(boldStart)));
                    break;
                }
                String boldText = line.substring(boldStart + 2, boldEnd);
                int start = lineSb.length();
                lineSb.append(processItalic(boldText));
                lineSb.setSpan(new StyleSpan(Typeface.BOLD), start, lineSb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                pos = boldEnd + 2;
            }

            if (isHeader) {
                int start = sb.length();
                sb.append(lineSb);
                sb.setSpan(new StyleSpan(Typeface.BOLD), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new RelativeSizeSpan(headerScale), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                sb.append(lineSb);
            }

            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }
        return sb;
    }

    private static SpannableStringBuilder processItalic(String text) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        int pos = 0;
        while (pos < text.length()) {
            int italicStart = text.indexOf("*", pos);
            if (italicStart == -1) {
                sb.append(text.substring(pos));
                break;
            }
            if (italicStart > pos) {
                sb.append(text.substring(pos, italicStart));
            }
            int italicEnd = text.indexOf("*", italicStart + 1);
            if (italicEnd == -1) {
                sb.append(text.substring(italicStart));
                break;
            }
            String italicText = text.substring(italicStart + 1, italicEnd);
            int start = sb.length();
            sb.append(italicText);
            sb.setSpan(new StyleSpan(Typeface.ITALIC), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            pos = italicEnd + 1;
        }
        return sb;
    }
}
