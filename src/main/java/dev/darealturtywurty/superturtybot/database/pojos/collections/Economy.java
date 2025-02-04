package dev.darealturtywurty.superturtybot.database.pojos.collections;

import dev.darealturtywurty.superturtybot.modules.economy.Loan;
import dev.darealturtywurty.superturtybot.modules.economy.Property;
import dev.darealturtywurty.superturtybot.modules.economy.ShopItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Data
public class Economy {
    private long guild;
    private long user;

    private BigInteger wallet;
    private BigInteger bank;

    private long nextRob;
    private long nextWork;
    private long nextCrime;
    private long nextHeist;
    private long nextCrash;

    private long nextDaily;
    private long nextWeekly;
    private long nextMonthly;
    private long nextYearly;

    private long nextLoan;

    private Job job;
    private int jobLevel;
    private boolean readyForPromotion;

    private BigInteger totalBetLoss;
    private BigInteger totalBetWin;

    private int crimeLevel;
    private int heistLevel = 1;
    private int totalHeists;
    private long totalCrimes;
    private long totalSuccessfulCrimes;
    private long totalCaughtCrimes;

    private List<ShopItem> shopItems = new ArrayList<>();
    private List<Loan> loans = new ArrayList<>();
    private List<Property> properties = new ArrayList<>();

    public Economy() {
        this(0, 0);
    }

    public Economy(long guild, long user) {
        this.guild = guild;
        this.user = user;

        this.bank = BigInteger.ZERO;
        this.wallet = BigInteger.ZERO;

        this.totalBetWin = BigInteger.ZERO;
        this.totalBetLoss = BigInteger.ZERO;
    }

    public void addBank(BigInteger amount) {
        this.bank = this.bank.add(amount);
    }

    public void addWallet(BigInteger amount) {
        this.wallet = this.wallet.add(amount);
    }

    public void removeBank(BigInteger amount) {
        this.bank = this.bank.subtract(amount);
    }

    public void removeWallet(BigInteger amount) {
        this.wallet = this.wallet.subtract(amount);
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

        private final long salary;
        private final float promotionMultiplier;
        private final float promotionChance;
        private final int workCooldownSeconds;
    }
}