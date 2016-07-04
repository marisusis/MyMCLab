package me.megamichiel.mymclab.api;

import java.util.ArrayList;
import java.util.List;

public class Modal {

    private static int modalCount = 0, itemCount = 0;

    private final int id;
    private final String title;

    private final List<Item> items = new ArrayList<>();

    public Modal(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public Modal(String title) {
        this(modalCount++, title);
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<Item> getItems() {
        return items;
    }

    public static class Item {

        private final int id;

        private final String html;
        private final List<ItemClickListener> clickListeners = new ArrayList<>();

        public Item(int id, String html) {
            this.id = id;
            this.html = html;
        }

        public Item(String html) {
            this(itemCount++, html);
        }

        public int getId() {
            return id;
        }

        public String getHtml() {
            return html;
        }

        public List<ItemClickListener> getClickListeners() {
            return clickListeners;
        }

        public void onClick(Client client, Modal modal) {
            for (ItemClickListener listener : clickListeners)
                listener.onClick(client, modal, this);
        }
    }

    public interface ItemClickListener {
        void onClick(Client client, Modal modal, Item item);
    }
}
