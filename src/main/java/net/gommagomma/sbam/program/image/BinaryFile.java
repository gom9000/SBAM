package net.gommagomma.sbam.program.image;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Un file binario grezzo (.bin): i byte così come sono, da un indirizzo di partenza in poi. */
public final class BinaryFile
{
    private BinaryFile() {}

    /** Legge il file e mette il suo primo byte all'indirizzo dato. */
    public static MemoryImage read(Path file, int origin)
    {
        byte[] content;
        try {
            content = Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException("file binario non leggibile: " + file, e);
        }
        int[] values = new int[content.length];
        for (int i = 0; i < content.length; i++) values[i] = content[i] & 0xFF;
        return new MemoryImage().write(origin, values);
    }
}
