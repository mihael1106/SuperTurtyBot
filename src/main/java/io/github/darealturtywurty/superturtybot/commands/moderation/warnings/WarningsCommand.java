package io.github.darealturtywurty.superturtybot.commands.moderation.warnings;

import java.awt.Color;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class WarningsCommand extends CoreCommand {
    public WarningsCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "user", "The user to clear warns from", true));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MODERATION;
    }
    
    @Override
    public String getDescription() {
        return "Gets all warnings for a user";
    }
    
    @Override
    public String getName() {
        return "warnings";
    }
    
    @Override
    public String getRichName() {
        return "Gather Warnings";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.deferReply(true).setContent("This command can only be used inside of a server!")
                .mentionRepliedUser(false).queue();
            return;
        }

        final User user = event.getOption("user").getAsUser();
        final Set<WarnInfo> warns = WarnManager.getWarns(event.getGuild(), user);
        
        final var embed = new EmbedBuilder();
        embed.setColor(Color.BLUE);
        embed.setTitle(user.getName() + "'s warnings!");
        if (warns.isEmpty()) {
            embed.setDescription("This user has no warns!");
        } else {
            embed.setDescription("This user has " + warns.size() + " warns!");
            int index = 1;
            for (final var warn : warns) {
                embed.addField("Warn #" + index++,
                    "Reason: `" + warn.reason() + "`\nUUID: `" + warn.uuid().toString() + "`\nModerator: "
                        + warn.warner().getAsMention() + "\nOccured On: "
                        + formatTime(Instant.ofEpochMilli(warn.warnTime()).atOffset(ZoneOffset.UTC)),
                    false);
            }
        }
        
        embed.setTimestamp(Instant.now());
        embed.setFooter(event.getMember().getEffectiveName() + "#" + event.getUser().getDiscriminator(),
            event.getMember().getEffectiveAvatarUrl());
        
        event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
    }

    // TODO: Utility class
    private static String formatTime(OffsetDateTime time) {
        return time.format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }
}
