package dev.darealturtywurty.superturtybot.modules.economy;

import lombok.Data;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
public class PublicShop {
    private static final AtomicBoolean IS_RUNNING = new AtomicBoolean(false);
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    public static final PublicShop INSTANCE = new PublicShop();

    private List<ShopItem> dailyItems;

    public PublicShop() {
        this.dailyItems = new ArrayList<>();
    }

    public void reloadShop() {
        Hashtable<ShopItem, Integer> randomAmount = new Hashtable<>();
        Random random = ThreadLocalRandom.current();

        randomAmount.put(ShopItemRegistry.APPLE, random.nextInt(9) + 1);
        randomAmount.put(ShopItemRegistry.BANANA, random.nextInt(7) + 1);
        randomAmount.put(ShopItemRegistry.CHERRY, random.nextInt(5) + 1);
        randomAmount.put(ShopItemRegistry.ORANGE, random.nextInt(3) + 1);
        for(Map.Entry<ShopItem, Integer> entry : randomAmount.entrySet()) {
            for(int i = 0; i < entry.getValue(); i++) {
                this.dailyItems.add(entry.getKey());
            }
        }
    }

    public boolean isEmpty() {
        return this.dailyItems.isEmpty();
    }

    public static boolean isRunning() {
        return IS_RUNNING.get();
    }

    public static void run() {
        if (isRunning()) return;

        IS_RUNNING.set(true);

        // every midnight reload the shop
        INSTANCE.reloadShop();
        EXECUTOR.scheduleAtFixedRate(INSTANCE::reloadShop, getInitialDelay(), 24, TimeUnit.HOURS);
    }

    private static long getInitialDelay() {
        Calendar now = Calendar.getInstance();
        Calendar midnight = (Calendar) now.clone();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);

        if (now.after(midnight)) {
            midnight.add(Calendar.DAY_OF_MONTH, 1);
        }

        return midnight.getTimeInMillis() - now.getTimeInMillis();
    }
}
