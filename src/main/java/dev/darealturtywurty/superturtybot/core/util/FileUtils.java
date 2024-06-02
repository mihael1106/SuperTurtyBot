package dev.darealturtywurty.superturtybot.core.util;

import dev.darealturtywurty.superturtybot.TurtyBot;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {
    public static @Nullable BufferedImage loadImage(String filename) {
        InputStream stream = TurtyBot.loadResource(filename);
        if(stream == null)
            return null;

        try {
            return ImageIO.read(stream);
        }
        catch (IOException e) {
            Constants.LOGGER.error("Failed to load image", e);
            return null;
        }
    }
}
