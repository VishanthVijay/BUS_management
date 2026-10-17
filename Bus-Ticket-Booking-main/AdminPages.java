import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Admin console: trips (edit / cancel / delete / manifest), fleet, scheduling, booking search, revenue. */
public class AdminPages {
    static final int B = Font.BOLD, P = Font.PLAIN;
    final BusApp app;
    final DefaultTableModel tripModel = model("ID", "Route", "Date", "Time", "Bus", "Fare", "Seats sold", "Status");
    final JTable tripTable = new JTable(tripModel);
    final JCheckBox past = new JCheckBox("Include past trips");
    final DefaultTableModel findModel = model("Ref", "Passenger", "Route", "Date", "Seat", "Status", "Amount", "Booked by");
    final JTextField findField = Ui.field(18);
    final JComboBox<String> tripBus = new JComboBox<>();
    final JLabel statRev = BusApp.L("-", B, 28, Ui.PRIMARY), statSeats = BusApp.L("-", B, 28, Ui.OK), statRoutes = BusApp.L("-", B, 28, Ui.ACCENT_DARK);
    final Ui.Bars bars = new Ui.Bars();

    AdminPages(BusApp app) { this.app = app; }

    static DefaultTableModel model(String... cols) { return new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } }; }

    JPanel page() {
        JPanel p = new JPanel(new BorderLayout(0, 16)); p.setBackground(Ui.BG); p.setBorder(Ui.pad(18, 22, 18, 22));
        CardLayout cl = new CardLayout(); JPanel host = new JPanel(cl); host.setOpaque(false);
        host.add(tripsPanel(), "0"); host.add(busForm(), "1"); host.add(tripForm(), "2"); host.add(findPanel(), "3"); host.add(reportPanel(), "4");
        JPanel top = new JPanel(new BorderLayout(0, 12)); top.setOpaque(false);
        top.add(BusApp.L("Admin console", B, 24, Ui.TEXT), BorderLayout.NORTH);
        top.add(Ui.pills(new String[]{"Trips", "Add Bus", "Schedule Trip", "Find Booking", "Revenue"}, i -> { cl.show(host, String.valueOf(i)); if (i == 0) loadTrips(); if (i == 4) loadReport(); }), BorderLayout.CENTER);
        p.add(top, BorderLayout.NORTH); p.add(host, BorderLayout.CENTER);
        return p;
    }
    void onShow() { loadTrips(); }

    // ---------------- trips ----------------
    JPanel tripsPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 12)); p.setOpaque(false);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); bar.setOpaque(false);
        Ui.Btn edit = new Ui.Btn("Edit trip", Ui.PRIMARY, Ui.PRIMARY_DARK), men = new Ui.Btn("Passengers", Ui.VIOLET, new Color(0x6D28D9)),
                cancel = new Ui.Btn("Cancel trip + refund", Ui.BAD, new Color(0xB91C1C)), del = new Ui.Btn("Delete", Ui.LINE, Ui.FIELD_LINE), ref = new Ui.Btn("Refresh", Ui.LINE, Ui.FIELD_LINE);
        del.fg = Ui.TEXT; ref.fg = Ui.TEXT;
        past.setOpaque(false); past.setForeground(Ui.TEXT); past.setFont(Ui.font(P, 13)); past.setFocusPainted(false); past.addActionListener(e -> loadTrips());
        edit.addActionListener(e -> editTrip()); men.addActionListener(e -> manifest()); cancel.addActionListener(e -> cancelTrip()); del.addActionListener(e -> deleteTrip()); ref.addActionListener(e -> loadTrips());
        bar.add(edit); bar.add(men); bar.add(cancel); bar.add(del); bar.add(ref); bar.add(past);
        Ui.styleTable(tripTable); tripTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); Ui.widths(tripTable, 55, 250, 110, 80, 190, 110, 100, 100);
        Ui.Card c = new Ui.Card(); c.add(Ui.tableScroll(tripTable), BorderLayout.CENTER);
        p.add(bar, BorderLayout.NORTH); p.add(c, BorderLayout.CENTER);
        return p;
    }

    void loadTrips() { try { tripModel.setRowCount(0); for (Object[] r : Db.adminTrips(past.isSelected())) tripModel.addRow(r); } catch (Exception e) { app.err(e); } }

    int selectedTrip() {
        int r = tripTable.getSelectedRow();
        if (r < 0) { app.info("Select a trip in the table first."); return -1; }
        return Db.I(tripModel.getValueAt(r, 0));
    }

    void editTrip() {
        int id = selectedTrip(); if (id < 0) return; int r = tripTable.getSelectedRow();
        JTextField date = Ui.field(10), time = Ui.field(6), fare = Ui.field(8);
        date.setText((String) tripModel.getValueAt(r, 2)); time.setText((String) tripModel.getValueAt(r, 3)); fare.setText(String.valueOf(Db.D(tripModel.getValueAt(r, 5))));
        JPanel row = new JPanel(new GridLayout(1, 3, 12, 0)); row.setOpaque(false);
        row.add(Ui.labeled("DATE (YYYY-MM-DD)", date)); row.add(Ui.labeled("TIME (HH:MM)", time)); row.add(Ui.labeled("FARE", fare));
        JPanel box = new JPanel(); box.setOpaque(false); box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(Ui.left(BusApp.L("Edit trip #" + id, B, 18, Ui.TEXT))); box.add(Ui.left(BusApp.L((String) tripModel.getValueAt(r, 1), P, 13, Ui.MUTED))); box.add(Ui.vgap(12)); box.add(Ui.left(row));
        box.add(Ui.vgap(8)); box.add(Ui.left(BusApp.L("Passengers are notified automatically if the date or time changes.", P, 12, Ui.MUTED)));
        if (JOptionPane.showConfirmDialog(app, box, "Edit trip", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            LocalDate.parse(date.getText().trim()); LocalTime.parse(time.getText().trim());
            Db.updateTrip(id, date.getText().trim(), time.getText().trim(), Double.parseDouble(fare.getText().trim())); loadTrips(); app.info("Trip updated.");
        } catch (Exception e) { app.err(new Exception("Use date YYYY-MM-DD, time HH:MM (24h) and a numeric fare.")); }
    }

    void cancelTrip() {
        int id = selectedTrip(); if (id < 0) return; int r = tripTable.getSelectedRow();
        if ("CANCELLED".equals(tripModel.getValueAt(r, 7))) { app.info("This trip is already cancelled."); return; }
        int sold = Db.I(tripModel.getValueAt(r, 6));
        if (JOptionPane.showConfirmDialog(app, "Cancel this trip?\n" + sold + " booked seat(s) will be refunded 100% to passenger wallets and they will be notified.", "Cancel trip", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try { int n = Db.cancelTrip(id); loadTrips(); app.info("Trip cancelled. " + n + " seat(s) refunded."); } catch (Exception e) { app.err(e); }
    }

    void deleteTrip() {
        int id = selectedTrip(); if (id < 0) return;
        if (JOptionPane.showConfirmDialog(app, "Delete trip #" + id + " permanently? (Only possible if it never had bookings.)", "Delete trip", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try { Db.deleteTrip(id); loadTrips(); } catch (Exception e) { app.err(e); }
    }

    void manifest() {
        int id = selectedTrip(); if (id < 0) return; int r = tripTable.getSelectedRow();
        try {
            List<Object[]> rows = Db.manifest(id);
            DefaultTableModel m = model("Seat", "Passenger", "Age", "Gender", "Boarding", "Dropping", "Ref", "Booked by");
            for (Object[] x : rows) m.addRow(x);
            JTable t = new JTable(m); Ui.styleTable(t); Ui.widths(t, 55, 170, 55, 80, 150, 150, 100, 100);
            JScrollPane sp = Ui.tableScroll(t); sp.setPreferredSize(new Dimension(780, 340));
            JPanel box = new JPanel(new BorderLayout(0, 10));
            box.add(BusApp.L("Passenger list  -  " + tripModel.getValueAt(r, 1) + Ui.DOT + tripModel.getValueAt(r, 2) + " " + tripModel.getValueAt(r, 3) + Ui.DOT + rows.size() + " passengers", B, 15, Ui.TEXT), BorderLayout.NORTH);
            box.add(sp, BorderLayout.CENTER);
            int c = JOptionPane.showOptionDialog(app, box, "Passenger manifest", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[]{"Export CSV", "Close"}, "Close");
            if (c == 0) {
                StringBuilder sb = new StringBuilder("Seat,Passenger,Age,Gender,Boarding,Dropping,Ref,BookedBy\n");
                for (Object[] x : rows) { for (int i = 0; i < x.length; i++) sb.append(i > 0 ? "," : "").append('"').append(String.valueOf(x[i]).replace("\"", "\"\"")).append('"'); sb.append('\n'); }
                File f = new File("manifest_trip_" + id + ".csv"); Files.writeString(f.toPath(), sb.toString());
                app.info("Saved " + f.getAbsolutePath());
            }
        } catch (Exception e) { app.err(e); }
    }

    // ---------------- fleet + scheduling ----------------
    JPanel busForm() {
        JTextField name = Ui.field(20), seats = Ui.field(5), amen = Ui.field(20), lady = Ui.field(3); seats.setText("40"); lady.setText("2"); amen.setText("WiFi, Charging, Blanket");
        JComboBox<String> type = new JComboBox<>(new String[]{"AC Sleeper", "AC Seater", "Non-AC Seater", "Non-AC Sleeper", "Electric AC Seater", "Electric AC Sleeper"}); Ui.style(type);
        JPanel r = new JPanel(new GridLayout(1, 2, 12, 0)); r.setOpaque(false); r.add(Ui.labeled("TOTAL SEATS (4-60)", seats)); r.add(Ui.labeled("LADIES ROWS (0-4)", lady));
        Ui.Btn add = new Ui.Btn("Add bus", Ui.PRIMARY, Ui.PRIMARY_DARK);
        add.addActionListener(e -> {
            try {
                int s = Integer.parseInt(seats.getText().trim()), l = Integer.parseInt(lady.getText().trim());
                if (name.getText().isBlank() || s < 4 || s > 60 || l < 0 || l > 4) throw new IllegalArgumentException("Enter a name, seats 4-60 and ladies rows 0-4.");
                Db.addBus(name.getText().trim(), (String) type.getSelectedItem(), s, l, amen.getText().trim().isEmpty() ? "Charging" : amen.getText().trim());
                fillBuses(); name.setText(""); app.info("Bus added to your fleet.");
            } catch (Exception ex) { app.err(ex); }
        });
        return Ui.formCard("Add a new bus", "Register a bus so you can schedule trips for it.", Ui.labeled("BUS NAME", name), Ui.labeled("BUS TYPE", type), r, Ui.labeled("AMENITIES (comma separated)", amen), add);
    }

    void fillBuses() { try { tripBus.removeAllItems(); for (String b : Db.buses()) tripBus.addItem(b); } catch (Exception e) { app.err(e); } }

    JPanel tripForm() {
        Ui.style(tripBus); tripBus.setPreferredSize(new Dimension(440, 36)); tripBus.setMaximumSize(new Dimension(440, 36)); fillBuses();
        JTextField src = Ui.field(10), dst = Ui.field(10), date = Ui.field(10), time = Ui.field(6), fare = Ui.field(6), dur = Ui.field(5), days = Ui.field(4);
        date.setText(LocalDate.now().plusDays(1).toString()); time.setText("22:00"); fare.setText("800"); dur.setText("420"); days.setText("1");
        JPanel r1 = new JPanel(new GridLayout(1, 2, 12, 0)); r1.setOpaque(false); r1.add(Ui.labeled("FROM", src)); r1.add(Ui.labeled("TO", dst));
        JPanel r2 = new JPanel(new GridLayout(1, 3, 12, 0)); r2.setOpaque(false);
        r2.add(Ui.labeled("DATE (YYYY-MM-DD)", date)); r2.add(Ui.labeled("TIME (HH:MM)", time)); r2.add(Ui.labeled("FARE", fare));
        JPanel r3 = new JPanel(new GridLayout(1, 2, 12, 0)); r3.setOpaque(false); r3.add(Ui.labeled("DURATION (MINUTES)", dur)); r3.add(Ui.labeled("REPEAT DAILY FOR (DAYS)", days));
        Ui.Btn add = new Ui.Btn("Schedule trip", Ui.PRIMARY, Ui.PRIMARY_DARK);
        add.addActionListener(e -> {
            try {
                if (tripBus.getSelectedItem() == null) throw new IllegalArgumentException("Add a bus first.");
                int busId = Integer.parseInt(((String) tripBus.getSelectedItem()).split(" - ")[0]);
                LocalDate.parse(date.getText().trim()); LocalTime.parse(time.getText().trim());
                int d = Integer.parseInt(days.getText().trim()), m = Integer.parseInt(dur.getText().trim());
                if (src.getText().isBlank() || dst.getText().isBlank()) throw new IllegalArgumentException("Enter source and destination.");
                if (d < 1 || d > 60 || m < 30) throw new IllegalArgumentException("Repeat days must be 1-60 and duration at least 30 minutes.");
                Db.addTrips(busId, src.getText().trim(), dst.getText().trim(), date.getText().trim(), time.getText().trim(), Double.parseDouble(fare.getText().trim()), m, d);
                app.info(d + " trip(s) scheduled!");
            } catch (java.time.format.DateTimeParseException ex) { app.err(new Exception("Use date YYYY-MM-DD and time HH:MM (24h)."));
            } catch (NumberFormatException ex) { app.err(new Exception("Fare, duration and days must be numbers."));
            } catch (Exception ex) { app.err(ex); }
        });
        return Ui.formCard("Schedule a trip", "Pick a bus, route, departure and fare. Repeat it daily to fill the calendar.", Ui.labeled("BUS", tripBus), r1, r2, r3, add);
    }

    // ---------------- find booking ----------------
    JPanel findPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 12)); p.setOpaque(false);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); bar.setOpaque(false);
        Ui.Btn go = new Ui.Btn("Search", Ui.PRIMARY, Ui.PRIMARY_DARK); go.addActionListener(e -> doFind()); findField.addActionListener(e -> doFind());
        findField.setToolTipText("Booking reference, passenger name or username");
        bar.add(findField); bar.add(go); bar.add(BusApp.L("Search by booking reference, passenger name or username", P, 12, Ui.MUTED));
        JTable t = new JTable(findModel); Ui.styleTable(t); Ui.widths(t, 110, 160, 240, 110, 60, 110, 110, 120);
        Ui.Card c = new Ui.Card(); c.add(Ui.tableScroll(t), BorderLayout.CENTER);
        p.add(bar, BorderLayout.NORTH); p.add(c, BorderLayout.CENTER);
        return p;
    }
    void doFind() {
        if (findField.getText().isBlank()) { app.info("Type a reference, passenger name or username."); return; }
        try { findModel.setRowCount(0); for (Object[] r : Db.searchBookings(findField.getText())) findModel.addRow(r); } catch (Exception e) { app.err(e); }
    }

    // ---------------- revenue ----------------
    JPanel reportPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 16)); p.setOpaque(false);
        JPanel stats = new JPanel(new GridLayout(1, 3, 16, 0)); stats.setOpaque(false);
        stats.add(statCard("NET REVENUE", statRev)); stats.add(statCard("SEATS SOLD", statSeats)); stats.add(statCard("ACTIVE ROUTES", statRoutes));
        Ui.Card chart = new Ui.Card();
        JPanel h = new JPanel(new BorderLayout()); h.setOpaque(false); h.setBorder(Ui.pad(0, 0, 10, 0));
        h.add(BusApp.L("Top routes by revenue", B, 17, Ui.TEXT), BorderLayout.WEST);
        Ui.Btn r = new Ui.Btn("Refresh", Ui.LINE, Ui.FIELD_LINE); r.fg = Ui.TEXT; r.addActionListener(e -> loadReport()); h.add(r, BorderLayout.EAST);
        JPanel hold = new JPanel(new BorderLayout()); hold.setOpaque(false); hold.add(bars, BorderLayout.NORTH);
        JScrollPane sp = Ui.scroll(new JScrollPane(hold)); sp.setOpaque(false); sp.getViewport().setOpaque(false);
        chart.add(h, BorderLayout.NORTH); chart.add(sp, BorderLayout.CENTER);
        p.add(stats, BorderLayout.NORTH); p.add(chart, BorderLayout.CENTER);
        return p;
    }
    Ui.Card statCard(String title, JLabel value) { Ui.Card c = new Ui.Card(); c.add(BusApp.L(title, B, 11, Ui.MUTED), BorderLayout.NORTH); c.add(value, BorderLayout.CENTER); return c; }
    void loadReport() {
        try {
            List<Object[]> rows = Db.routeReport(); double rev = 0; int sold = 0;
            for (Object[] r : rows) { sold += Db.I(r[1]); rev += Db.D(r[2]); }
            statRev.setText(Ui.money(rev)); statSeats.setText(String.valueOf(sold)); statRoutes.setText(String.valueOf(rows.size())); bars.set(rows);
        } catch (Exception e) { app.err(e); }
    }
}
