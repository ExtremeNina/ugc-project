package com.example.onlyone.Utils;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
//用于防护xss攻击
public class HtmlSanitizer {

    private static final Safelist safelist = Safelist.relaxed()
            .addTags("section", "article", "main", "header", "footer")
            .addAttributes(":all", "style", "class", "id", "dir")
            .addProtocols("a", "href", "ftp", "http", "https", "mailto")
            .addEnforcedAttribute("a", "rel", "nofollow")
            .preserveRelativeLinks(true);


    public String sanitize(String html) {
        if (html == null || html.trim().isEmpty()) {
            return html;
        }
        return Jsoup.clean(html, safelist);
    }
}
