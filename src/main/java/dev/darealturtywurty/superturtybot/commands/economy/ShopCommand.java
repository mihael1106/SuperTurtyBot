package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.FileUtils;
import dev.darealturtywurty.superturtybot.core.util.discord.PaginatedEmbed;
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
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;

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
                                false),
                new SubcommandData("list", "Lists all items you have"));
    }

    @Override
    public String getDescription() {
        return "Access the shop to view, buy, and sell items!";
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
        return "/shop view <user>\n/shop buy <item_id>\n/shop sell <item_id> <amount>";
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

                    var contents = new PaginatedEmbed.ContentsBuilder();
                    for (ShopItem item : shop) {
                        contents.field(item.getImage() + " " + item.getName(),
                                "ID: " + item.getId() + "\nPrice: " + item.getPrice());
                    }

                    PaginatedEmbed paginatedEmbed = new PaginatedEmbed.Builder(10, contents)
                            .timestamp(Instant.now())
                            .title("Shop for " + user.getName())
                            .color(member.getColorRaw())
                            .footer(user.getName(), member.getEffectiveAvatarUrl())
                            .authorOnly(event.getUser().getIdLong())
                            .build(event.getJDA());

                    paginatedEmbed.send(event.getHook());
                    return;
                }

                PublicShop shop = EconomyManager.getPublicShop();
                if (shop.getDiscountItems().isEmpty() && shop.getFeaturedItems().isEmpty() && shop.getNewItems()
                        .isEmpty()) {
                    event.getHook().editOriginal("❌ The shop is currently empty, please come back later!").queue();
                    return;
                }

                try {
                    var boas = new ByteArrayOutputStream();
                    ImageIO.write(generateShopImage(), "png", boas);
                    var upload = FileUpload.fromData(boas.toByteArray(), "shop.png");
                    event.getHook().sendFiles(upload).queue();
                } catch (IOException exception) {
                    event.getHook().editOriginal("❌ An error occurred while generating the shop image!").queue();
                    Constants.LOGGER.error("❌ An error occurred while generating the shop image!", exception);
                }
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
            case "list" -> {
                final EmbedBuilder builder = new EmbedBuilder();
                builder.setTitle("Item List");

                Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());
                List<ShopItem> shop = account.getShopItems();
                Hashtable<ShopItem, Integer> amount = new Hashtable<>();
                for(ShopItem item : shop) {
                    if(item == null) {
                        Constants.LOGGER.warn("An item should not be null");
                        continue;
                    }
                    Integer integer = amount.get(item);
                    if(integer == null)
                        integer = 0;
                    amount.put(item, integer + 1);
                }
                for(ShopItem item : amount.keySet()) {
                    builder.addField(item.getName(), String.valueOf(amount.get(item)), false);
                }
                builder.setFooter("Requested by " + event.getUser().getName(), event.getUser().getEffectiveAvatarUrl());

                event.getHook().sendMessageEmbeds(builder.build()).queue();
            }
        }
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

            final List<String> allowed = SHOP_ITEMS.stream()
                    .filter(str -> str.toLowerCase()
                    .contains(event.getFocusedOption().getValue().toLowerCase()))
                    .toList();
            event.replyChoiceStrings(allowed).queue();
        }
    }

    private static BufferedImage generateShopImage() throws IOException {
        PublicShop shop = EconomyManager.getPublicShop();

        BufferedImage image = FileUtils.loadImage("economy/shop.png");
        if (image == null)
            throw new IOException("Could not find shop image!");

        Graphics2D g2d = image.createGraphics();

        g2d.setColor(Color.RED);
        g2d.fillRect(0, image.getHeight() / 2 - 5, image.getWidth(), 10);
        g2d.setColor(Color.GREEN);
        g2d.fillRect(image.getWidth() / 2 - 5, 0, 10, image.getHeight());

        g2d.setFont(g2d.getFont().deriveFont(500f));
        g2d.setColor(Color.BLACK);
        g2d.drawString("New", image.getWidth() / 2 - g2d.getFontMetrics().stringWidth("New") / 2, 400);

        g2d.setFont(g2d.getFont().deriveFont(250f));

        List<ShopItem> newItems = shop.getFeaturedItems();

        int totalWidth = 0;
        for (ShopItem item : newItems) {
            String name = item.getName();
            totalWidth += Math.max(g2d.getFontMetrics().stringWidth(name), 750) + 50;
        }

        int x = (image.getWidth() - totalWidth) / 2 + 50;
        for (ShopItem item : newItems) {
            String name = item.getName();
            BufferedImage img = Objects.requireNonNull(FileUtils.loadImage(item.getImage()));

            g2d.setColor(Color.BLACK);
            g2d.fillRect(x - 5,
                    545,
                    Math.max(g2d.getFontMetrics().stringWidth(name) + 5, 755),
                    805 + g2d.getFontMetrics().getHeight());

            g2d.drawImage(img, x, 550, 750, 750, null);

            g2d.setColor(Color.WHITE);
            g2d.drawString(name, x + 375 - g2d.getFontMetrics().stringWidth(name) / 2, 1500);

            x += Math.max(g2d.getFontMetrics().stringWidth(name), 750) + 50;
        }

        g2d.dispose();
        return image;
    }
}