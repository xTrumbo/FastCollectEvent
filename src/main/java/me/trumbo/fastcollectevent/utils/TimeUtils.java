package me.trumbo.fastcollectevent.utils;

public final class TimeUtils {
    private TimeUtils() {}

    public static TimeRemaining ticksToTime(long ticks) {
        int totalSeconds = (int) (ticks / 20);
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return new TimeRemaining(hours, minutes, seconds);
    }

    public static String formatTime(String message, TimeRemaining time, String... additionalReplacements) {
        String formatted = message
                .replace("%hours%", String.valueOf(time.getHours()))
                .replace("%minutes%", String.valueOf(time.getMinutes()))
                .replace("%seconds%", String.valueOf(time.getSeconds()));

        for (int i = 0; i < additionalReplacements.length - 1; i += 2) {
            formatted = formatted.replace(additionalReplacements[i], additionalReplacements[i + 1]);
        }
        return formatted;
    }

    public static class TimeRemaining {
        private final int hours;
        private final int minutes;
        private final int seconds;

        public TimeRemaining(int hours, int minutes, int seconds) {
            this.hours = hours;
            this.minutes = minutes;
            this.seconds = seconds;
        }

        public int getHours() {
            return hours;
        }

        public int getMinutes() {
            return minutes;
        }

        public int getSeconds() {
            return seconds;
        }
    }
}