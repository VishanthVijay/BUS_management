import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

public class BusApp extends JFrame {
    static final int MAX_SEATS = 6, B = Font.BOLD, P = Font.PLAIN;

    final int userId; final String username, role;
    String page = "book";
    final CardLayout pages = new CardLayout(); final JPanel pageHost = new JPanel(pages);
    final List<Ui.Btn> navBtns = new ArrayList<>();
    Ui.Btn inboxBtn; final JLabel walletChip = L("", B, 12, Color.WHITE);
    AdminPages admin;

    // ---- booking state ----
    final JComboBox<String> from = new JComboBox<>(), to = new JComboBox<>();
    final List<Ui.Btn> dayBtns = new ArrayList<>();
    final List<Ui.Tile> tiles = new ArrayList<>();
    final JPanel busList = new JPanel(), seatGrid = new JPanel(new GridLayout(0, 5, 8, 8)), chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
    final CardLayout seatCards = new CardLayout(); final JPanel seatHost = new JPanel(seatCards);
    final JLabel found = L("", P, 12, Ui.MUTED), sumRoute = L("No bus selected", B, 16, Ui.TEXT), sumWhen = L(" ", P, 13, Ui.MUTED), sumBus = L(" ", P, 13, Ui.MUTED),
            fareName = L("Fare", P, 13, Ui.MUTED), fareVal = L("", B, 13, Ui.TEXT), discName = L("Discount", P, 13, Ui.OK), discVal = L("", B, 13, Ui.OK),
            totalVal = L(Ui.money(0), B, 22, Ui.PRIMARY), couponMsg = L(" ", P, 12, Ui.MUTED);
    final JPanel discRow = new JPanel(new BorderLayout());
    final JTextField coupon = Ui.field(8);
    final Ui.Btn bookBtn = new Ui.Btn("Continue to payment", Ui.ACCENT, Ui.ACCENT_DARK);
    final Set<Integer> selected = new TreeSet<>();
    LocalDate selDate = LocalDate.now(); boolean loading = true;
    int currentTrip = -1, currentBusId = -1, couponPct = 0; double currentFare;
    String curRoute = "", curWhen = "", curBus = "", curFrom = "", curTo = "", curBusName = "";

    // ---- my bookings / wallet / inbox ----
    final DefaultTableModel myModel = new DefaultTableModel(new String[]{"ID", "Ref", "Route", "Date", "Time", "Seat", "Passenger", "Paid", "Status", "Refund"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    final JTable myTable = new JTable(myModel);
    final CardLayout myCards = new CardLayout(); final JPanel myHost = new JPanel(myCards);
    final JTextField findField = Ui.field(14); final JLabel findMsg = L(" ", P, 12, Ui.MUTED);
    final DefaultTableModel txModel = new DefaultTableModel(new String[]{"Date", "Description", "Amount"}, 0) { public boolean isCellEditable(int r, int c) { return false; } };
    final JTable txTable = new JTable(txModel);
    final JLabel balanceBig = L(Ui.money(0), B, 40, Color.WHITE);
    final JTextField topAmount = Ui.field(8);
    final JPanel inboxList = new JPanel();

    static JLabel L(String t, int st, float sz, Color c) { return Ui.label(t, st, sz, c); }

    BusApp(int userId, String username, String role) {
        this.userId = userId; this.username = username; this.role = role;
        setTitle("BusGo - Bus Ticket Booking"); setSize(1340, 840); setMinimumSize(new Dimension(1220, 720));
        setLocationRelativeTo(null); setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImage(icon());
        JPanel root = new JPanel(new BorderLayout()); root.setBackground(Ui.BG);
        JPanel top = new JPanel(new BorderLayout()); top.add(header(), BorderLayout.CENTER);
        Ui.Grad strip = new Ui.Grad(null, Ui.VIOLET, Ui.NEON); strip.setPreferredSize(new Dimension(10, 3)); top.add(strip, BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);
        pageHost.setOpaque(false);
        pageHost.add(bookPage(), "book"); pageHost.add(myPage(), "mine"); pageHost.add(walletPage(), "wallet"); pageHost.add(inboxPage(), "inbox");
        if (role.equals("ADMIN")) { admin = new AdminPages(this); pageHost.add(admin.page(), "admin"); }
        root.add(pageHost, BorderLayout.CENTER);
        setContentPane(root);
        goTo("book");
    }

    static Image icon() {
        java.awt.image.BufferedImage im = new java.awt.image.BufferedImage(64, 64, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = im.createGraphics(); Ui.aa(g); Ui.drawBus(g, 0, 14, 64, false); g.dispose(); return im;
    }

    // ================= header / navigation =================
    JPanel header() {
        Ui.Grad h = new Ui.Grad(new BorderLayout(), Ui.DEEP, Ui.PRIMARY_DARK); h.decor = true;
        h.setBorder(Ui.pad(8, 22, 8, 22));
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8)); brand.setOpaque(false);
        brand.add(new Ui.BusIcon(78)); brand.add(L("BusGo", B, 26, Color.WHITE));
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 12)); nav.setOpaque(false);
        addNav(nav, "Book Ticket", "book"); addNav(nav, "My Bookings", "mine"); addNav(nav, "Wallet", "wallet"); inboxBtn = addNav(nav, "Inbox", "inbox");
        if (role.equals("ADMIN")) addNav(nav, "Admin", "admin");
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12)); right.setOpaque(false);
        walletChip.setBorder(Ui.pad(5, 12, 5, 12)); walletChip.setOpaque(false);
        JPanel chip = new JPanel(new BorderLayout()) { protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); Ui.aa(g); g.setColor(new Color(34, 211, 238, 60)); g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight()); g.dispose(); } };
        chip.setOpaque(false); chip.add(walletChip);
        right.add(chip);
        right.add(new Ui.Badge(username + Ui.DOT + role, Color.WHITE, new Color(255, 255, 255, 45)));
        Ui.Btn theme = new Ui.Btn(Ui.dark ? "Light mode" : "Dark mode", new Color(255, 255, 255, 25), new Color(255, 255, 255, 60));
        theme.addActionListener(e -> toggleTheme());
        Ui.Btn out = new Ui.Btn("Logout", new Color(255, 255, 255, 25), new Color(255, 255, 255, 60));
        out.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
        right.add(theme); right.add(out);
        h.add(brand, BorderLayout.WEST); h.add(nav, BorderLayout.CENTER); h.add(right, BorderLayout.EAST);
        return h;
    }

    Ui.Btn addNav(JPanel nav, String text, String pg) {
        Ui.Btn b = new Ui.Btn(text, new Color(255, 255, 255, 0), new Color(255, 255, 255, 35));
        b.putClientProperty("page", pg); b.putClientProperty("label", text); b.setFont(Ui.font(B, 14));
        b.addActionListener(e -> goTo(pg));
        navBtns.add(b); nav.add(b); return b;
    }

    void goTo(String next) {
        if (page.equals("inbox") && !next.equals("inbox")) { try { Db.markAllRead(userId); } catch (Exception e) { } }
        page = next;
        for (Ui.Btn b : navBtns) { b.base = new Color(255, 255, 255, next.equals(b.getClientProperty("page")) ? 55 : 0); b.repaint(); }
        pages.show(pageHost, next);
        if (next.equals("mine")) loadMine();
        if (next.equals("wallet")) loadWallet();
        if (next.equals("inbox")) loadInbox();
        if (next.equals("admin") && admin != null) admin.onShow();
        refreshHeader();
    }

    void refreshHeader() {
        try {
            walletChip.setText("Wallet  " + Ui.money(Db.wallet(userId)));
            int n = Db.unread(userId);
            inboxBtn.setText(n > 0 ? "Inbox (" + n + ")" : "Inbox");
            inboxBtn.revalidate(); inboxBtn.getParent().revalidate(); inboxBtn.getParent().repaint();
        } catch (Exception e) { }
    }

    void toggleTheme() {
        Ui.applyTheme(!Ui.dark); saveSettings();
        BusApp n = new BusApp(userId, username, role); n.setBounds(getBounds()); n.goTo(page); n.setVisible(true); dispose();
    }

    static void loadSettings() {
        try (InputStream in = Files.newInputStream(Paths.get("busgo.properties"))) { Properties p = new Properties(); p.load(in); Ui.applyTheme("dark".equals(p.getProperty("theme"))); } catch (Exception e) { }
    }
    static void saveSettings() {
        Properties p = new Properties(); p.setProperty("theme", Ui.dark ? "dark" : "light");
        try (OutputStream o = Files.newOutputStream(Paths.get("busgo.properties"))) { p.store(o, "BusGo settings"); } catch (Exception e) { }
    }

    // ================= BOOK PAGE =================
    JPanel bookPage() {
        JPanel p = new JPanel(new BorderLayout(0, 16)); p.setBackground(Ui.BG); p.setBorder(Ui.pad(18, 22, 18, 22));
        p.add(searchCard(), BorderLayout.NORTH);
        JPanel mid = new JPanel(new BorderLayout(16, 0)); mid.setOpaque(false);
        mid.add(busesColumn(), BorderLayout.WEST); mid.add(seatCard(), BorderLayout.CENTER); mid.add(summaryCard(), BorderLayout.EAST);
        p.add(mid, BorderLayout.CENTER);
        loading = false; search();
        return p;
    }

    static <T extends JComponent> T fixed(T c) { c.setMaximumSize(c.getPreferredSize()); return c; }

    JPanel xrow() { JPanel r = new JPanel(); r.setOpaque(false); r.setLayout(new BoxLayout(r, BoxLayout.X_AXIS)); return r; }

    Ui.Card searchCard() {
        try {
            for (String c : Db.cities()) { from.addItem(c); to.addItem(c); }
            from.setSelectedItem("Chennai"); to.setSelectedItem("Bengaluru");
        } catch (Exception e) { err(e); }
        Ui.style(from); Ui.style(to);
        from.addActionListener(e -> { if (!loading) search(); }); to.addActionListener(e -> { if (!loading) search(); });
        Ui.Btn swap = new Ui.Btn("Swap", Ui.LINE, Ui.FIELD_LINE); swap.fg = Ui.TEXT;
        swap.addActionListener(e -> { loading = true; Object a = from.getSelectedItem(); from.setSelectedItem(to.getSelectedItem()); to.setSelectedItem(a); loading = false; search(); });
        Ui.Btn go = new Ui.Btn("Search buses", Ui.PRIMARY, Ui.PRIMARY_DARK); go.addActionListener(e -> search());
        JPanel row1 = xrow();
        row1.add(fixed(Ui.labeled("FROM", from))); row1.add(Box.createHorizontalStrut(12));
        JPanel sw = new JPanel(new BorderLayout()); sw.setOpaque(false); sw.setBorder(Ui.pad(18, 0, 0, 0)); sw.add(swap); sw.setMaximumSize(sw.getPreferredSize()); row1.add(sw); row1.add(Box.createHorizontalStrut(12));
        row1.add(fixed(Ui.labeled("TO", to))); row1.add(Box.createHorizontalStrut(14));
        JPanel gp = new JPanel(new BorderLayout()); gp.setOpaque(false); gp.setBorder(Ui.pad(18, 0, 0, 0)); gp.add(go); gp.setMaximumSize(gp.getPreferredSize()); row1.add(gp); row1.add(Box.createHorizontalGlue());

        JPanel days = xrow(); DateTimeFormatter f = DateTimeFormatter.ofPattern("EEE, d MMM");
        for (int i = 0; i < 7; i++) {
            LocalDate d = LocalDate.now().plusDays(i);
            Ui.Btn b = new Ui.Btn(i == 0 ? "Today" : i == 1 ? "Tomorrow" : d.format(f), Ui.CHIP, Ui.LINE);
            b.setFont(Ui.font(B, 12)); b.setBorder(Ui.pad(7, 14, 7, 14)); b.putClientProperty("d", d);
            b.addActionListener(e -> { selDate = d; styleDays(); search(); });
            dayBtns.add(b); days.add(b); days.add(Box.createHorizontalStrut(8));
        }
        days.add(Box.createHorizontalGlue()); styleDays();
        Ui.Card c = new Ui.Card(); c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.add(Ui.left(row1)); c.add(Ui.vgap(14)); c.add(Ui.left(L("TRAVEL DATE", B, 11, Ui.MUTED))); c.add(Ui.vgap(6)); c.add(Ui.left(days));
        return c;
    }

    void styleDays() {
        for (Ui.Btn b : dayBtns) {
            boolean on = selDate.equals(b.getClientProperty("d"));
            b.base = on ? Ui.PRIMARY : Ui.CHIP; b.hover = on ? Ui.PRIMARY_DARK : Ui.LINE; b.fg = on ? Color.WHITE : Ui.TEXT; b.repaint();
        }
    }

    JPanel busesColumn() {
        JPanel col = new JPanel(new BorderLayout(0, 10)); col.setOpaque(false); col.setPreferredSize(new Dimension(360, 10));
        JPanel head = new JPanel(new BorderLayout()); head.setOpaque(false);
        head.add(L("Available buses", B, 17, Ui.TEXT), BorderLayout.WEST); head.add(found, BorderLayout.EAST);
        busList.setOpaque(false); busList.setLayout(new BoxLayout(busList, BoxLayout.Y_AXIS));
        JPanel hold = new JPanel(new BorderLayout()); hold.setOpaque(false); hold.add(busList, BorderLayout.NORTH);
        JScrollPane sp = Ui.scroll(new JScrollPane(hold)); sp.setOpaque(false); sp.getViewport().setOpaque(false); sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        col.add(head, BorderLayout.NORTH); col.add(sp, BorderLayout.CENTER);
        return col;
    }

    void search() {
        if (from.getSelectedItem() == null || to.getSelectedItem() == null) return;
        try {
            busList.removeAll(); tiles.clear(); selected.clear(); couponPct = 0; currentTrip = -1; currentBusId = -1; coupon.setText(""); couponMsg.setText(" ");
            seatCards.show(seatHost, "empty");
            List<Object[]> rows = from.getSelectedItem().equals(to.getSelectedItem()) ? new ArrayList<>()
                    : Db.searchTrips((String) from.getSelectedItem(), (String) to.getSelectedItem(), selDate.toString());
            found.setText(rows.size() + (rows.size() == 1 ? " bus" : " buses"));
            if (rows.isEmpty()) {
                busList.add(Ui.left(L("No buses on this route/date.", B, 14, Ui.MUTED)));
                busList.add(Ui.left(L("Try another day or different cities.", P, 12, Ui.MUTED)));
            }
            for (Object[] r : rows) { Ui.Tile t = busTile(r); tiles.add(t); busList.add(t); busList.add(Ui.vgap(10)); }
            busList.revalidate(); busList.repaint(); refreshSummary();
        } catch (Exception e) { err(e); }
    }

    /** r: 0 tripId, 1 bus, 2 type, 3 depart, 4 fare, 5 left, 6 amenities, 7 busId, 8 avg, 9 count, 10 duration */
    Ui.Tile busTile(Object[] r) {
        int left = Db.I(r[5]), dur = Db.I(r[10]), count = Db.I(r[9]); String time = (String) r[3]; double avg = Db.D(r[8]);
        LocalTime dep = LocalTime.parse(time); long tm = dep.toSecondOfDay() / 60 + dur;
        String arr = dep.plusMinutes(dur) + (tm >= 1440 ? " +" + (tm / 1440) : "");
        boolean gone = selDate.isEqual(LocalDate.now()) && dep.isBefore(LocalTime.now()), blocked = gone || left == 0;
        Ui.Tile t = new Ui.Tile(new BorderLayout(14, 0));
        JPanel when = col(); when.add(Ui.left(L(time, B, 22, Ui.TEXT))); when.add(Ui.left(L("Arr " + arr, P, 11, Ui.MUTED))); when.add(Ui.left(L(dur / 60 + "h " + String.format("%02d", dur % 60) + "m", P, 11, Ui.MUTED)));
        JPanel mid = col(); mid.add(Ui.left(L((String) r[1], B, 14, Ui.TEXT))); mid.add(Ui.vgap(3));
        JPanel rate = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); rate.setOpaque(false);
        rate.add(new Ui.Stars(avg, 12)); rate.add(L(count == 0 ? "No reviews" : String.format("%.1f (%d)", avg, count), P, 11, Ui.MUTED));
        mid.add(Ui.left(rate)); mid.add(Ui.vgap(5));
        mid.add(Ui.left(new Ui.Badge((String) r[2], Ui.PRIMARY, Ui.BADGE_BG))); mid.add(Ui.vgap(4));
        String[] am = ((String) r[6]).split(", "); mid.add(Ui.left(L(am.length > 1 ? am[0] + Ui.DOT + am[1] : am[0], P, 11, Ui.MUTED)));
        JPanel end = col();
        JLabel fare = L(Ui.RS + String.format("%,.0f", Db.D(r[4])), B, 18, Ui.PRIMARY); fare.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JLabel seats = L(gone ? "Departed" : left == 0 ? "Sold out" : left + " seats left", B, 12, blocked ? Ui.BAD : left < 8 ? Ui.WARN : Ui.OK);
        seats.setAlignmentX(Component.RIGHT_ALIGNMENT);
        end.add(fare); end.add(Ui.vgap(4)); end.add(seats);
        t.add(when, BorderLayout.WEST); t.add(mid, BorderLayout.CENTER); t.add(end, BorderLayout.EAST);
        if (blocked) t.setCursor(Cursor.getDefaultCursor());
        else t.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) { for (Ui.Tile x : tiles) { x.sel = x == t; x.repaint(); } pickTrip(r); }
        });
        return t;
    }

    JPanel col() { JPanel p = new JPanel(); p.setOpaque(false); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); return p; }

    void pickTrip(Object[] r) {
        try {
            currentTrip = Db.I(r[0]); currentFare = Db.D(r[4]); currentBusId = Db.I(r[7]); curBusName = (String) r[1];
            curFrom = (String) from.getSelectedItem(); curTo = (String) to.getSelectedItem();
            curRoute = curFrom + Ui.ARROW + curTo;
            curWhen = selDate.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")) + Ui.DOT + r[3]; curBus = r[1] + " (" + r[2] + ")";
            selected.clear(); couponPct = 0; coupon.setText(""); couponMsg.setText(" ");
            int[] info = Db.busInfo(currentTrip); int total = info[0], ladiesRows = info[1];
            Set<Integer> booked = Db.bookedSeats(currentTrip);
            seatGrid.removeAll();
            for (int n = 1; n <= total; n++) {
                if ((n - 1) % 4 == 2) seatGrid.add(new JLabel(""));   // aisle
                Ui.Seat s = new Ui.Seat(n, booked.contains(n) ? 2 : 0, (n - 1) / 4 < ladiesRows, (n - 1) % 4); final int no = n;
                if (!booked.contains(n)) s.addActionListener(e -> {
                    if (selected.contains(no)) { selected.remove(Integer.valueOf(no)); s.set(0); }
                    else if (selected.size() >= MAX_SEATS) info("You can book up to " + MAX_SEATS + " seats at a time.");
                    else { selected.add(no); s.set(1); }
                    refreshSummary();
                });
                seatGrid.add(s);
            }
            seatGrid.revalidate(); seatGrid.repaint(); seatCards.show(seatHost, "seats"); refreshSummary();
        } catch (Exception e) { err(e); }
    }

    Ui.Card seatCard() {
        Ui.Card c = new Ui.Card(); c.setLayout(new BorderLayout(0, 12));
        JPanel head = col();
        JPanel t1 = new JPanel(new BorderLayout()); t1.setOpaque(false);
        t1.add(L("Choose your seats", B, 17, Ui.TEXT), BorderLayout.WEST);
        Ui.Btn rv = new Ui.Btn("Reviews", Ui.LINE, Ui.FIELD_LINE); rv.fg = Ui.TEXT; rv.setBorder(Ui.pad(5, 14, 5, 14)); rv.addActionListener(e -> showReviews());
        t1.add(rv, BorderLayout.EAST);
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2)); legend.setOpaque(false);
        legend.add(new Ui.Badge("Available", Ui.OK, Ui.OK_BG)); legend.add(new Ui.Badge("Selected", Color.WHITE, Ui.PRIMARY));
        legend.add(new Ui.Badge("Booked", Ui.SEAT_TAKEN_TEXT, Ui.SEAT_TAKEN)); legend.add(new Ui.Badge("Ladies", Ui.PINK, Ui.SEAT_LADY));
        legend.add(new Ui.Badge("Window edge", Ui.WINDOW, Ui.BADGE_BG));
        head.add(Ui.left(t1)); head.add(Ui.vgap(6)); head.add(Ui.left(legend));

        JPanel shape = new JPanel(new BorderLayout()) {
            { setOpaque(false); setBorder(Ui.pad(64, 30, 26, 30)); }
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create(); Ui.aa(g); int w = getWidth(), h = getHeight();
                g.setColor(Ui.BUS_FILL); g.fillRoundRect(0, 0, w - 1, h - 1, 46, 46);
                g.setColor(Ui.BUS_EDGE); g.setStroke(new BasicStroke(2f)); g.drawRoundRect(0, 0, w - 2, h - 2, 46, 46);
                g.setColor(Ui.MUTED); g.setStroke(new BasicStroke(3f)); g.drawOval(26, 20, 30, 30); g.fillOval(37, 31, 8, 8);
                g.setFont(Ui.font(B, 11)); g.drawString("DRIVER", 66, 40); g.drawString("FRONT", w / 2 - 18, 18);
                g.dispose();
            }
        };
        seatGrid.setOpaque(false);
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0)); wrap.setOpaque(false); wrap.add(seatGrid);
        JScrollPane sp = Ui.scroll(new JScrollPane(wrap)); sp.setOpaque(false); sp.getViewport().setOpaque(false); sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        shape.add(sp, BorderLayout.CENTER);

        JPanel empty = new JPanel(new GridBagLayout()); empty.setOpaque(false);
        JPanel eb = col();
        eb.add(Ui.left(new Ui.BusIcon(190, true))); eb.add(Ui.vgap(14));
        eb.add(Ui.left(L("Pick a bus from the list", B, 16, Ui.MUTED))); eb.add(Ui.left(L("Its live seat layout will appear here.", P, 13, Ui.MUTED)));
        empty.add(eb);
        seatHost.setOpaque(false); seatHost.add(empty, "empty"); seatHost.add(shape, "seats");
        c.add(head, BorderLayout.NORTH); c.add(seatHost, BorderLayout.CENTER);
        return c;
    }

    Ui.Card summaryCard() {
        Ui.Card c = new Ui.Card(); c.setLayout(new BorderLayout(0, 10)); c.setPreferredSize(new Dimension(300, 10));
        JPanel top = col();
        top.add(Ui.left(L("Booking summary", B, 17, Ui.TEXT))); top.add(Ui.vgap(12));
        top.add(Ui.left(sumRoute)); top.add(Ui.vgap(3)); top.add(Ui.left(sumWhen)); top.add(Ui.left(sumBus)); top.add(Ui.vgap(12));
        top.add(Ui.left(new Ui.Dashed())); top.add(Ui.vgap(6));
        top.add(Ui.left(L("SELECTED SEATS", B, 11, Ui.MUTED))); top.add(Ui.vgap(4));
        chips.setOpaque(false); top.add(Ui.left(chips)); top.add(Ui.vgap(12));
        top.add(Ui.left(L("PROMO CODE", B, 11, Ui.MUTED))); top.add(Ui.vgap(5));
        Ui.Btn apply = new Ui.Btn("Apply", Ui.PRIMARY, Ui.PRIMARY_DARK); apply.addActionListener(e -> applyCoupon());
        JPanel cr = new JPanel(new BorderLayout(8, 0)); cr.setOpaque(false); cr.add(coupon, BorderLayout.CENTER); cr.add(apply, BorderLayout.EAST);
        top.add(Ui.left(cr)); top.add(Ui.vgap(3)); top.add(Ui.left(couponMsg)); top.add(Ui.vgap(6));
        top.add(Ui.left(new Ui.Dashed())); top.add(Ui.vgap(6));
        top.add(Ui.left(priceRow(fareName, fareVal)));
        discRow.setOpaque(false); discRow.add(discName, BorderLayout.WEST); discRow.add(discVal, BorderLayout.EAST);
        top.add(Ui.vgap(4)); top.add(Ui.left(discRow)); top.add(Ui.vgap(8));
        top.add(Ui.left(priceRow(L("Total", B, 15, Ui.TEXT), totalVal)));

        JPanel bottom = col(); bookBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46)); bookBtn.addActionListener(e -> bookSeats());
        bottom.add(Ui.left(bookBtn)); bottom.add(Ui.vgap(8));
        bottom.add(Ui.left(L("Cancel for 90% refund (24h+), 50% (6-24h).", P, 11, Ui.MUTED)));
        c.add(top, BorderLayout.NORTH); c.add(bottom, BorderLayout.SOUTH);
        return c;
    }

    JPanel priceRow(JLabel l, JLabel r) { JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false); p.add(l, BorderLayout.WEST); p.add(r, BorderLayout.EAST); return p; }
    double total() { return selected.size() * currentFare * (100 - couponPct) / 100.0; }

    void refreshSummary() {
        boolean has = currentTrip >= 0;
        sumRoute.setText(has ? curRoute : "No bus selected"); sumWhen.setText(has ? curWhen : " "); sumBus.setText(has ? curBus : " ");
        chips.removeAll();
        if (selected.isEmpty()) chips.add(L(has ? "Tap seats on the map" : "-", P, 13, Ui.MUTED));
        for (int s : selected) chips.add(new Ui.Badge(String.valueOf(s), Color.WHITE, Ui.PRIMARY));
        chips.revalidate(); chips.repaint();
        double base = selected.size() * currentFare;
        fareName.setText("Fare  x " + selected.size()); fareVal.setText(Ui.money(base));
        discRow.setVisible(couponPct > 0); discName.setText("Discount (" + couponPct + "%)"); discVal.setText("- " + Ui.money(base * couponPct / 100.0));
        totalVal.setText(Ui.money(total()));
        bookBtn.setEnabled(!selected.isEmpty());
    }

    void applyCoupon() {
        try {
            int p = Db.couponPercent(coupon.getText());
            if (p < 0) { couponPct = 0; couponMsg.setForeground(Ui.BAD); couponMsg.setText("Invalid code. Try WELCOME10, SAVE20, FESTIVE15"); }
            else if (p == 0) { couponPct = 0; couponMsg.setForeground(Ui.MUTED); couponMsg.setText("Enter a promo code"); }
            else { couponPct = p; couponMsg.setForeground(Ui.OK); couponMsg.setText(p + "% discount applied!"); }
            refreshSummary();
        } catch (Exception e) { err(e); }
    }

    // ---------------- reviews ----------------
    void showReviews() {
        if (currentBusId < 0) { info("Select a bus first to read its reviews."); return; }
        try {
            List<Object[]> rows = Db.reviews(currentBusId);
            JPanel list = col(); double sum = 0;
            for (Object[] r : rows) {
                sum += Db.D(r[1]);
                JPanel row = new JPanel(new BorderLayout(12, 0)); row.setOpaque(false); row.setBorder(Ui.pad(8, 0, 8, 0));
                JPanel l = col(); l.add(Ui.left(L((String) r[0], B, 13, Ui.TEXT))); l.add(Ui.left(new Ui.Stars(Db.D(r[1]), 11)));
                JPanel rr = col(); rr.add(Ui.left(L(r[2] == null ? "" : (String) r[2], P, 13, Ui.TEXT))); rr.add(Ui.left(L(String.valueOf(r[3]).substring(0, 10), P, 11, Ui.MUTED)));
                l.setPreferredSize(new Dimension(110, 40)); row.add(l, BorderLayout.WEST); row.add(rr, BorderLayout.CENTER);
                list.add(Ui.left(row)); list.add(Ui.left(new Ui.Dashed()));
            }
            if (rows.isEmpty()) list.add(Ui.left(L("No reviews yet. Be the first to rate this bus after your trip!", P, 13, Ui.MUTED)));
            JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); head.setOpaque(false);
            head.add(L(curBusName, B, 18, Ui.TEXT));
            if (!rows.isEmpty()) { head.add(new Ui.Stars(sum / rows.size(), 15)); head.add(L(String.format("%.1f from %d reviews", sum / rows.size(), rows.size()), P, 12, Ui.MUTED)); }
            JPanel box = new JPanel(new BorderLayout(0, 10)); box.add(head, BorderLayout.NORTH);
            JScrollPane sp = Ui.scroll(new JScrollPane(list)); sp.setPreferredSize(new Dimension(520, 280)); sp.getViewport().setBackground(Ui.CARD); box.add(sp, BorderLayout.CENTER);
            JOptionPane.showMessageDialog(this, box, "Reviews", JOptionPane.PLAIN_MESSAGE);
        } catch (Exception e) { err(e); }
    }

    void rateBus() {
        int r = myTable.getSelectedRow();
        if (r < 0) { info("Select one of your bookings first, then click Rate bus."); return; }
        try {
            Object[] bus = Db.busOfBooking(Db.I(myModel.getValueAt(r, 0)), userId);
            if (bus == null) { info("Booking not found."); return; }
            final int[] stars = {5};
            JTextArea comment = Ui.area(3, 28);
            JPanel p = col();
            p.add(Ui.left(L("Rate " + bus[1], B, 18, Ui.TEXT))); p.add(Ui.vgap(4)); p.add(Ui.left(L("Tap a star to choose your rating", P, 12, Ui.MUTED))); p.add(Ui.vgap(10));
            p.add(Ui.left(new Ui.Stars(5, 30).editable(v -> stars[0] = v))); p.add(Ui.vgap(12));
            p.add(Ui.left(Ui.labeled("YOUR REVIEW (OPTIONAL)", comment)));
            if (JOptionPane.showConfirmDialog(this, p, "Rate your trip", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            Db.addReview(userId, (int) bus[0], stars[0], comment.getText().trim()); info("Thanks! Your review has been saved.");
        } catch (Exception e) { err(e); }
    }

    // ---------------- booking flow: details -> payment -> ticket ----------------
    static void cell(JPanel g, JComponent c, int x, int y) {
        GridBagConstraints gc = new GridBagConstraints(); gc.gridx = x; gc.gridy = y; gc.insets = new Insets(4, 4, 4, 4);
        gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = x == 1 ? 1 : 0; g.add(c, gc);
    }

    void bookSeats() {
        if (currentTrip < 0 || selected.isEmpty()) return;
        List<Integer> seats = new ArrayList<>(selected); double tot = total();
        try {
            int ladiesRows = Db.busInfo(currentTrip)[1];
            JPanel head = col();
            head.add(Ui.left(L("Passenger details", B, 18, Ui.TEXT)));
            head.add(Ui.left(L(seats.size() + (seats.size() == 1 ? " seat" : " seats") + Ui.DOT + "Total " + Ui.money(tot), P, 13, Ui.MUTED)));
            JPanel grid = new JPanel(new GridBagLayout()); grid.setOpaque(false);
            String[] hd = {"SEAT", "FULL NAME", "AGE", "GENDER"};
            for (int i = 0; i < 4; i++) cell(grid, L(hd[i], B, 11, Ui.MUTED), i, 0);
            List<JTextField> names = new ArrayList<>(), ages = new ArrayList<>(); List<JComboBox<String>> genders = new ArrayList<>();
            boolean anyLadies = false;
            for (int i = 0; i < seats.size(); i++) {
                JTextField n = Ui.field(16), a = Ui.field(3); JComboBox<String> g = new JComboBox<>(new String[]{"Male", "Female", "Other"});
                Ui.style(g); g.setPreferredSize(new Dimension(110, 36));
                boolean lady = (seats.get(i) - 1) / 4 < ladiesRows;
                if (lady) { g.setSelectedItem("Female"); g.setEnabled(false); anyLadies = true; }
                names.add(n); ages.add(a); genders.add(g);
                cell(grid, new Ui.Badge(String.valueOf(seats.get(i)), lady ? Ui.PINK : Color.WHITE, lady ? Ui.SEAT_LADY : Ui.PRIMARY), 0, i + 1);
                cell(grid, n, 1, i + 1); cell(grid, a, 2, i + 1); cell(grid, g, 3, i + 1);
            }
            JComboBox<String> bp = new JComboBox<>(Db.stops(curFrom).toArray(new String[0])), dp = new JComboBox<>(Db.stops(curTo).toArray(new String[0]));
            Ui.style(bp); Ui.style(dp);
            JPanel pts = new JPanel(new GridLayout(1, 2, 14, 0)); pts.setOpaque(false); pts.add(Ui.labeled("BOARDING POINT  (" + curFrom + ")", bp)); pts.add(Ui.labeled("DROPPING POINT  (" + curTo + ")", dp));
            JPanel south = col(); south.setBorder(Ui.pad(12, 0, 0, 0)); south.add(Ui.left(pts));
            if (anyLadies) { south.add(Ui.vgap(8)); south.add(Ui.left(L("Pink seats are reserved for women passengers.", P, 12, Ui.PINK))); }
            JPanel form = new JPanel(new BorderLayout(0, 12)); form.add(head, BorderLayout.NORTH); form.add(grid, BorderLayout.CENTER); form.add(south, BorderLayout.SOUTH);
            if (JOptionPane.showConfirmDialog(this, form, "Passenger details", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            List<String[]> pax = new ArrayList<>();
            for (int i = 0; i < seats.size(); i++) {
                String n = names.get(i).getText().trim(); int age;
                try { age = Integer.parseInt(ages.get(i).getText().trim()); } catch (NumberFormatException e) { age = -1; }
                if (n.isEmpty() || age < 1 || age > 120) { info("Please enter a valid name and age (1-120) for seat " + seats.get(i) + "."); return; }
                pax.add(new String[]{n, String.valueOf(age), (String) genders.get(i).getSelectedItem()});
            }
            String method = PayDialog.ask(this, tot, Db.wallet(userId));
            if (method == null) { info("Payment was not completed. Your seats are not booked yet."); return; }
            String ref = Db.book(currentTrip, userId, seats, pax, couponPct > 0 ? coupon.getText() : "", method, (String) bp.getSelectedItem(), (String) dp.getSelectedItem());
            refreshHeader();
            int choice = JOptionPane.showOptionDialog(this, ticketPanel(ref, seats, pax, tot, (String) bp.getSelectedItem(), (String) dp.getSelectedItem(), method), "Booking confirmed",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[]{"Download PDF ticket", "Close"}, "Download PDF ticket");
            if (choice == 0) savePdf(ref);
            search();
        } catch (Exception e) { err(e); search(); }
    }

    JPanel ticketPanel(String ref, List<Integer> seats, List<String[]> pax, double tot, String boarding, String dropping, String method) {
        Ui.Grad head = new Ui.Grad(new BorderLayout(), Ui.DEEP, Ui.VIOLET); head.decor = true; head.setBorder(Ui.pad(16, 20, 16, 20));
        JPanel hl = col();
        hl.add(Ui.left(L("BOOKING CONFIRMED", B, 11, new Color(0xA5F3FC)))); hl.add(Ui.vgap(4)); hl.add(Ui.left(L(curRoute.trim(), B, 18, Color.WHITE)));
        JPanel hr = col();
        JLabel rl = L("REF", B, 11, new Color(0xA5F3FC)); rl.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JLabel rv = L(ref, B, 20, Ui.ACCENT); rv.setAlignmentX(Component.RIGHT_ALIGNMENT);
        hr.add(rl); hr.add(Ui.vgap(4)); hr.add(rv);
        head.add(hl, BorderLayout.WEST); head.add(hr, BorderLayout.EAST);

        JPanel body = col();
        body.add(Ui.left(L(curWhen, B, 14, Ui.TEXT))); body.add(Ui.left(L(curBus, P, 13, Ui.MUTED))); body.add(Ui.vgap(6));
        body.add(Ui.left(L("Board: " + boarding + "   |   Drop: " + dropping, P, 12, Ui.MUTED))); body.add(Ui.vgap(8));
        body.add(Ui.left(new Ui.Dashed())); body.add(Ui.vgap(6));
        for (int i = 0; i < seats.size(); i++) {
            JPanel r = new JPanel(new BorderLayout(10, 0)); r.setOpaque(false);
            r.add(new Ui.Badge("Seat " + seats.get(i), Color.WHITE, Ui.PRIMARY), BorderLayout.WEST);
            r.add(L(pax.get(i)[0] + "  (" + pax.get(i)[1] + ", " + pax.get(i)[2] + ")", P, 13, Ui.TEXT), BorderLayout.CENTER);
            body.add(Ui.left(r)); body.add(Ui.vgap(6));
        }
        body.add(Ui.left(new Ui.Dashed())); body.add(Ui.vgap(6));
        body.add(Ui.left(priceRow(L("Total paid  (" + method + ")", B, 14, Ui.TEXT), L(Ui.money(tot), B, 18, Ui.PRIMARY)))); body.add(Ui.vgap(8));
        body.add(Ui.left(L("A confirmation was sent to your Inbox.", P, 11, Ui.MUTED)));
        JPanel bw = new JPanel(new BorderLayout()); bw.setBackground(Ui.CARD); bw.setBorder(Ui.pad(14, 20, 16, 20)); bw.add(body, BorderLayout.NORTH);
        JPanel root = new JPanel(new BorderLayout()); root.add(head, BorderLayout.NORTH); root.add(bw, BorderLayout.CENTER);
        root.setPreferredSize(new Dimension(440, root.getPreferredSize().height));
        return root;
    }

    void savePdf(String ref) {
        try {
            List<Object[]> rows = Db.ticketRows(ref, userId);
            if (rows.isEmpty()) { info("There are no active seats in booking " + ref + " (all cancelled)."); return; }
            File f = new File("ticket_" + ref + ".pdf");
            Pdf.ticket(f, ref, rows, username);
            try { if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) Desktop.getDesktop().open(f); } catch (Exception ignore) { }
            info("Ticket saved as " + f.getAbsolutePath());
        } catch (Exception e) { err(e); }
    }

    // ================= MY BOOKINGS =================
    JPanel myPage() {
        JPanel p = new JPanel(new BorderLayout(0, 14)); p.setBackground(Ui.BG); p.setBorder(Ui.pad(18, 22, 18, 22));
        JPanel top = new JPanel(new BorderLayout(0, 10)); top.setOpaque(false);
        JPanel t1 = new JPanel(new BorderLayout()); t1.setOpaque(false); t1.add(L("My bookings", B, 24, Ui.TEXT), BorderLayout.WEST);
        JPanel find = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); find.setOpaque(false);
        findField.setToolTipText("Booking reference, e.g. BG123456");
        Ui.Btn go = new Ui.Btn("Find by reference", Ui.PRIMARY, Ui.PRIMARY_DARK), all = new Ui.Btn("Show all", Ui.LINE, Ui.FIELD_LINE); all.fg = Ui.TEXT;
        go.addActionListener(e -> findBooking()); findField.addActionListener(e -> findBooking()); all.addActionListener(e -> { findField.setText(""); loadMine(); });
        find.add(findField); find.add(go); find.add(all); t1.add(find, BorderLayout.EAST);
        JPanel acts = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); acts.setOpaque(false);
        Ui.Btn pdf = new Ui.Btn("Download ticket PDF", Ui.PRIMARY, Ui.PRIMARY_DARK), rate = new Ui.Btn("Rate bus", Ui.ACCENT, Ui.ACCENT_DARK), cancel = new Ui.Btn("Cancel selected seat", Ui.BAD, new Color(0xB91C1C));
        pdf.addActionListener(e -> { int r = myTable.getSelectedRow(); if (r < 0) info("Select a booking row first."); else savePdf((String) myModel.getValueAt(r, 1)); });
        rate.addActionListener(e -> rateBus()); cancel.addActionListener(e -> cancelSelected());
        acts.add(pdf); acts.add(rate); acts.add(cancel); acts.add(findMsg);
        top.add(t1, BorderLayout.NORTH); top.add(acts, BorderLayout.SOUTH);

        Ui.styleTable(myTable); myTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); Ui.widths(myTable, 60, 100, 230, 110, 70, 60, 170, 110, 110, 110);
        JScrollPane sp = Ui.tableScroll(myTable);
        JPanel empty = new JPanel(new GridBagLayout()); empty.setOpaque(false);
        JPanel eb = col(); eb.add(Ui.left(new Ui.BusIcon(170, true))); eb.add(Ui.vgap(12));
        eb.add(Ui.left(L("No bookings found", B, 17, Ui.MUTED))); eb.add(Ui.left(L("Your tickets will show up here after you book.", P, 13, Ui.MUTED)));
        empty.add(eb);
        myHost.setOpaque(false); myHost.add(sp, "table"); myHost.add(empty, "empty");
        Ui.Card card = new Ui.Card(); card.add(myHost, BorderLayout.CENTER);
        card.add(L("Refund policy:  90% if cancelled 24h+ before departure  \u2022  50% between 6 and 24h  \u2022  none under 6h  \u2022  refunds go to your BusGo wallet", P, 12, Ui.MUTED), BorderLayout.SOUTH);
        p.add(top, BorderLayout.NORTH); p.add(card, BorderLayout.CENTER);
        return p;
    }

    void fillMine(List<Object[]> rows) {
        myModel.setRowCount(0); for (Object[] r : rows) myModel.addRow(r);
        myCards.show(myHost, rows.isEmpty() ? "empty" : "table");
    }
    void loadMine() { try { fillMine(Db.myBookings(userId)); findMsg.setText(" "); } catch (Exception e) { err(e); } }
    void findBooking() {
        if (findField.getText().isBlank()) { loadMine(); return; }
        try { List<Object[]> r = Db.findByRef(findField.getText(), userId); fillMine(r); findMsg.setText(r.size() + " seat(s) found"); } catch (Exception e) { err(e); }
    }

    void cancelSelected() {
        int r = myTable.getSelectedRow();
        if (r < 0) { info("Select a booking row first."); return; }
        if (!"CONFIRMED".equals(myModel.getValueAt(r, 8))) { info("That seat is already cancelled."); return; }
        if (JOptionPane.showConfirmDialog(this, "Cancel this seat? A cancellation fee may apply.", "Confirm cancellation", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            double refund = Db.cancel(Db.I(myModel.getValueAt(r, 0)), userId);
            info("Seat cancelled. " + Ui.money(refund) + " has been credited to your wallet."); loadMine(); refreshHeader();
        } catch (Exception e) { err(e); }
    }

    // ================= WALLET =================
    JPanel walletPage() {
        JPanel p = new JPanel(new BorderLayout(0, 16)); p.setBackground(Ui.BG); p.setBorder(Ui.pad(18, 22, 18, 22));
        JPanel hero = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create(); Ui.aa(g);
                g.setPaint(new GradientPaint(0, 0, Ui.DEEP, getWidth(), getHeight(), Ui.VIOLET)); g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g.setPaint(new GradientPaint(0, 0, new Color(34, 211, 238, 90), getWidth(), 0, new Color(34, 211, 238, 0))); g.fillRoundRect(0, 0, getWidth() - 1, 6, 6, 6);
                Ui.drawBus(g, getWidth() - 330, 24, 280, false); g.dispose();
            }
        };
        hero.setOpaque(false); hero.setBorder(Ui.pad(24, 28, 24, 28)); hero.setPreferredSize(new Dimension(10, 150));
        balanceBig.setPreferredSize(new Dimension(420, 56));
        JPanel hl = col(); hl.add(Ui.left(L("BUSGO WALLET", B, 12, new Color(0xA5F3FC)))); hl.add(Ui.vgap(6)); hl.add(Ui.left(balanceBig)); hl.add(Ui.vgap(4));
        hl.add(Ui.left(L("Refunds land here instantly. Use it to pay for tickets.", P, 13, new Color(0xC7D2FE))));
        hero.add(hl, BorderLayout.WEST);

        Ui.Card add = new Ui.Card(); add.setLayout(new BoxLayout(add, BoxLayout.Y_AXIS));
        add.add(Ui.left(L("Add money", B, 17, Ui.TEXT))); add.add(Ui.vgap(10));
        JPanel row = xrow();
        for (int amt : new int[]{500, 1000, 2000}) {
            Ui.Btn b = new Ui.Btn("+ " + Ui.RS + amt, Ui.CHIP, Ui.LINE); b.fg = Ui.TEXT; b.addActionListener(e -> topUp(amt)); row.add(b); row.add(Box.createHorizontalStrut(8));
        }
        topAmount.setMaximumSize(new Dimension(120, 40)); topAmount.setToolTipText("Custom amount");
        Ui.Btn custom = new Ui.Btn("Add custom", Ui.PRIMARY, Ui.PRIMARY_DARK);
        custom.addActionListener(e -> { try { double v = Double.parseDouble(topAmount.getText().trim()); if (v < 10 || v > 50000) throw new NumberFormatException(); topUp(v); } catch (NumberFormatException ex) { info("Enter an amount between 10 and 50000."); } });
        row.add(topAmount); row.add(Box.createHorizontalStrut(8)); row.add(custom); row.add(Box.createHorizontalGlue());
        add.add(Ui.left(row));

        Ui.styleTable(txTable); txTable.putClientProperty("signed", Boolean.TRUE); Ui.widths(txTable, 170, 520, 140);
        Ui.Card hist = new Ui.Card(); hist.add(L("Transactions", B, 17, Ui.TEXT), BorderLayout.NORTH); hist.add(Ui.tableScroll(txTable), BorderLayout.CENTER);
        JPanel north = new JPanel(new BorderLayout(0, 16)); north.setOpaque(false); north.add(hero, BorderLayout.NORTH); north.add(add, BorderLayout.CENTER);
        p.add(north, BorderLayout.NORTH); p.add(hist, BorderLayout.CENTER);
        return p;
    }

    void topUp(double amt) {
        String m = PayDialog.ask(this, amt, 0);
        if (m == null) return;
        try { Db.topUp(userId, amt); topAmount.setText(""); loadWallet(); refreshHeader(); } catch (Exception e) { err(e); }
    }
    void loadWallet() {
        try {
            balanceBig.setText(Ui.money(Db.wallet(userId))); txModel.setRowCount(0);
            for (Object[] r : Db.walletTx(userId)) txModel.addRow(new Object[]{r[0], r[1], Db.D(r[2])});
        } catch (Exception e) { err(e); }
    }

    // ================= INBOX =================
    JPanel inboxPage() {
        JPanel p = new JPanel(new BorderLayout(0, 14)); p.setBackground(Ui.BG); p.setBorder(Ui.pad(18, 22, 18, 22));
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false);
        top.add(L("Inbox", B, 24, Ui.TEXT), BorderLayout.WEST);
        Ui.Btn mark = new Ui.Btn("Mark all as read", Ui.LINE, Ui.FIELD_LINE); mark.fg = Ui.TEXT;
        mark.addActionListener(e -> { try { Db.markAllRead(userId); loadInbox(); refreshHeader(); } catch (Exception ex) { err(ex); } });
        top.add(mark, BorderLayout.EAST);
        inboxList.setOpaque(false); inboxList.setLayout(new BoxLayout(inboxList, BoxLayout.Y_AXIS));
        JPanel hold = new JPanel(new BorderLayout()); hold.setOpaque(false); hold.add(inboxList, BorderLayout.NORTH);
        JScrollPane sp = Ui.scroll(new JScrollPane(hold)); sp.setOpaque(false); sp.getViewport().setOpaque(false); sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        p.add(top, BorderLayout.NORTH); p.add(sp, BorderLayout.CENTER);
        return p;
    }

    void loadInbox() {
        try {
            inboxList.removeAll();
            List<Object[]> rows = Db.notifications(userId);
            if (rows.isEmpty()) inboxList.add(Ui.left(L("You're all caught up - no notifications yet.", B, 14, Ui.MUTED)));
            for (Object[] r : rows) {
                boolean unread = Db.I(r[3]) == 0;
                Ui.Tile t = new Ui.Tile(new BorderLayout(14, 0)); t.sel = unread; t.setCursor(Cursor.getDefaultCursor());
                JTextArea msg = new JTextArea((String) r[1]); msg.setEditable(false); msg.setOpaque(false); msg.setLineWrap(true); msg.setWrapStyleWord(true);
                msg.setFont(Ui.font(unread ? B : P, 13)); msg.setForeground(Ui.TEXT); msg.setBorder(null); msg.setFocusable(false);
                JPanel mid = col(); mid.add(Ui.left(msg)); mid.add(Ui.vgap(4)); mid.add(Ui.left(L(String.valueOf(r[2]), P, 11, Ui.MUTED)));
                JPanel badge = new JPanel(new BorderLayout()); badge.setOpaque(false); badge.setPreferredSize(new Dimension(52, 10));
                if (unread) badge.add(new Ui.Badge("NEW", Color.WHITE, Ui.PRIMARY), BorderLayout.NORTH);
                t.add(badge, BorderLayout.WEST); t.add(mid, BorderLayout.CENTER);
                inboxList.add(t); inboxList.add(Ui.vgap(10));
            }
            inboxList.revalidate(); inboxList.repaint();
        } catch (Exception e) { err(e); }
    }

    // ================= helpers & entry =================
    void info(String m) { JOptionPane.showMessageDialog(this, m, "BusGo", JOptionPane.INFORMATION_MESSAGE); }
    void err(Exception e) { JOptionPane.showMessageDialog(this, e.getMessage() == null ? e.toString() : e.getMessage(), "Something went wrong", JOptionPane.ERROR_MESSAGE); }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            for (String k : new String[]{"Label.font", "Button.font", "ComboBox.font", "TextField.font", "ToolTip.font", "OptionPane.messageFont", "OptionPane.buttonFont", "Table.font", "CheckBox.font"})
                UIManager.put(k, Ui.font(P, 13));
            loadSettings();
            Db.init();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage() + "\nIs the sqlite-jdbc jar in the lib folder?");
            return;
        }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
