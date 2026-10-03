package net.gommagomma.sbam.program;

import net.gommagomma.sbam.program.image.MemoryImage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * L'assemblatore del set di istruzioni (InstructionSet): dal testo del programma a un'immagine di memoria,
 * da mettere in una ROM o nella memoria interna di una CPU Harvard.
 *
 *     ; un commento, fino a fine riga
 *     LED     EQU 0xC000          ; un nome per un valore
 *             ORG 0x0000          ; da qui in poi gli indirizzi partono da qui
 *     inizio: LDI 0               ; un'etichetta: il nome dell'indirizzo di questa riga
 *     ciclo:  INC
 *             STA LED
 *             JMP ciclo
 *     tabella: DB 1, 2, 0x10      ; dei byte così come sono
 *
 * I numeri sono decimali, oppure esadecimali con 0x, $ o la h in fondo (0C000h), binari con 0b. Un operando
 * può essere un numero, un nome o una somma di questi (tabella+1). Due passate: le etichette si possono usare
 * prima di definirle. Gli errori dicono la riga.
 */
public final class Assembler
{
    private Assembler() {}

    /**
     * Quello che produce l'assemblatore: l'immagine da mettere in memoria, i nomi con il loro valore, e il listato
     * (indirizzo, byte, riga). Il programma vero è l'immagine; questo è il risultato del lavoro di traduzione.
     */
    public static final class Result
    {
        private final MemoryImage image;
        private final Map<String, Integer> symbols;
        private final List<String> listing;

        private Result(MemoryImage image, Map<String, Integer> symbols, List<String> listing)
        {
            this.image = image;
            this.symbols = symbols;
            this.listing = listing;
        }

        public MemoryImage image()           { return image; }
        public List<String> listing()        { return listing; }

        /** Il valore di un nome (etichetta o EQU). */
        public int symbol(String name)
        {
            Integer v = symbols.get(name.toUpperCase(Locale.ROOT));
            if (v == null) throw new IllegalArgumentException("nome non definito: " + name);
            return v;
        }
    }

    /** Assembla un testo. */
    public static Result assemble(String source)
    {
        return assemble("programma", source);
    }

    /** Assembla un file. */
    public static Result assemble(Path file)
    {
        try {
            return assemble(file.toString(), new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("programma non leggibile: " + file, e);
        }
    }

    /** @param name il nome del testo, per i messaggi d'errore */
    public static Result assemble(String name, String source)
    {
        String[] lines = source.split("\r?\n", -1);
        Map<String, Integer> symbols = new LinkedHashMap<>();
        pass(name, lines, symbols, null, null);                       // prima passata: gli indirizzi dei nomi
        MemoryImage image = new MemoryImage();
        List<String> listing = new ArrayList<>();
        pass(name, lines, symbols, image, listing);                   // seconda: i byte
        return new Result(image, symbols, listing);
    }

    /** Una passata; senza immagine raccoglie solo i nomi. */
    private static void pass(String name, String[] lines, Map<String, Integer> symbols, MemoryImage image, List<String> listing)
    {
        boolean emit = image != null;
        int pc = 0;
        for (int n = 0; n < lines.length; n++) {
            String where = name + ":" + (n + 1);
            String text = lines[n];
            int semicolon = text.indexOf(';');
            String code = (semicolon >= 0 ? text.substring(0, semicolon) : text).trim();
            if (code.isEmpty()) {
                if (emit && !text.trim().isEmpty()) listing.add(String.format("%-18s%s", "", text));
                continue;
            }
            // l'etichetta, se c'è
            int colon = code.indexOf(':');
            if (colon > 0 && isName(code.substring(0, colon).trim())) {
                define(symbols, code.substring(0, colon).trim(), pc, where, emit);
                code = code.substring(colon + 1).trim();
                if (code.isEmpty()) {
                    if (emit) listing.add(String.format("%04X%14s%s", pc, "", text));
                    continue;
                }
            }
            String[] parts = code.split("\\s+", 2);
            String word = parts[0];
            String rest = parts.length > 1 ? parts[1].trim() : "";

            // NOME EQU valore
            String[] equ = rest.split("\\s+", 2);
            if (equ[0].equalsIgnoreCase("EQU")) {
                if (!isName(word)) throw error(where, "nome non valido: " + word);
                if (equ.length < 2) throw error(where, "EQU senza valore");
                define(symbols, word, value(equ[1], symbols, where, true), where, emit);
                if (emit) listing.add(String.format("%-18s%s", "", text));
                continue;
            }

            int start = pc;
            int[] bytes;
            if (word.equalsIgnoreCase("ORG")) {
                pc = value(rest, symbols, where, true);
                if (pc < 0 || pc > 0xFFFF) throw error(where, "ORG fuori dai 64K");
                if (emit) listing.add(String.format("%04X%14s%s", pc, "", text));
                continue;
            } else if (word.equalsIgnoreCase("DB")) {
                String[] items = rest.split(",");
                bytes = new int[items.length];
                for (int i = 0; i < items.length; i++) bytes[i] = sized(value(items[i], symbols, where, emit), 1, where, emit);
            } else {
                Instruction ins = InstructionSet.byMnemonic(word);
                if (ins == null) throw error(where, "istruzione sconosciuta: " + word);
                int size = ins.operandBytes();
                if (size == 0 && !rest.isEmpty()) throw error(where, ins + " non vuole un operando");
                if (size > 0 && rest.isEmpty()) throw error(where, ins + " vuole un operando");
                bytes = new int[1 + size];
                bytes[0] = ins.opcode();
                if (size > 0) {
                    int v = sized(value(rest, symbols, where, emit), size, where, emit);
                    for (int i = 0; i < size; i++) bytes[1 + i] = (v >> (8 * i)) & 0xFF;    // prima il byte basso
                }
            }
            if (start + bytes.length > 0x10000) throw error(where, "il programma supera i 64K");
            if (emit) {
                try {
                    image.write(start, bytes);
                } catch (IllegalArgumentException e) {
                    throw error(where, e.getMessage());
                }
                listing.add(String.format("%04X  %-12s%s", start, hex(bytes), text));
            }
            pc = start + bytes.length;
        }
    }

    private static void define(Map<String, Integer> symbols, String name, int value, String where, boolean secondPass)
    {
        String key = name.toUpperCase(Locale.ROOT);
        if (InstructionSet.byMnemonic(key) != null) throw error(where, "un nome non può essere un'istruzione: " + name);
        if (secondPass) return;                                     // già definito alla prima passata
        if (symbols.containsKey(key)) throw error(where, "nome definito due volte: " + name);
        symbols.put(key, value);
    }

    /** Il valore di un operando: numeri e nomi, sommati o sottratti. Alla prima passata un nome mancante vale 0. */
    private static int value(String text, Map<String, Integer> symbols, String where, boolean required)
    {
        String t = text.trim().replace(" ", "");
        if (t.isEmpty()) throw error(where, "manca un valore");
        int total = 0, sign = 1, i = 0;
        while (i <= t.length()) {
            int j = i;
            while (j < t.length() && t.charAt(j) != '+' && t.charAt(j) != '-') j++;
            String term = t.substring(i, j);
            if (term.isEmpty()) throw error(where, "valore non valido: " + text);
            total += sign * term(term, symbols, where, required);
            if (j >= t.length()) break;
            sign = t.charAt(j) == '+' ? 1 : -1;
            i = j + 1;
        }
        return total;
    }

    private static int term(String term, Map<String, Integer> symbols, String where, boolean required)
    {
        String lower = term.toLowerCase(Locale.ROOT);
        try {
            if (lower.startsWith("0x")) return Integer.parseInt(term.substring(2), 16);
            if (lower.startsWith("$")) return Integer.parseInt(term.substring(1), 16);
            if (lower.startsWith("0b")) return Integer.parseInt(term.substring(2), 2);
            if (lower.endsWith("h") && Character.isDigit(term.charAt(0))) return Integer.parseInt(term.substring(0, term.length() - 1), 16);
            if (Character.isDigit(term.charAt(0))) return Integer.parseInt(term);
        } catch (NumberFormatException e) {
            throw error(where, "numero non valido: " + term);
        }
        if (!isName(term)) throw error(where, "valore non valido: " + term);
        Integer v = symbols.get(term.toUpperCase(Locale.ROOT));
        if (v != null) return v;
        if (required) throw error(where, "nome non definito: " + term);
        return 0;                                                   // prima passata: si saprà alla seconda
    }

    /** Controlla che un valore stia in quei byte. */
    private static int sized(int value, int bytes, String where, boolean check)
    {
        int max = bytes == 1 ? 0xFF : 0xFFFF;
        if (check && (value < 0 || value > max)) throw error(where, String.format("%d non sta in %d byte", value, bytes));
        return value & max;
    }

    private static boolean isName(String s)
    {
        if (s.isEmpty() || !(Character.isLetter(s.charAt(0)) || s.charAt(0) == '_')) return false;
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_')) return false;
        }
        return true;
    }

    private static String hex(int[] bytes)
    {
        StringBuilder sb = new StringBuilder();
        for (int b : bytes) sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }

    private static IllegalArgumentException error(String where, String what)
    {
        return new IllegalArgumentException(where + ": " + what);
    }
}
