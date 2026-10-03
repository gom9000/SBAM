package net.gommagomma.sbam.program.image;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Le immagini di memoria e i loro file: Intel HEX e binario. */
class ImageFilesTest
{
    @Test
    void intelHexGoesAndComesBack() throws Exception
    {
        MemoryImage image = new MemoryImage().write(0x0000, 0x01, 0x5A).write(0x7FF0, new int[20]).write(0x12345, 0xEE);
        Path file = Files.createTempFile("sbam", ".hex");
        IntelHex.write(image, file);
        String text = new String(Files.readAllBytes(file), "US-ASCII");
        assertTrue(text.startsWith(":02000000015AA3\n"), text);
        assertTrue(text.endsWith(":00000001FF\n"));
        MemoryImage back = IntelHex.read(file);
        assertEquals(0x5A, back.read(1));
        assertEquals(0, back.read(0x8003));
        assertEquals(0xEE, back.read(0x12345));
        assertFalse(back.isWritten(2));
        Files.write(file, ":0200000001FFFF\n:00000001FF\n".getBytes("US-ASCII"));          // somma sbagliata
        assertThrows(IllegalArgumentException.class, () -> IntelHex.read(file));
        Files.delete(file);
    }

    @Test
    void aBinaryFileStartsWhereItIsPut() throws Exception
    {
        Path file = Files.createTempFile("sbam", ".bin");
        Files.write(file, new byte[] { 1, (byte) 0xFF, 3 });
        MemoryImage image = BinaryFile.read(file, 0x100);
        assertEquals(0xFF, image.read(0x101));
        assertEquals(0x103, image.end());
        Files.delete(file);
    }

    @Test
    void anAddressCannotBeWrittenTwice()
    {
        MemoryImage image = new MemoryImage().write(0x10, 1);
        assertThrows(IllegalArgumentException.class, () -> image.write(0x10, 2));
        assertEquals(7, image.read(0x20, 7), "un indirizzo vuoto dà il valore scelto da chi legge");
    }
}
