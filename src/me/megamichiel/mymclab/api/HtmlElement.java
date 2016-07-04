package me.megamichiel.mymclab.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HtmlElement {

    private final List<HtmlElement> children = new ArrayList<>();
    private final Map<String, String> properties = new HashMap<>();

    private final String tag;

    public HtmlElement(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public List<HtmlElement> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<").append(tag);
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            sb.append(' ').append(entry.getKey()).append("=\"").append(entry.getValue()).append('"');
        }
        sb.append('>');
        for (HtmlElement child : children)
        sb.append(child.toString());
        return sb.append("</").append(tag).append('>').toString();
    }
}
