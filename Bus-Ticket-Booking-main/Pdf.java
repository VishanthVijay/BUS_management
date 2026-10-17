import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Tiny dependency-free PDF writer (one page, built-in Helvetica) plus the BusGo ticket layout. */
public class Pdf {
    final StringBuilder cs = new StringBuilder();
    final double W, H;
    Pdf(double w, double h) { W = w; H = h; }

    static String f(double v) { return String.format(Locale.US, "%.2f", v); }
    static String col(int rgb) { return f(((rgb >> 16) & 255) / 255.0) + " " + f(((rgb >> 8) & 255) / 255.0) + " " + f((rgb & 255) / 255.0); }
    static String esc(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) { if (c == '(' || c == ')' || c == '\\') b.append('\\').append(c); else if (c < 32 || c > 126) b.append('?'); else b.append(c); }
        return b.toString();
    }
    static double width(String s, double size, boolean bold) { return s.length() * size * (bold ? 0.58 : 0.54); }

    void rect(double x, double top, double w, double h, int rgb) {
        cs.append(col(rgb)).append(" rg ").append(f(x)).append(' ').append(f(H - top - h)).append(' ').append(f(w)).append(' ').append(f(h)).append(" re f\n");
    }
    void text(double x, double baseline, String s, boolean bold, double size, int rgb) {
        cs.append("BT /").append(bold ? "F2" : "F1").append(' ').append(f(size)).append(" Tf ").append(col(rgb)).append(" rg ")
          .append(f(x)).append(' ').append(f(H - baseline)).append(" Td (").append(esc(s)).append(") Tj ET\n");
    }
    void textRight(double xRight, double baseline, String s, boolean bold, double size, int rgb) { text(xRight - width(s, size, bold), baseline, s, bold, size, rgb); }
    void dashed(double x1, double x2, double y, int rgb) {
        cs.append(col(rgb)).append(" RG 1.2 w [5 4] 0 d ").append(f(x1)).append(' ').append(f(H - y)).append(" m ").append(f(x2)).append(' ').append(f(H - y)).append(" l S [] 0 d\n");
    }
    void qr(boolean[][] q, double x, double top, double size) {
        int n = q.length; double u = size / (n + 8);   // 4-module quiet zone each side
        rect(x, top, size, size, 0xFFFFFF);
        cs.append("0 0 0 rg\n");
        for (int r = 0; r < n; r++) for (int c = 0; c < n; c++)
            if (q[r][c]) cs.append(f(x + (c + 4) * u)).append(' ').append(f(H - (top + (r + 4) * u) - u)).append(' ').append(f(u + 0.02)).append(' ').append(f(u + 0.02)).append(" re f\n");
    }

    byte[] build() throws IOException {
        byte[] content = cs.toString().getBytes(StandardCharsets.ISO_8859_1);
        ByteArrayOutputStream out = new ByteArrayOutputStream(); List<Integer> off = new ArrayList<>();
        out.write("%PDF-1.4\n".getBytes());
        String[] objs = {
            "<</Type/Catalog/Pages 2 0 R>>", "<</Type/Pages/Kids[3 0 R]/Count 1>>",
            "<</Type/Page/Parent 2 0 R/MediaBox[0 0 " + f(W) + " " + f(H) + "]/Contents 4 0 R/Resources<</Font<</F1 5 0 R/F2 6 0 R>>>>>>", null,
            "<</Type/Font/Subtype/Type1/BaseFont/Helvetica/Encoding/WinAnsiEncoding>>", "<</Type/Font/Subtype/Type1/BaseFont/Helvetica-Bold/Encoding/WinAnsiEncoding>>"};
        for (int i = 0; i < objs.length; i++) {
            off.add(out.size());
            out.write(((i + 1) + " 0 obj\n").getBytes());
            if (objs[i] == null) { out.write(("<</Length " + content.length + ">>\nstream\n").getBytes()); out.write(content); out.write("\nendstream".getBytes()); }
            else out.write(objs[i].getBytes());
            out.write("\nendobj\n".getBytes());
        }
        int xref = out.size();
        out.write(("xref\n0 " + (objs.length + 1) + "\n0000000000 65535 f \n").getBytes());
        for (int o : off) out.write(String.format("%010d 00000 n \n", o).getBytes());
        out.write(("trailer\n<</Size " + (objs.length + 1) + "/Root 1 0 R>>\nstartxref\n" + xref + "\n%%EOF\n").getBytes());
        return out.toByteArray();
    }

    // ---------------- BusGo e-ticket ----------------
    /** rows: Db.ticketRows(...) - all rows belong to one booking reference. */
    static void ticket(File file, String ref, List<Object[]> rows, String username) throws IOException {
        Object[] r0 = rows.get(0);
        String route = r0[4] + " -> " + r0[5];
        LocalDate date = LocalDate.parse((String) r0[6]); LocalTime dep = LocalTime.parse((String) r0[7]);
        int dur = Db.I(r0[15]); long total = dep.toSecondOfDay() / 60 + dur;
        String arrive = dep.plusMinutes(dur).toString() + (total >= 1440 ? " (+" + (total / 1440) + ")" : "");
        double sum = 0; StringBuilder seats = new StringBuilder();
        for (Object[] r : rows) { sum += Db.D(r[10]); seats.append(seats.length() > 0 ? "," : "").append(r[0]); }

        Pdf p = new Pdf(595, 600);
        p.rect(0, 0, 595, 600, 0xFFFFFF);
        p.rect(0, 0, 595, 116, 0x1E1B4B); p.rect(0, 116, 595, 4, 0x22D3EE);
        p.text(40, 62, "BusGo", true, 30, 0xFFFFFF); p.text(40, 84, "E-TICKET  |  Futuristic bus travel", false, 10, 0xC7D2FE);
        p.textRight(555, 44, "BOOKING REF", true, 9, 0xC7D2FE); p.textRight(555, 74, ref, true, 26, 0xF59E0B);

        p.text(40, 166, route, true, 24, 0x111827);
        p.text(40, 190, date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)) + "   |   Departs " + dep + "   |   Arrives " + arrive, false, 11.5, 0x374151);
        p.text(40, 210, r0[8] + "  -  " + r0[9], true, 12, 0x4F46E5);
        p.text(40, 242, "BOARDING POINT", true, 8.5, 0x6B7280); p.text(40, 258, String.valueOf(r0[11]), true, 12, 0x111827);
        p.text(215, 242, "DROPPING POINT", true, 8.5, 0x6B7280); p.text(215, 258, String.valueOf(r0[12]), true, 12, 0x111827);
        p.text(40, 284, "SEATS", true, 8.5, 0x6B7280); p.text(40, 300, seats.toString(), true, 12, 0x111827);

        String payload = "BUSGO|" + ref + "|" + route + "|" + date + " " + dep + "|Seats " + seats + "|" + r0[1];
        p.qr(Qr.make(payload.length() > 150 ? payload.substring(0, 150) : payload), 425, 138, 130);
        p.textRight(555, 282, "Scan at boarding", false, 9, 0x6B7280);

        p.dashed(40, 555, 326, 0xD1D5DB);
        p.rect(40, 342, 515, 24, 0xEEF2FF);
        p.text(50, 358, "SEAT", true, 9, 0x4F46E5); p.text(110, 358, "PASSENGER", true, 9, 0x4F46E5); p.text(340, 358, "AGE / GENDER", true, 9, 0x4F46E5); p.textRight(545, 358, "FARE", true, 9, 0x4F46E5);
        double y = 386;
        for (Object[] r : rows) {
            p.text(50, y, String.valueOf(r[0]), true, 12, 0x111827); p.text(110, y, String.valueOf(r[1]), false, 12, 0x111827);
            p.text(340, y, r[2] + " / " + r[3], false, 12, 0x374151); p.textRight(545, y, String.format(Locale.US, "Rs. %,.2f", Db.D(r[10])), false, 12, 0x111827);
            y += 24;
        }
        p.dashed(40, 555, 534, 0xD1D5DB);
        p.text(40, 562, "TOTAL PAID", true, 9, 0x6B7280); p.textRight(555, 568, String.format(Locale.US, "Rs. %,.2f", sum), true, 22, 0x4F46E5);
        p.text(40, 580, "Paid via " + r0[14] + "  |  Booked " + r0[13] + " by " + username, false, 9, 0x6B7280);
        p.text(40, 594, "Please arrive 15 minutes before departure and carry a valid photo ID.", false, 8.5, 0x9CA3AF);
        Files.write(file.toPath(), p.build());
    }
}
