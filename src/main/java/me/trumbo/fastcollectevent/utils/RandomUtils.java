package me.trumbo.fastcollectevent.utils;

import org.bukkit.Material;

import java.util.List;
import java.util.Random;

public final class RandomUtils {
    private static final Random RANDOM = new Random();

    private RandomUtils() {}

    public static int getRandomInt(int min, int max) {
        return min + RANDOM.nextInt(max - min + 1);
    }

    public static <T> T getRandomElement(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(RANDOM.nextInt(list.size()));
    }

    public static Object[] getRandomEventItem(List<String> items) {
        if (items == null || items.isEmpty()) {
            return new Object[]{Material.DIAMOND, 64};
        }

        String selectedItem = getRandomElement(items);
        String[] parts = selectedItem.split(";");

        if (parts.length != 2) {
            return new Object[]{Material.DIAMOND, 64};
        }

        Material material = Material.matchMaterial(parts[0]);
        if (material == null) {
            material = Material.DIAMOND;
        }

        String[] range = parts[1].split("-");
        int amount = 64;
        if (range.length == 2) {
            try {
                int min = Integer.parseInt(range[0]);
                int max = Integer.parseInt(range[1]);
                amount = getRandomInt(min, max);
            } catch (NumberFormatException e) {
                amount = 64;
            }
        }

        return new Object[]{material, amount};
    }

    public static int parseRandomRange(String rangeStr) {
        String[] range = rangeStr.split("-");
        if (range.length == 2) {
            try {
                int min = Integer.parseInt(range[0]);
                int max = Integer.parseInt(range[1]);
                return getRandomInt(min, max);
            } catch (NumberFormatException e) {
                return Integer.parseInt(range[0]);
            }
        }
        return Integer.parseInt(rangeStr);
    }
}