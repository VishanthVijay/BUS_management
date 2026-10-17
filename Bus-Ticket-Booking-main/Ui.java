import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.function.Consumer;

/** Look and feel: themes (light/dark), colours, fonts and reusable painted components. */
public class Ui {
    // ---- brand colours (same in both themes) ----
    static final Color PRIMARY = new Color(0x4F46E5), PRIMARY_DARK = new Color(0x3730A3), DEEP = new Color(0x1E1B4B),
            ACCENT = new Color(0xF59E0B), ACCENT_DARK = new Color(0xD97706), NEON = new Color(0x22D3EE), VIOLET = new Color(0x7C3AED),
            OK = new Color(0x059669), BAD = new Color(0xDC2626), WARN = new Color(0xD97706), PINK = new Color(0xEC4899), WINDOW = new Color(0x38BDF8);
    // ---- themed colours (set by applyTheme) ----
    static Color BG, CARD, TEXT, MUTED, LINE, SOFT, FIELD_BG, FIELD_LINE, CHIP, OK_BG, BADGE_BG, ROW_SEL, HEAD_BG, BUS_FILL, BUS_EDGE,
            SEAT_FREE, SEAT_FREE_EDGE, SEAT_TAKEN, SEAT_TAKEN_EDGE, SEAT_TAKEN_TEXT, SEAT_LADY, SEAT_LADY_EDGE;
    static boolean dark;
    static final String RS = "\u20B9 ", ARROW = "  \u2192  ", DOT = "  \u2022  ";
    static { applyTheme(false); }

    static Color c(int rgb) { return new Color(rgb); }
    static void applyTheme(boolean d) {
        dark = d;
        BG = d ? c(0x0B1020) : c(0xF3F4F6); CARD = d ? c(0x151B2E) : Color.WHITE; TEXT = d ? c(0xE5E7EB) : c(0x111827);
        MUTED = d ? c(0x94A3B8) : c(0x6B7280); LINE = d ? c(0x273049) : c(0xE5E7EB); SOFT = d ? c(0x1E2547) : c(0xEEF2FF);
        FIELD_BG = d ? c(0x0F1527) : Color.WHITE; FIELD_LINE = d ? c(0x334155) : c(0xD1D5DB); CHIP = d ? c(0x1E2640) : c(0xF3F4F6);
        OK_BG = d ? c(0x0F3D33) : c(0xD1FAE5); BADGE_BG = d ? c(0x2A2F63) : c(0xE0E7FF); ROW_SEL = d ? c(0x25305A) : c(0xE0E7FF);
        HEAD_BG = d ? c(0x1A2138) : c(0xF9FAFB); BUS_FILL = d ? c(0x10162A) : c(0xF9FAFB); BUS_EDGE = d ? c(0x334155) : c(0xD1D5DB);
        SEAT_FREE = d ? c(0x0F3D33) : c(0xECFDF5); SEAT_FREE_EDGE = d ? c(0x10B981) : c(0x34D399);
        SEAT_TAKEN = d ? c(0x2B3350) : c(0xE5E7EB); SEAT_TAKEN_EDGE = d ? c(0x3B4466) : c(0xD1D5DB); SEAT_TAKEN_TEXT = d ? c(0x64748B) : c(0x9CA3AF);
        SEAT_LADY = d ? c(0x4A1D3A) : c(0xFCE7F3); SEAT_LADY_EDGE = d ? c(0xF472B6) : c(0xF472B6);
        UIManager.put("Panel.background", CARD); UIManager.put("OptionPane.background", CARD); UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("Label.foreground", TEXT); UIManager.put("Viewport.background", CARD); UIManager.put("ScrollPane.background", CARD);
        for (String k : new String[]{"TextField", "PasswordField", "TextArea", "ComboBox", "List", "Table"}) {
            UIManager.put(k + ".background", FIELD_BG); UIManager.put(k + ".foreground", TEXT);
        }
        UIManager.put("TextField.caretForeground", TEXT); UIManager.put("PasswordField.caretForeground", TEXT); UIManager.put("TextArea.caretForeground", TEXT);
        UIManager.put("ComboBox.selectionBackground", ROW_SEL); UIManager.put("ComboBox.selectionForeground", TEXT);
        UIManager.put("List.selectionBackground", ROW_SEL); UIManager.put("List.selectionForeground", TEXT);
        UIManager.put("ToolTip.background", d ? c(0x1E2640) : c(0xFFFFE1)); UIManager.put("ToolTip.foreground", TEXT);
    }

    static Font font(int style, float size) { return new Font("Segoe UI", style, Math.round(size)); }
    static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
    static String money(double v) { return RS + String.format("%,.2f", v); }
    static Border pad(int t, int l, int b, int r) { return BorderFactory.createEmptyBorder(t, l, b, r); }
    static Component vgap(int h) { return Box.createVerticalStrut(h); }
    static <T extends JComponent> T left(T c) { c.setAlignmentX(Component.LEFT_ALIGNMENT); return c; }

    static JLabel label(String t, int style, float size, Color c) { JLabel l = new JLabel(t); l.setFont(font(style, size)); l.setForeground(c); return l; }
    static Border fieldBorder() { return BorderFactory.createCompoundBorder(new LineBorder(FIELD_LINE, 1, true), pad(7, 10, 7, 10)); }
    static JTextField field(int cols) {
        JTextField f = new JTextField(cols); f.setFont(font(Font.PLAIN, 13)); f.setBorder(fieldBorder());
        f.setBackground(FIELD_BG); f.setForeground(TEXT); f.setCaretColor(TEXT); return f;
    }
    static JTextArea area(int rows, int cols) {
        JTextArea a = new JTextArea(rows, cols); a.setFont(font(Font.PLAIN, 13)); a.setBorder(fieldBorder()); a.setLineWrap(true); a.setWrapStyleWord(true);
        a.setBackground(FIELD_BG); a.setForeground(TEXT); a.setCaretColor(TEXT); return a;
    }
    static void style(JComboBox<?> c) {
        c.setFont(font(Font.PLAIN, 13)); c.setBackground(FIELD_BG); c.setForeground(TEXT); c.setPreferredSize(new Dimension(190, 36)); c.setMaximumSize(new Dimension(190, 36));
    }
    static JPanel labeled(String t, JComponent c) {
        JPanel p = new JPanel(); p.setOpaque(false); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(left(label(t, Font.BOLD, 11, MUTED))); p.add(vgap(5)); p.add(left(c));
        return p;
    }
    static JScrollPane scroll(JScrollPane sp) {
        sp.setBorder(null); sp.getVerticalScrollBar().setUI(new SlimBar()); sp.getHorizontalScrollBar().setUI(new SlimBar());
        sp.getVerticalScrollBar().setOpaque(false); sp.getHorizontalScrollBar().setOpaque(false); sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    static void styleTable(JTable t) {
        t.setRowHeight(38); t.setFont(font(Font.PLAIN, 13)); t.setShowVerticalLines(false); t.setGridColor(LINE);
        t.setSelectionBackground(ROW_SEL); t.setSelectionForeground(TEXT); t.setBackground(CARD); t.setIntercellSpacing(new Dimension(0, 1)); t.setFillsViewportHeight(true);
        t.setDefaultRenderer(Object.class, new Cell());
        JTableHeader h = t.getTableHeader(); h.setReorderingAllowed(false); h.setPreferredSize(new Dimension(100, 38));
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(tb, v, false, false, r, c);
                setBackground(HEAD_BG); setForeground(MUTED); setFont(font(Font.BOLD, 12));
                setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LINE), pad(0, 10, 0, 10)));
                return this;
            }
        });
    }
    static void widths(JTable t, int... w) { for (int i = 0; i < w.length && i < t.getColumnCount(); i++) t.getColumnModel().getColumn(i).setPreferredWidth(w[i]); }
    static JScrollPane tableScroll(JTable t) { JScrollPane sp = scroll(new JScrollPane(t)); sp.getViewport().setBackground(CARD); return sp; }

    /** Pill tab group; calls pick with the index of the clicked pill. */
    static JPanel pills(String[] labels, Consumer<Integer> pick) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); p.setOpaque(false);
        java.util.List<Btn> bs = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            Btn b = new Btn(labels[i], LINE, FIELD_LINE); b.fg = TEXT;
            b.addActionListener(e -> {
                for (int j = 0; j < bs.size(); j++) {
                    Btn x = bs.get(j); boolean on = j == idx;
                    x.base = on ? PRIMARY : LINE; x.hover = on ? PRIMARY_DARK : FIELD_LINE; x.fg = on ? Color.WHITE : TEXT; x.repaint();
                }
                pick.accept(idx);
            });
            bs.add(b); p.add(b);
        }
        bs.get(0).doClick();
        return p;
    }

    static JPanel formCard(String title, String sub, JComponent... items) {
        Card c = new Card(); c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.add(left(label(title, Font.BOLD, 18, TEXT))); c.add(vgap(2)); c.add(left(label(sub, Font.PLAIN, 13, MUTED))); c.add(vgap(18));
        for (JComponent it : items) {
            if (it instanceof Btn) it.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            c.add(left(it)); c.add(vgap(14));
        }
        c.setPreferredSize(new Dimension(520, c.getPreferredSize().height));
        JPanel w = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); w.setOpaque(false); w.add(c);
        return w;
    }

    // ================= the futuristic bus =================
    static void drawBus(Graphics2D g, int x, int y, int w, boolean mono) {
        AffineTransform old = g.getTransform(); Shape oldClip = g.getClip();
        g.translate(x, y); double s = w;
        if (!mono) {   // speed lines
            for (int k = 0; k < 3; k++) {
                g.setStroke(new BasicStroke((float) (0.014 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(34, 211, 238, 170 - k * 45));
                g.draw(new Line2D.Double((0.01 + k * 0.015) * s, (0.12 + 0.075 * k) * s, (0.10 - k * 0.012) * s, (0.12 + 0.075 * k) * s));
            }
        }
        g.translate(0.10 * s, 0); g.scale(0.90 * s, 0.90 * s);
        if (!mono) { g.setColor(new Color(34, 211, 238, 55)); g.fill(new Ellipse2D.Double(0.06, 0.40, 0.88, 0.07)); }   // underglow
        Path2D body = new Path2D.Double();
        body.moveTo(0.03, 0.38); body.lineTo(0.03, 0.14); body.quadTo(0.03, 0.03, 0.14, 0.03); body.lineTo(0.64, 0.03);
        body.curveTo(0.82, 0.03, 0.94, 0.17, 0.98, 0.32); body.quadTo(1.0, 0.38, 0.93, 0.38); body.closePath();
        g.setPaint(new GradientPaint(0f, 0f, mono ? c(0xE5E7EB) : VIOLET, 1f, 0.4f, mono ? c(0xCBD5E1) : NEON)); g.fill(body);
        g.setStroke(new BasicStroke(0.008f)); g.setColor(new Color(255, 255, 255, 110)); g.draw(body);
        Path2D win = new Path2D.Double();
        win.moveTo(0.08, 0.09); win.lineTo(0.63, 0.09); win.curveTo(0.76, 0.09, 0.86, 0.19, 0.90, 0.26); win.lineTo(0.08, 0.26); win.closePath();
        g.setColor(mono ? Color.WHITE : new Color(8, 12, 30, 225)); g.fill(win);
        g.clip(win);
        g.setColor(new Color(255, 255, 255, mono ? 0 : 55)); Path2D refl = new Path2D.Double();
        refl.moveTo(0.14, 0.09); refl.lineTo(0.27, 0.09); refl.lineTo(0.17, 0.26); refl.lineTo(0.04, 0.26); refl.closePath(); g.fill(refl);
        refl = new Path2D.Double(); refl.moveTo(0.34, 0.09); refl.lineTo(0.38, 0.09); refl.lineTo(0.28, 0.26); refl.lineTo(0.24, 0.26); refl.closePath(); g.fill(refl);
        g.setClip(oldClip == null ? null : oldClip);
        g.setClip(null);
        g.setStroke(new BasicStroke(0.012f)); g.setColor(mono ? c(0xCBD5E1) : new Color(99, 102, 241));
        for (double lx : new double[]{0.23, 0.38, 0.53}) g.draw(new Line2D.Double(lx, 0.09, lx, 0.26));
        g.setColor(mono ? c(0xCBD5E1) : new Color(255, 255, 255, 230)); g.setStroke(new BasicStroke(0.014f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(0.07, 0.325, 0.90, 0.325));
        g.setColor(mono ? c(0xCBD5E1) : NEON); g.fill(new RoundRectangle2D.Double(0.30, 0.012, 0.22, 0.016, 0.01, 0.01));   // roof light bar
        if (!mono) {   // headlight with glow
            g.setPaint(new RadialGradientPaint(new Point2D.Double(0.975, 0.285), 0.11f, new float[]{0f, 1f}, new Color[]{new Color(255, 255, 255, 200), new Color(34, 211, 238, 0)}));
            g.fill(new Ellipse2D.Double(0.86, 0.17, 0.23, 0.23));
            g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(0.945, 0.265, 0.04, 0.045));
        }
        for (double cx : new double[]{0.25, 0.76}) {   // wheels
            g.setColor(c(0x0B1020)); g.fill(new Ellipse2D.Double(cx - 0.078, 0.302, 0.156, 0.156));
            g.setColor(mono ? c(0x94A3B8) : c(0x111827)); g.fill(new Ellipse2D.Double(cx - 0.058, 0.322, 0.116, 0.116));
            g.setColor(mono ? c(0xE5E7EB) : NEON); g.setStroke(new BasicStroke(0.012f)); g.draw(new Ellipse2D.Double(cx - 0.034, 0.346, 0.068, 0.068));
            g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(cx - 0.011, 0.368, 0.022, 0.022));
        }
        g.setTransform(old); g.setClip(oldClip);
    }

    static class BusIcon extends JComponent {
        final int w; final boolean mono;
        BusIcon(int w) { this(w, false); }
        BusIcon(int w, boolean mono) { this.w = w; this.mono = mono; setPreferredSize(new Dimension(w, (int) (w * 0.5))); }
        protected void paintComponent(Graphics g0) { Graphics2D g = (Graphics2D) g0.create(); aa(g); drawBus(g, 0, 0, w, mono); g.dispose(); }
    }

    // ================= components =================
    static class Btn extends JButton {
        Color base, hover, fg = Color.WHITE; boolean over;
        Btn(String t, Color base, Color hover) {
            super(t); this.base = base; this.hover = hover;
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setOpaque(false);
            setFont(font(Font.BOLD, 13)); setBorder(pad(9, 18, 9, 18)); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { over = true; repaint(); }
                public void mouseExited(MouseEvent e) { over = false; repaint(); }
            });
        }
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); aa(g);
            g.setColor(!isEnabled() ? FIELD_LINE : over ? hover : base);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g.setColor(isEnabled() ? fg : Color.WHITE); g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            g.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }

    static class Card extends JPanel {
        Card() { super(new BorderLayout()); setOpaque(false); setBorder(pad(16, 18, 20, 18)); }
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); aa(g);
            g.setColor(new Color(0, 0, 0, dark ? 60 : 16)); g.fillRoundRect(1, 3, getWidth() - 2, getHeight() - 3, 20, 20);
            g.setColor(CARD); g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 3, 20, 20);
            if (dark) { g.setColor(LINE); g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 4, 20, 20); }
            g.dispose();
        }
    }

    static class Grad extends JPanel {
        final Color a, b; boolean decor;
        Grad(LayoutManager lm, Color a, Color b) { super(lm); this.a = a; this.b = b; }
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); aa(g); int w = getWidth(), h = getHeight();
            g.setPaint(new GradientPaint(0, 0, a, w, h, b)); g.fillRect(0, 0, w, h);
            if (decor) {   // futuristic grid + glow rings
                g.setColor(new Color(255, 255, 255, 14)); g.setStroke(new BasicStroke(1f));
                for (int x = 0; x < w; x += 40) g.drawLine(x, h / 2, (x - w / 2) * 3 + w / 2, h);
                for (int k = 0; k < 6; k++) { int y = h / 2 + (int) (Math.pow(k / 6.0, 1.8) * h / 2); g.drawLine(0, y, w, y); }
                g.setPaint(new RadialGradientPaint(new Point2D.Double(w * 0.8, h * 0.18), w * 0.55f, new float[]{0f, 1f}, new Color[]{new Color(34, 211, 238, 70), new Color(34, 211, 238, 0)}));
                g.fillRect(0, 0, w, h);
            }
            g.dispose();
        }
    }

    static class Badge extends JLabel {
        final Color bg;
        Badge(String t, Color fg, Color bg) { super(t, SwingConstants.CENTER); this.bg = bg; setForeground(fg); setFont(font(Font.BOLD, 11)); setOpaque(false); setBorder(pad(3, 10, 3, 10)); }
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); aa(g);
            g.setColor(bg); g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight()); g.dispose();
            super.paintComponent(g0);
        }
    }

    static class Tile extends JPanel {
        boolean sel;
        Tile(LayoutManager lm) { super(lm); setOpaque(false); setBorder(pad(12, 14, 12, 14)); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); aa(g);
            g.setColor(sel ? SOFT : CARD); g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 18, 18);
            g.setColor(sel ? PRIMARY : LINE); g.setStroke(new BasicStroke(sel ? 2f : 1f)); g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 18, 18);
            g.dispose();
        }
    }

    /** Seat: state 0 free, 1 selected, 2 booked. pos = column in row (0,3 = window). ladies = women-only row. */
    static class Seat extends JButton {
        int state; final boolean ladies; final int pos;
        Seat(int n, int state, boolean ladies, int pos) {
            super(String.valueOf(n)); this.state = state; this.ladies = ladies; this.pos = pos;
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setOpaque(false);
            setPreferredSize(new Dimension(52, 46)); setFont(font(Font.BOLD, 12));
            setToolTipText("Seat " + n + "  |  " + (pos == 0 || pos == 3 ? "Window" : "Aisle") + (ladies ? "  |  Ladies (women only)" : "") + (state == 2 ? "  |  Booked" : ""));
            if (state != 2) setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        void set(int s) { state = s; repaint(); }
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); aa(g);
            int w = getWidth(), h = getHeight();
            Color fill = state == 1 ? PRIMARY : state == 2 ? SEAT_TAKEN : ladies ? SEAT_LADY : SEAT_FREE;
            Color edge = state == 1 ? PRIMARY_DARK : state == 2 ? SEAT_TAKEN_EDGE : ladies ? SEAT_LADY_EDGE : SEAT_FREE_EDGE;
            g.setColor(fill); g.fillRoundRect(2, 7, w - 5, h - 9, 12, 12); g.fillRoundRect(w / 2 - 12, 1, 24, 9, 6, 6);
            g.setColor(edge); g.setStroke(new BasicStroke(1.5f)); g.drawRoundRect(2, 7, w - 5, h - 9, 12, 12);
            if (pos == 0 || pos == 3) {   // window edge marker
                g.setColor(state == 2 ? SEAT_TAKEN_EDGE : WINDOW); g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int bx = pos == 0 ? 5 : w - 6; g.drawLine(bx, 17, bx, h - 6);
            }
            g.setColor(state == 1 ? Color.WHITE : state == 2 ? SEAT_TAKEN_TEXT : TEXT); g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            g.drawString(getText(), (w - fm.stringWidth(getText())) / 2, 7 + (h - 9 - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }

    static class Cell extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            boolean signed = t.getClientProperty("signed") != null && v instanceof Double;
            String text = signed ? ((Double) v >= 0 ? "+ " : "- ") + RS + String.format("%,.2f", Math.abs((Double) v))
                    : v instanceof Double ? RS + String.format("%,.2f", (Double) v) : String.valueOf(v);
            super.getTableCellRendererComponent(t, text, sel, false, r, c);
            setBorder(pad(0, 10, 0, 10)); setFont(font(Font.PLAIN, 13)); setForeground(TEXT);
            if (!sel) setBackground(CARD);
            if (signed) { setForeground((Double) v >= 0 ? OK : BAD); setFont(font(Font.BOLD, 13)); }
            if ("CONFIRMED".equals(text) || "ACTIVE".equals(text)) { setForeground(OK); setFont(font(Font.BOLD, 12)); }
            if ("CANCELLED".equals(text)) { setForeground(BAD); setFont(font(Font.BOLD, 12)); }
            return this;
        }
    }

    static class Dashed extends JComponent {
        Dashed() { setPreferredSize(new Dimension(10, 14)); }
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); g.setColor(FIELD_LINE);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 5f}, 0f));
            g.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2); g.dispose();
        }
    }

    /** Slim rounded scrollbar without arrow buttons. */
    static class SlimBar extends BasicScrollBarUI {
        protected JButton createDecreaseButton(int o) { return none(); }
        protected JButton createIncreaseButton(int o) { return none(); }
        JButton none() { JButton b = new JButton(); Dimension d = new Dimension(0, 0); b.setPreferredSize(d); b.setMinimumSize(d); b.setMaximumSize(d); return b; }
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }
        protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
            if (r.isEmpty()) return;
            Graphics2D g = (Graphics2D) g0.create(); aa(g); g.setColor(isThumbRollover() || isDragging ? PRIMARY : (dark ? c(0x475569) : c(0xC7CBD4)));
            if (scrollbar.getOrientation() == Adjustable.VERTICAL) g.fillRoundRect(r.x + 3, r.y + 2, r.width - 6, r.height - 4, 8, 8);
            else g.fillRoundRect(r.x + 2, r.y + 3, r.width - 4, r.height - 6, 8, 8);
            g.dispose();
        }
        public Dimension getPreferredSize(JComponent c) { return new Dimension(12, 12); }
    }

    /** Star rating. Read-only by default; editable() makes the stars clickable. */
    static class Stars extends JComponent {
        double rating; final int size; Consumer<Integer> onPick;
        Stars(double rating, int size) { this.rating = rating; this.size = size; setPreferredSize(new Dimension(5 * (size + 3), size + 2)); }
        Stars editable(Consumer<Integer> pick) {
            onPick = pick; setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { rating = Math.max(1, Math.min(5, e.getX() / (size + 3) + 1)); repaint(); onPick.accept((int) rating); }
            });
            return this;
        }
        static Path2D star(double cx, double cy, double R) {
            Path2D p = new Path2D.Double();
            for (int i = 0; i < 10; i++) {
                double a = -Math.PI / 2 + i * Math.PI / 5, r = i % 2 == 0 ? R : R * 0.45, x = cx + r * Math.cos(a), y = cy + r * Math.sin(a);
                if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
            }
            p.closePath(); return p;
        }
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); aa(g);
            for (int i = 0; i < 5; i++) {
                double cx = i * (size + 3) + size / 2.0 + 1, cy = size / 2.0 + 1; Path2D s = star(cx, cy, size / 2.0);
                g.setColor(dark ? c(0x334155) : c(0xD1D5DB)); g.fill(s);
                double fill = Math.max(0, Math.min(1, rating - i));
                if (fill > 0) { Shape old = g.getClip(); g.clip(new Rectangle2D.Double(cx - size / 2.0 - 1, 0, size * fill + 1, size + 4)); g.setColor(ACCENT); g.fill(s); g.setClip(old); }
            }
            g.dispose();
        }
    }

    /** Animated payment status: mode 0 spinner, 1 success tick, 2 failure cross. */
    static class StatusIcon extends JComponent {
        int mode; double t; final javax.swing.Timer timer;
        StatusIcon() { setPreferredSize(new Dimension(120, 120)); timer = new javax.swing.Timer(20, e -> { t += 0.02; repaint(); }); }
        void mode(int m) { mode = m; t = 0; if (!timer.isRunning()) timer.start(); }
        void stop() { timer.stop(); }
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); aa(g);
            int cx = getWidth() / 2, cy = getHeight() / 2, r = 44; g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (mode == 0) {
                g.setColor(SOFT); g.drawOval(cx - r, cy - r, 2 * r, 2 * r);
                g.setPaint(new GradientPaint(cx - r, cy, PRIMARY, cx + r, cy, NEON)); g.draw(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, -t * 600, 110, Arc2D.OPEN));
            } else {
                Color col = mode == 1 ? OK : BAD; double p = Math.min(1, t * 1.6), q = Math.max(0, Math.min(1, (t - 0.55) * 3));
                g.setColor(col); g.draw(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, 90, -360 * p, Arc2D.OPEN));
                if (mode == 1) {
                    double ax = cx - 20, ay = cy + 2, bx = cx - 6, by = cy + 16, ex = cx + 22, ey = cy - 14;
                    if (q > 0) g.draw(new Line2D.Double(ax, ay, ax + (bx - ax) * Math.min(1, q * 2), ay + (by - ay) * Math.min(1, q * 2)));
                    if (q > 0.5) { double k = (q - 0.5) * 2; g.draw(new Line2D.Double(bx, by, bx + (ex - bx) * k, by + (ey - by) * k)); }
                } else {
                    if (q > 0) g.draw(new Line2D.Double(cx - 16, cy - 16, cx - 16 + 32 * Math.min(1, q * 2), cy - 16 + 32 * Math.min(1, q * 2)));
                    if (q > 0.5) { double k = (q - 0.5) * 2; g.draw(new Line2D.Double(cx + 16, cy - 16, cx + 16 - 32 * k, cy - 16 + 32 * k)); }
                }
            }
            g.dispose();
        }
    }

    /** Horizontal bar chart: rows are {label, seatsSold, revenue(Double)}. */
    static class Bars extends JPanel {
        java.util.List<Object[]> rows = new ArrayList<>();
        Bars() { setOpaque(false); setPreferredSize(new Dimension(100, 140)); }
        void set(java.util.List<Object[]> r) { rows = r; setPreferredSize(new Dimension(100, Math.max(140, r.size() * 54 + 10))); revalidate(); repaint(); }
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); aa(g);
            if (rows.isEmpty()) { g.setFont(font(Font.PLAIN, 14)); g.setColor(MUTED); g.drawString("No bookings yet - revenue will appear here once tickets are sold.", 8, 30); g.dispose(); return; }
            double max = 1; for (Object[] r : rows) max = Math.max(max, Db.D(r[2]));
            int labelW = 190, valW = 200, bw = Math.max(60, getWidth() - labelW - valW);
            for (int i = 0; i < rows.size(); i++) {
                Object[] r = rows.get(i); int y = i * 54 + 8; double v = Db.D(r[2]);
                g.setFont(font(Font.BOLD, 13)); g.setColor(TEXT); g.drawString((String) r[0], 4, y + 23);
                g.setColor(SOFT); g.fillRoundRect(labelW, y + 6, bw, 22, 22, 22);
                int w = Math.max(22, (int) (bw * v / max));
                g.setPaint(new GradientPaint(labelW, 0, VIOLET, labelW + bw, 0, NEON)); g.fillRoundRect(labelW, y + 6, w, 22, 22, 22);
                g.setFont(font(Font.PLAIN, 13)); g.setColor(MUTED);
                g.drawString(RS + String.format("%,.0f", v) + DOT + r[1] + " seats", labelW + bw + 12, y + 23);
            }
            g.dispose();
        }
    }
}
