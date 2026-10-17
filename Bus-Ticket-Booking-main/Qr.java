import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Minimal QR Code generator: byte mode, error-correction level M, versions 1-10. */
public class Qr {
    static final int[] ECC_PER_BLOCK = {-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26};
    static final int[] NUM_BLOCKS = {-1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5};
    final int ver, size;
    final boolean[][] m, fn;

    /** @return square module matrix (true = dark), without quiet zone */
    static boolean[][] make(String text) {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        for (int v = 1; v <= 10; v++)
            if (4 + (v <= 9 ? 8 : 16) + data.length * 8 <= dataCodewords(v) * 8) return new Qr(v, data).m;
        throw new IllegalArgumentException("Text too long for QR code");
    }

    static int rawModules(int v) {
        int r = (16 * v + 128) * v + 64;
        if (v >= 2) { int n = v / 7 + 2; r -= (25 * n - 10) * n - 55; if (v >= 7) r -= 36; }
        return r;
    }
    static int dataCodewords(int v) { return rawModules(v) / 8 - ECC_PER_BLOCK[v] * NUM_BLOCKS[v]; }

    Qr(int v, byte[] data) {
        ver = v; size = v * 4 + 17; m = new boolean[size][size]; fn = new boolean[size][size];
        drawFunction();
        drawCodewords(addEcc(codewords(data)));
        int best = 0, bestPen = Integer.MAX_VALUE;
        for (int mask = 0; mask < 8; mask++) {
            applyMask(mask); drawFormat(mask);
            int pen = penalty();
            if (pen < bestPen) { best = mask; bestPen = pen; }
            applyMask(mask);
        }
        applyMask(best); drawFormat(best);
    }

    byte[] codewords(byte[] d) {
        int cap = dataCodewords(ver);
        List<Boolean> b = new ArrayList<>();
        put(b, 4, 4); put(b, d.length, ver <= 9 ? 8 : 16);
        for (byte x : d) put(b, x & 0xFF, 8);
        for (int i = 0; i < 4 && b.size() < cap * 8; i++) b.add(false);
        while (b.size() % 8 != 0) b.add(false);
        for (int pad = 0xEC; b.size() < cap * 8; pad ^= 0xEC ^ 0x11) put(b, pad, 8);
        byte[] out = new byte[cap];
        for (int i = 0; i < b.size(); i++) if (b.get(i)) out[i >> 3] |= 1 << (7 - (i & 7));
        return out;
    }
    static void put(List<Boolean> b, int val, int len) { for (int i = len - 1; i >= 0; i--) b.add(((val >>> i) & 1) != 0); }

    byte[] addEcc(byte[] data) {
        int nb = NUM_BLOCKS[ver], el = ECC_PER_BLOCK[ver], raw = rawModules(ver) / 8;
        int shortBlocks = nb - raw % nb, shortLen = raw / nb;
        byte[][] blocks = new byte[nb][];
        byte[] div = divisor(el);
        for (int i = 0, k = 0; i < nb; i++) {
            int len = shortLen - el + (i < shortBlocks ? 0 : 1);
            byte[] dat = java.util.Arrays.copyOfRange(data, k, k + len); k += len;
            byte[] ecc = remainder(dat, div);
            byte[] blk = java.util.Arrays.copyOf(dat, shortLen + 1);     // short blocks keep one unused slot
            System.arraycopy(ecc, 0, blk, blk.length - el, el);
            blocks[i] = blk;
        }
        byte[] out = new byte[raw]; int idx = 0;
        for (int i = 0; i < shortLen + 1; i++)
            for (int j = 0; j < nb; j++)
                if (i != shortLen - el || j >= shortBlocks) out[idx++] = blocks[j][i];
        return out;
    }

    static byte[] divisor(int degree) {
        byte[] r = new byte[degree]; r[degree - 1] = 1; int root = 1;
        for (int i = 0; i < degree; i++) {
            for (int j = 0; j < r.length; j++) { r[j] = (byte) mul(r[j] & 0xFF, root); if (j + 1 < r.length) r[j] ^= r[j + 1]; }
            root = mul(root, 2);
        }
        return r;
    }
    static byte[] remainder(byte[] data, byte[] div) {
        byte[] r = new byte[div.length];
        for (byte b : data) {
            int f = (b ^ r[0]) & 0xFF;
            System.arraycopy(r, 1, r, 0, r.length - 1); r[r.length - 1] = 0;
            for (int i = 0; i < r.length; i++) r[i] ^= mul(div[i] & 0xFF, f);
        }
        return r;
    }
    static int mul(int x, int y) { int z = 0; for (int i = 7; i >= 0; i--) { z = (z << 1) ^ ((z >>> 7) * 0x11D); z ^= ((y >>> i) & 1) * x; } return z; }

    void setFn(int x, int y, boolean dark) { m[y][x] = dark; fn[y][x] = true; }

    void drawFunction() {
        for (int i = 0; i < size; i++) { setFn(6, i, i % 2 == 0); setFn(i, 6, i % 2 == 0); }
        finder(3, 3); finder(size - 4, 3); finder(3, size - 4);
        int[] al = alignPos(); int n = al.length;
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++)
            if (!((i == 0 && j == 0) || (i == 0 && j == n - 1) || (i == n - 1 && j == 0))) align(al[i], al[j]);
        drawFormat(0); drawVersion();
    }
    int[] alignPos() {
        if (ver == 1) return new int[0];
        int n = ver / 7 + 2, step = (ver * 4 + n * 2 + 1) / (n * 2 - 2) * 2;
        int[] r = new int[n]; r[0] = 6;
        for (int i = n - 1, pos = ver * 4 + 10; i >= 1; i--, pos -= step) r[i] = pos;
        return r;
    }
    void finder(int cx, int cy) {
        for (int dy = -4; dy <= 4; dy++) for (int dx = -4; dx <= 4; dx++) {
            int d = Math.max(Math.abs(dx), Math.abs(dy)), x = cx + dx, y = cy + dy;
            if (x >= 0 && x < size && y >= 0 && y < size) setFn(x, y, d != 2 && d != 4);
        }
    }
    void align(int cx, int cy) { for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) setFn(cx + dx, cy + dy, Math.max(Math.abs(dx), Math.abs(dy)) != 1); }
    static boolean bit(long v, int i) { return ((v >>> i) & 1) != 0; }

    void drawFormat(int mask) {
        int data = 0 << 3 | mask;                       // error-correction level M = 00
        int rem = data;
        for (int i = 0; i < 10; i++) rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
        int bits = (data << 10 | rem) ^ 0x5412;
        for (int i = 0; i <= 5; i++) setFn(8, i, bit(bits, i));
        setFn(8, 7, bit(bits, 6)); setFn(8, 8, bit(bits, 7)); setFn(7, 8, bit(bits, 8));
        for (int i = 9; i < 15; i++) setFn(14 - i, 8, bit(bits, i));
        for (int i = 0; i < 8; i++) setFn(size - 1 - i, 8, bit(bits, i));
        for (int i = 8; i < 15; i++) setFn(8, size - 15 + i, bit(bits, i));
        setFn(8, size - 8, true);
    }
    void drawVersion() {
        if (ver < 7) return;
        int rem = ver;
        for (int i = 0; i < 12; i++) rem = (rem << 1) ^ ((rem >>> 11) * 0x1F25);
        long bits = (long) ver << 12 | rem;
        for (int i = 0; i < 18; i++) { boolean b = bit(bits, i); int a = size - 11 + i % 3, c = i / 3; setFn(a, c, b); setFn(c, a, b); }
    }

    void drawCodewords(byte[] data) {
        int i = 0;
        for (int right = size - 1; right >= 1; right -= 2) {
            if (right == 6) right = 5;
            for (int vert = 0; vert < size; vert++)
                for (int j = 0; j < 2; j++) {
                    int x = right - j, y = ((right + 1) & 2) == 0 ? size - 1 - vert : vert;
                    if (!fn[y][x] && i < data.length * 8) { m[y][x] = bit(data[i >>> 3], 7 - (i & 7)); i++; }
                }
        }
    }
    void applyMask(int mask) {
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            boolean inv;
            switch (mask) {
                case 0: inv = (x + y) % 2 == 0; break;
                case 1: inv = y % 2 == 0; break;
                case 2: inv = x % 3 == 0; break;
                case 3: inv = (x + y) % 3 == 0; break;
                case 4: inv = (x / 3 + y / 2) % 2 == 0; break;
                case 5: inv = x * y % 2 + x * y % 3 == 0; break;
                case 6: inv = (x * y % 2 + x * y % 3) % 2 == 0; break;
                default: inv = ((x + y) % 2 + x * y % 3) % 2 == 0;
            }
            if (!fn[y][x] && inv) m[y][x] = !m[y][x];
        }
    }
    int penalty() {
        int r = 0, dark = 0;
        for (int pass = 0; pass < 2; pass++)
            for (int a = 0; a < size; a++) {
                int run = 1;
                for (int b = 1; b < size; b++) {
                    boolean cur = pass == 0 ? m[a][b] : m[b][a], prev = pass == 0 ? m[a][b - 1] : m[b - 1][a];
                    if (cur == prev) { run++; if (run == 5) r += 3; else if (run > 5) r++; } else run = 1;
                }
            }
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            if (m[y][x]) dark++;
            if (x < size - 1 && y < size - 1 && m[y][x] == m[y][x + 1] && m[y][x] == m[y + 1][x] && m[y][x] == m[y + 1][x + 1]) r += 3;
        }
        int total = size * size, k = (Math.abs(dark * 20 - total * 10) + total - 1) / total - 1;
        return r + Math.max(0, k) * 10;
    }
}
