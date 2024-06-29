package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.PublicShop;
import dev.darealturtywurty.superturtybot.modules.economy.ShopItem;
import dev.darealturtywurty.superturtybot.modules.economy.ShopItemRegistry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class ShopCommand extends EconomyCommand {
    private static final List<String> SHOP_ITEMS;

    static {
        SHOP_ITEMS = ShopItemRegistry.SHOP_ITEMS.getRegistry().keySet().stream().toList();
    }
    @Override
    public List<SubcommandData> createSubcommandData() {
        return List.of(new SubcommandData("view", "View the shop")
                        .addOption(OptionType.USER, "user", "The user to view the shop of", false),
                new SubcommandData("buy", "Buy an item from the shop")
                        .addOption(OptionType.STRING, "item", "The ID of the item you want to buy", true, true)
                        .addOption(OptionType.USER, "from", "The user to buy the items from", false),
                new SubcommandData("sell", "Sell an item to the shop")
                        .addOption(OptionType.STRING, "item", "The ID of the item you want to sell", true, true)
                        .addOption(OptionType.BOOLEAN, "instant",
                                "If the item will just be sold for money or if it will be put up for sale for else to buy it",
                                false));
    }

    @Override
    public String getDescription() {
        return "Access the shop to view, buy, sell, and view all your items!";
    }

    @Override
    public String getName() {
        return "shop";
    }

    @Override
    public String getRichName() {
        return "Shop";
    }

    @Override
    public String getHowToUse() {
        return "/shop view <user>\n/shop buy <item_id>\n/shop sell <item_id> <amount>\n/shop list";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.getHook().editOriginal("❌ You must provide a subcommand!").queue();
            return;
        }

        switch (subcommand) {
            case "view" -> {
                User user = event.getOption("user", null, OptionMapping::getAsUser);
                if (user != null) {
                    Member member = guild.getMember(user);
                    if (member == null) {
                        event.getHook().editOriginal("❌ That user is not in this server!").queue();
                        return;
                    }

                    Economy account = EconomyManager.getOrCreateAccount(guild, user);
                    List<ShopItem> shop = account.getShopItems();
                    if (shop.isEmpty()) {
                        event.getHook().editOriginal("❌ That user does not have any items in their shop!").queue();
                        return;
                    }

                    EmbedBuilder builder = new EmbedBuilder();
                    Hashtable<ShopItem, Integer> amount = countShopItems(shop);

                    for (ShopItem item : amount.keySet()) {
                        String name = StringUtils.upperSnakeToSpacedPascal(item.getName());
                        int itemAmount = amount.get(item);
                        builder.addField(name, """
                                Amount: %d
                                Price: %s%d
                                Combined Price: %s%d""".formatted(
                                itemAmount,
                                config.getEconomyCurrency(), item.getPrice(),
                                config.getEconomyCurrency(), item.getPrice() * itemAmount), false);
                    }

                    builder.setTimestamp(Instant.now());
                    builder.setTitle("Shop for " + user.getName());
                    builder.setColor(member.getColorRaw());
                    builder.setFooter(user.getName(), member.getEffectiveAvatarUrl());
                    event.getHook().sendMessageEmbeds(builder.build()).queue();
                    return;
                }

                PublicShop shop = EconomyManager.getPublicShop();
                if (shop.isEmpty()) {
                    event.getHook().editOriginal("❌ The shop is currently empty, please come back later!").queue();
                    return;
                }

                event.getHook().sendMessageEmbeds(createShopEmbed(event, guild, shop).build()).queue();
            }
            case "buy" -> {
                String id = event.getOption("item", null, OptionMapping::getAsString);
                ShopItem item = getShopItem(id);
                if(item == null) {
                    event.getHook().editOriginal("❌ That item does not exist!").queue();
                    return;
                }

                User user = event.getOption("user", null, OptionMapping::getAsUser);
                Economy localAccount = EconomyManager.getOrCreateAccount(guild, event.getUser());
                int price = item.getPrice();
                if(!(localAccount.getBank() >= price)) {
                    event.getHook().editOriginal("❌ You do not have enough money in the bank!").queue();
                    return;
                }

                if(user == null) {
                    PublicShop shop = EconomyManager.getPublicShop();
                    if(!shop.getDailyItems().contains(item)) {
                        event.getHook().editOriginal("❌ That item is currently not available, please come back later!").queue();
                        return;
                    }
                    shop.getDailyItems().remove(item);
                    localAccount.removeBank(price);
                    localAccount.getShopItems().add(item);
                    EconomyManager.updateAccount(localAccount);
                    event.getHook().editOriginal("✅ Successfully bought an item!").queue();
                } else {
                    Economy remoteAccount = EconomyManager.getOrCreateAccount(guild, user);
                    if(remoteAccount.getShopItemsOnSale().contains(item)) {
                        remoteAccount.getShopItemsOnSale().remove(item);
                        remoteAccount.addBank(price);
                        EconomyManager.updateAccount(remoteAccount);

                        localAccount.getShopItems().add(item);
                        localAccount.removeBank(price);
                        EconomyManager.updateAccount(localAccount);
                        event.getHook().editOriginal("✅ Successfully bought an item from %s!".formatted(user.getEffectiveName())).queue();
                    }
                    event.getHook().editOriginal("❌ Specified user does not have that item for sale!").queue();
                }
            }
            case "sell" -> {
                ShopItem item = getShopItem(event.getOption("item", null, OptionMapping::getAsString));
                if(item == null) {
                    event.getHook().editOriginal("❌ That item does not exist!").queue();
                    return;
                }

                Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());

                if(!account.getShopItems().contains(item)) {
                    event.getHook().editOriginal("❌ You do not have that item!").queue();
                    return;
                }

                boolean instant = event.getOption("instant", true, OptionMapping::getAsBoolean);
                account.getShopItems().remove(item);
                if(instant) {
                    account.addBank(item.getPrice());
                    EconomyManager.updateAccount(account);
                    event.getHook().editOriginal("✅ Successfully sold an item!").queue();
                } else {
                    account.getShopItemsOnSale().add(item);
                    EconomyManager.updateAccount(account);
                    event.getHook().editOriginal("✅ Successfully put up item for sale!").queue();
                }
            }
        }
    }

    private EmbedBuilder createShopEmbed(SlashCommandInteractionEvent event, Guild guild, PublicShop shop) {
        EmbedBuilder builder = new EmbedBuilder();
        Hashtable<ShopItem, Integer> amount = countShopItems(shop.getDailyItems());

        StringBuilder sb = new StringBuilder();
        for(ShopItem item : amount.keySet()) {
            String name = StringUtils.upperSnakeToSpacedPascal(item.getName());
            sb.append("%s x%d".formatted(name, amount.get(item))).append("\n");
        }
        builder.setDescription(sb.toString());

        User user = event.getUser();
        Member member = guild.getMember(user);
        builder.setTimestamp(Instant.now());
        builder.setTitle("Daily shop items");
        builder.setColor(member.getColorRaw());
        builder.setFooter(user.getName(), member.getEffectiveAvatarUrl());
        return builder;
    }

    public Hashtable<ShopItem, Integer> countShopItems(List<ShopItem> itemList) {
        Hashtable<ShopItem, Integer> amount = new Hashtable<>();
        for(ShopItem item : itemList) {
            if(item == null) {
                Constants.LOGGER.warn("An item should not be null");
                continue;
            }
            Integer integer = amount.get(item);
            if(integer == null)
                integer = 0;
            amount.put(item, integer + 1);
        }
        return amount;
    }

    private ShopItem getShopItem(String id) {
        if(id == null) {
            return null;
        }
        return ShopItemRegistry.SHOP_ITEMS.getRegistry().get(id.toLowerCase());
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equalsIgnoreCase(getName()))
            return;
        String subcommand = event.getSubcommandName();
        if(subcommand == null)
            return;

        if((subcommand.equalsIgnoreCase("buy") || (subcommand.equalsIgnoreCase("sell"))
                && event.getFocusedOption().getName().equalsIgnoreCase("item"))) {

            final List<String> allowed = new ArrayList<>(SHOP_ITEMS.stream()
                    .filter(str -> str.toLowerCase()
                            .contains(event.getFocusedOption().getValue().toLowerCase()))
                    .toList());
            allowed.replaceAll(StringUtils::upperSnakeToSpacedPascal);
            event.replyChoiceStrings(allowed).queue();
        }
    }
}