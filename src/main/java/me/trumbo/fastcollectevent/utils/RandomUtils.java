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

    public static Object[] getRandomEventItem(String item) {
        if (item == null) {
            return new Object[]{Material.DIAMOND, 64};
        }

        String[] parts = item.split(";");
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
            } catch (NumberFormatException ignored) {}
        }

        return new Object[]{material, amount};
    }


    public static int parseRandomRange(String rangeStr) {

            String[] range = rangeStr.split("-");
            if (range.length == 2) {
                int min = Integer.parseInt(range[0]);
                int max = Integer.parseInt(range[1]);
                return getRandomInt(min, max);
            }

            return Integer.parseInt(rangeStr);

    }
}
