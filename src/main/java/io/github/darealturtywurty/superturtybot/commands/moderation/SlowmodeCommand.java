package io.github.darealturtywurty.superturtybot.commands.moderation;

import java.util.List;

import org.apache.commons.math3.util.Pair;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class SlowmodeCommand extends CoreCommand {
    public SlowmodeCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.INTEGER, "time", "How long the cooldown is (in seconds)", false)
            .setRequiredRange(0, 21600));
    }
    
    @Override
    public String getAccess() {
        return "Moderators (Manage Channel Permission)";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }
    
    @Override
    public String getDescription() {
        return "Puts the current channel on slowmode";
    }

    @Override
    public String getHowToUse() {
        return "/slowmode\n/slowmode [time]";
    }
    
    @Override
    public String getName() {
        return "slowmode";
    }
    
    @Override
    public String getRichName() {
        return "Slowmode";
    }
    
    @Override
    public boolean isServerOnly() {
        return true;
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getChannelType() != ChannelType.TEXT) {
            event.deferReply(true).setContent("This command can only be used in channels that allow for slowmode!")
                .mentionRepliedUser(false).queue();
            return;
        }
        
        final int time = event.getOption("time", 5, OptionMapping::getAsInt);
        event.getChannel().asTextChannel().getManager().setSlowmode(time).queue();
        event.deferReply().setContent("I have changed this channel's slowmode cooldown to " + time + " seconds!")
            .mentionRepliedUser(false).queue();
        final Pair<Boolean, TextChannel> logging = BanCommand.canLog(event.getGuild());
        if (Boolean.TRUE.equals(logging.getKey())) {
            BanCommand.log(logging.getValue(), event.getMember().getAsMention() + " has put "
                + event.getChannel().getAsMention() + " on a " + time + " second slowmode!", false);
        }
    }
}
