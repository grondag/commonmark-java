package grondag.mcmd.renderer.mc;

import java.io.IOException;

public class McMdContentWriter {

    private final Appendable buffer;

    private char lastChar;

    public McMdContentWriter(Appendable out) {
        buffer = out;
    }

    public void whitespace() {
        if (lastChar != 0 && lastChar != ' ') {
            append(' ');
        }
    }

    public void colon() {
        if (lastChar != 0 && lastChar != ':') {
            append(':');
        }
    }

    public void writeStripped(String s) {
        append(s.replaceAll("[\\r\\n\\s]+", " "));
    }

    public void write(String s) {
        append(s);
    }

    public void write(char c) {
        append(c);
    }

    private void append(String s) {
        try {
            buffer.append(s);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        final int length = s.length();
        if (length != 0) {
            lastChar = s.charAt(length - 1);
        }
    }

    private void append(char c) {
        try {
            buffer.append(c);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        lastChar = c;
    }
}
