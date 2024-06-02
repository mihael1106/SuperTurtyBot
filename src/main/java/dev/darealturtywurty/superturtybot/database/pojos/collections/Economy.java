package dev.darealturtywurty.superturtybot.database.pojos.collections;

import dev.darealturtywurty.superturtybot.modules.economy.Loan;
import dev.darealturtywurty.superturtybot.modules.economy.Property;
import dev.darealturtywurty.superturtybot.modules.economy.ShopItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
public class Economy {
    private long guild;
    private long user;

    private int wallet;
    private int bank;

    private long nextRob;
    private long nextWork;
    private long nextCrime;
    private long nextSexWork;

    private long nextDaily;
    private long nextWeekly;
    private long nextMonthly;
    private long nextYearly;

    private long nextLoan;

    private Job job;
    private int jobLevel;
    private boolean readyForPromotion;

    private long totalBetLoss;
    private long totalBetWin;

    private List<ShopItem> shopItems = new ArrayList<>();
    private List<ShopItem> shopItemsOnSale = new ArrayList<>();
    private List<Loan> loans = new ArrayList<>();
    private List<Property> properties = new ArrayList<>();

    public Economy() {
        this(0, 0);
    }

    public Economy(long guild, long user) {
        this.guild = guild;
        this.user = user;
    }

    public void addBank(int amount) {
        this.bank += amount;
    }

    public void addWallet(int amount) {
        this.wallet += amount;
    }

    public void removeBank(int amount) {
        this.bank -= amount;
    }

    public void removeWallet(int amount) {
        this.wallet -= amount;
    }

    @Getter
    @AllArgsConstructor
    @ToString
    public enum Job {
        PROGRAMMER(100, 2, 0.45f, 450),
        YOUTUBER(300, 5, 0.1f, 1350),
        MUSICIAN(50, 20, 0.2f, 900),
        ARTIST(50, 20, 0.15f, 675),
        DOCTOR(350, 1.125f, 0.25f, 1350),
        MATHEMATICIAN(150, 1.4f, 0.35f, 225);

        private final int salary;
        private final float promotionMultiplier;
        private final float promotionChance;
        private final int workCooldownSeconds;
    }
}