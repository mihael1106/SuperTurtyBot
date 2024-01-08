package dev.darealturtywurty.superturtybot.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.darealturtywurty.superturtybot.commands.music.manager.AudioManager;
import dev.darealturtywurty.superturtybot.commands.music.manager.data.TrackData;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.List;

public class LeaveCleanupCommand extends CoreCommand {

    public LeaveCleanupCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }

    @Override
    public String getDescription() {
        return "Removes all songs in the queue that were added by users that are no longer in the voice channel.";
    }

    @Override
    public String getName() {
        return "leavecleanup";
    }

    @Override
    public String getRichName() {
        return "Leave Cleanup";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null || event.getMember() == null) {
            reply(event, "❌ I must be in a server for you to be able to use this command!", false, true);
            return;
        }

        net.dv8tion.jda.api.managers.AudioManager audioManager = event.getGuild().getAudioManager();
        if (!audioManager.isConnected() || audioManager.getConnectedChannel() == null) {
            reply(event, "❌ I must be in a voice channel for you to be able to use this command!", false, true);
            return;
        }

        GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        if (memberVoiceState == null || !memberVoiceState.inAudioChannel()) {
            reply(event, "❌ You must be in a voice channel for you to be able to use this command!", false, true);
            return;
        }

        AudioChannel channel = audioManager.getConnectedChannel();
        if (memberVoiceState.getChannel() == null || memberVoiceState.getChannel().getIdLong() != channel.getIdLong()) {
            reply(event, "❌ You must be in the same voice channel as me for you to be able to use this command!", false, true);
            return;
        }

        List<AudioTrack> queue = AudioManager.getQueue(event.getGuild());
        if (queue.isEmpty()) {
            reply(event, "❌ There are no songs in the queue!", false, true);
            return;
        }

        AudioTrack currentlyPlaying = AudioManager.getCurrentlyPlaying(event.getGuild());
        int queueSize = queue.size();
        for (AudioTrack track : queue) {
            if (track.equals(currentlyPlaying))
                continue;

            TrackData trackData = track.getUserData(TrackData.class);
            if (trackData == null)
                continue;

            if (channel.getMembers().stream().noneMatch(member -> member.getIdLong() == trackData.getUserId())) {
                AudioManager.removeTrack(event.getGuild(), track);
            }
        }

        int removed = queueSize - AudioManager.getQueue(event.getGuild()).size();
        if (removed > 0) {
            reply(event, "✅ Removed " + removed + " songs from the queue!", false, false);
        } else {
            reply(event, "❌ There were no songs in the queue that were added by users that are no longer in the voice channel!", false, true);
        }
    }
}
