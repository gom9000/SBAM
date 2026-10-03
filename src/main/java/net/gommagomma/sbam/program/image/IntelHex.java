package net.gommagomma.sbam.program.image;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Il formato Intel HEX, quello dei programmatori di EPROM e degli assemblatori (MPLAB compreso):
 * righe ":LLAAAATT dati CC" con L la lunghezza, A l'indirizzo, T il tipo, C la somma di controllo.
 *
 * Tipi letti: 00 dati, 01 fine, 02 segmento esteso, 04 indirizzo lineare esteso; 03 e 05 (l'indirizzo
 * di partenza) si accettano e non servono. In scrittura: righe da 16 byte, 04 quando si supera 64K.
 */
public final class IntelHex
{
    private IntelHex() {}

    /** Legge un file Intel HEX. */
    public static MemoryImage read(Path file)
    {
        MemoryImage image = new MemoryImage();
        try {
            BufferedReader in = Files.newBufferedReader(file, StandardCharsets.US_ASCII);
            try {
                long base = 0;
                int number = 0;
                boolean ended = false;
                String line;
                while ((line = in.readLine()) != null) {
                    number++;
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if (ended) throw error(file, number, "righe dopo il record di fine");
                    int[] r = record(file, number, line);
                    int length = r[0], address = (r[1] << 8) | r[2], type = r[3];
                    switch (type) {
                        case 0x00:
                            int[] data = new int[length];
                            System.arraycopy(r, 4, data, 0, length);
                            long at = base + address;
                            if (at + length > Integer.MAX_VALUE) throw error(file, number, "indirizzo troppo grande");
                            image.write((int) at, data);
                            break;
                        case 0x01:
                            ended = true;
                            break;
                        case 0x02:
                            base = (long) ((r[4] << 8) | r[5]) << 4;
                            break;
                        case 0x04:
                            base = (long) ((r[4] << 8) | r[5]) << 16;
                            break;
                        case 0x03:
                        case 0x05:
                            break;
                        default:
                            throw error(file, number, String.format("tipo di record sconosciuto: %02X", type));
                    }
                }
                if (!ended) throw new IllegalArgumentException(file + ": manca il record di fine (:00000001FF)");
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("file Intel HEX non leggibile: " + file, e);
        }
        return image;
    }

    /** Scrive un'immagine in Intel HEX: solo gli indirizzi scritti, in righe da 16 byte al più. */
    public static void write(MemoryImage image, Path file)
    {
        try {
            Writer out = Files.newBufferedWriter(file, StandardCharsets.US_ASCII);
            try {
                long upper = 0;
                int a = 0;
                while (a < image.end()) {
                    if (!image.isWritten(a)) {
                        a++;
                        continue;
                    }
                    if ((a >>> 16) != upper) {
                        upper = a >>> 16;
                        line(out, 0, 0x04, new int[] { (int) (upper >> 8) & 0xFF, (int) upper & 0xFF });
                    }
                    int n = 0;
                    while (n < 16 && image.isWritten(a + n) && ((a + n) >>> 16) == upper) n++;
                    int[] data = new int[n];
                    for (int i = 0; i < n; i++) data[i] = image.read(a + i);
                    line(out, a & 0xFFFF, 0x00, data);
                    a += n;
                }
                line(out, 0, 0x01, new int[0]);
            } finally {
                out.close();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("file Intel HEX non scrivibile: " + file, e);
        }
    }

    /** I byte di una riga: lunghezza, indirizzo (2), tipo, dati...; con la somma di controllo verificata. */
    private static int[] record(Path file, int number, String line)
    {
        if (line.charAt(0) != ':' || line.length() < 11 || line.length() % 2 == 0) throw error(file, number, "riga non valida");
        int n = (line.length() - 1) / 2;
        int[] b = new int[n];
        int sum = 0;
        for (int i = 0; i < n; i++) {
            int hi = Character.digit(line.charAt(1 + 2 * i), 16), lo = Character.digit(line.charAt(2 + 2 * i), 16);
            if (hi < 0 || lo < 0) throw error(file, number, "cifra esadecimale non valida");
            b[i] = (hi << 4) | lo;
            sum += b[i];
        }
        if (b[0] != n - 5) throw error(file, number, "la lunghezza non torna");
        if ((sum & 0xFF) != 0) throw error(file, number, "somma di controllo sbagliata");
        return b;
    }

    private static void line(Writer out, int address, int type, int[] data) throws IOException
    {
        StringBuilder sb = new StringBuilder(":");
        int sum = data.length + (address >> 8) + (address & 0xFF) + type;
        sb.append(String.format("%02X%04X%02X", data.length, address, type));
        for (int d : data) {
            sb.append(String.format("%02X", d));
            sum += d;
        }
        sb.append(String.format("%02X", (-sum) & 0xFF));
        out.write(sb.append('\n').toString());
    }

    private static IllegalArgumentException error(Path file, int number, String what)
    {
        return new IllegalArgumentException(file + ":" + number + ": " + what);
    }
}
