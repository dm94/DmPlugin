package com.deeme.tasks.autoshop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Reads the item list that the game embeds in the shop pages, inside the
 * Shop.Parameters javascript object.
 */
public final class ShopPageParser {
    private static final String PARAMETERS = "Shop.Parameters";

    private ShopPageParser() {
    }

    /**
     * @return the items offered in the page, or null when the page could not be
     *         parsed
     */
    public static List<ShopItemData> parse(String html, ShopPage page) {
        JsonObject itemData = extractItemData(html);
        if (itemData == null) {
            return null;
        }

        List<ShopItemData> items = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : itemData.entrySet()) {
            ShopItemData item = parseItem(entry.getKey(), entry.getValue(), page);
            if (item != null) {
                items.add(item);
            }
        }

        items.sort(Comparator.comparing(ShopItemData::getCategory)
                .thenComparing(ShopItemData::getDisplayName));
        return items;
    }

    private static JsonObject extractItemData(String html) {
        if (html == null) {
            return null;
        }

        int start = html.indexOf(PARAMETERS);
        if (start < 0) {
            return null;
        }

        int braceStart = html.indexOf('{', start);
        if (braceStart < 0) {
            return null;
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = braceStart; i < html.length(); i++) {
            char c = html.charAt(i);

            // Braces inside a string value are part of the data, not of the object
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }

            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                try {
                    JsonObject parameters = JsonParser.parseString(html.substring(braceStart, i + 1))
                            .getAsJsonObject();
                    JsonElement itemData = parameters.get("itemData");
                    return itemData != null && itemData.isJsonObject() ? itemData.getAsJsonObject() : null;
                } catch (RuntimeException e) {
                    return null;
                }
            }
        }

        return null;
    }

    private static ShopItemData parseItem(String itemId, JsonElement element, ShopPage page) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }

        JsonObject object = element.getAsJsonObject();

        // Real money items can't be bought with credits or uridium
        JsonElement paymentItem = object.get("isPaymentItem");
        if (paymentItem != null && !paymentItem.isJsonNull() && paymentItem.getAsBoolean()) {
            return null;
        }

        String category = optString(object, "category", "");
        if (category.isEmpty()) {
            return null;
        }

        JsonElement priceElement = object.get("price");
        if (priceElement == null || priceElement.isJsonNull()) {
            return null;
        }

        ShopItemData item = new ShopItemData();
        item.itemId = itemId;
        item.name = optString(object, "code", itemId);
        item.category = category;
        item.page = page;
        item.price = priceElement.getAsDouble();
        item.currency = ShopItemData.normalizeCurrency(optString(object, "currency", ""));
        item.maxAmount = optInt(object, "maxAmount", 0);
        item.dailyLimit = optInt(object, "dailyLimitRemaining", -1);
        return item;
    }

    private static String optString(JsonObject object, String member, String fallback) {
        JsonElement element = object.get(member);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    private static int optInt(JsonObject object, String member, int fallback) {
        JsonElement element = object.get(member);
        return element == null || element.isJsonNull() ? fallback : element.getAsInt();
    }
}
