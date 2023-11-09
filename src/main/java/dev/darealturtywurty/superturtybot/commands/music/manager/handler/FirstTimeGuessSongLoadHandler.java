package dev.darealturtywurty.superturtybot.commands.music.manager.handler;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.darealturtywurty.superturtybot.commands.music.manager.AudioManager;
import dev.darealturtywurty.superturtybot.commands.music.manager.data.GuildAudioManager;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class FirstTimeGuessSongLoadHandler implements AudioLoadResultHandler {
    private final CompletableFuture<Either<AudioTrack, FriendlyException>> future;
    private final GuildAudioManager manager;
    private final Guild guild;
    private final AudioChannel channel;

    public FirstTimeGuessSongLoadHandler(CompletableFuture<Either<AudioTrack, FriendlyException>> future,
                                         GuildAudioManager manager,
                                         Guild guild,
                                         AudioChannel channel) {
        this.future = future;
        this.manager = manager;
        this.guild = guild;
        this.channel = channel;
    }

    @Override
    public void loadFailed(FriendlyException exception) {
        this.future.complete(Either.right(exception));
    }

    @Override
    public void noMatches() {
        this.future.complete(Either.left(null));
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        AudioManager.GUESS_THE_SONG_TRACKS.addAll(playlist.getTracks().stream().map(track -> track.getInfo().uri).toList());

        List<AudioTrack> tracks = playlist.getTracks();
        AudioTrack track = tracks.get(ThreadLocalRandom.current().nextInt(tracks.size()));
        this.manager.getMusicScheduler().setAudioChannel(this.channel);
        AudioManager.addGuessTheSongTrack(this.guild, track);
        this.future.complete(Either.left(track));
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        this.manager.getMusicScheduler().setAudioChannel(this.channel);
        AudioManager.addGuessTheSongTrack(this.guild, track);
        this.future.complete(Either.left(track));
    }
}
