import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.util.*;

/** All JDBC access lives here. SQLite file DB: busgo.db */
public class Db {
    static final String URL = "jdbc:sqlite:busgo.db";
    static int I(Object o) { return ((Number) o).intValue(); }
    static double D(Object o) { return o == null ? 0 : ((Number) o).doubleValue(); }
    static String now() { return LocalDateTime.now().withNano(0).toString().replace('T', ' '); }

    static Connection get() throws SQLException {
        Connection c = DriverManager.getConnection(URL);
        try (Statement s = c.createStatement()) { s.execute("PRAGMA foreign_keys=ON"); }
        return c;
    }

    static List<Object[]> rows(Connection c, String sql, Object... args) throws SQLException {
        List<Object[]> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            ResultSet rs = ps.executeQuery(); int n = rs.getMetaData().getColumnCount();
            while (rs.next()) { Object[] r = new Object[n]; for (int i = 0; i < n; i++) r[i] = rs.getObject(i + 1); out.add(r); }
        }
        return out;
    }
    static List<Object[]> rows(String sql, Object... args) throws SQLException { try (Connection c = get()) { return rows(c, sql, args); } }
    static int exec(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) { for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]); return ps.executeUpdate(); }
    }

    static String hash(String p) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(p.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // ================= schema + demo data =================
    static void init() throws SQLException {
        try (Connection c = get(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS users(id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL, password TEXT NOT NULL, role TEXT NOT NULL, wallet REAL NOT NULL DEFAULT 0)");
            s.execute("CREATE TABLE IF NOT EXISTS buses(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, type TEXT NOT NULL, total_seats INTEGER NOT NULL, ladies_rows INTEGER NOT NULL DEFAULT 2, amenities TEXT NOT NULL DEFAULT '')");
            s.execute("CREATE TABLE IF NOT EXISTS trips(id INTEGER PRIMARY KEY AUTOINCREMENT, bus_id INTEGER NOT NULL REFERENCES buses(id), source TEXT NOT NULL, dest TEXT NOT NULL, travel_date TEXT NOT NULL, depart_time TEXT NOT NULL, duration_min INTEGER NOT NULL DEFAULT 360, fare REAL NOT NULL, status TEXT NOT NULL DEFAULT 'ACTIVE')");
            s.execute("CREATE TABLE IF NOT EXISTS bookings(id INTEGER PRIMARY KEY AUTOINCREMENT, ref TEXT NOT NULL, trip_id INTEGER NOT NULL REFERENCES trips(id), user_id INTEGER NOT NULL REFERENCES users(id), seat_no INTEGER NOT NULL, passenger TEXT NOT NULL, age INTEGER, gender TEXT, amount REAL NOT NULL, status TEXT NOT NULL DEFAULT 'CONFIRMED', refund REAL DEFAULT 0, booked_at TEXT NOT NULL, pay_method TEXT DEFAULT '', boarding TEXT DEFAULT '', dropping TEXT DEFAULT '')");
            s.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_seat ON bookings(trip_id, seat_no) WHERE status='CONFIRMED'");
            s.execute("CREATE TABLE IF NOT EXISTS coupons(code TEXT PRIMARY KEY, percent INTEGER NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS stops(id INTEGER PRIMARY KEY AUTOINCREMENT, city TEXT NOT NULL, name TEXT NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS reviews(id INTEGER PRIMARY KEY AUTOINCREMENT, bus_id INTEGER NOT NULL REFERENCES buses(id), user_id INTEGER NOT NULL REFERENCES users(id), rating INTEGER NOT NULL, comment TEXT, created_at TEXT NOT NULL, UNIQUE(bus_id,user_id))");
            s.execute("CREATE TABLE IF NOT EXISTS notifications(id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id), message TEXT NOT NULL, created_at TEXT NOT NULL, is_read INTEGER NOT NULL DEFAULT 0)");
            s.execute("CREATE TABLE IF NOT EXISTS wallet_tx(id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id), amount REAL NOT NULL, note TEXT NOT NULL, created_at TEXT NOT NULL)");
            ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next() && rs.getInt(1) == 0) seed(c);
        }
    }

    static final String[][] BUSES = {   // name, type, seats, amenities, fare multiplier*100
        {"SRS Express", "AC Sleeper", "40", "WiFi, Charging, Blanket", "125"},
        {"KPN Travels", "Non-AC Seater", "32", "Charging, Water bottle", "75"},
        {"Orange Tours", "AC Seater", "36", "WiFi, Charging, Snacks", "100"},
        {"VRL Travels", "AC Sleeper", "40", "Blanket, Reading light, Charging", "125"},
        {"Parveen Travels", "AC Seater", "36", "Charging, Water bottle", "100"},
        {"Kallada Travels", "AC Sleeper", "40", "WiFi, Blanket, Snacks", "130"},
        {"IntrCity SmartBus", "AC Seater", "36", "WiFi, Charging, Live tracking", "105"},
        {"Neeta Tours", "Non-AC Seater", "32", "Charging, Water bottle", "70"},
        {"GreenLine Electric", "Electric AC Seater", "36", "Zero emission, WiFi, Charging", "110"},
        {"Zing Nova", "Electric AC Sleeper", "40", "Zero emission, WiFi, Blanket", "135"},
        {"National Travels", "AC Seater", "36", "Charging, Snacks", "95"},
        {"Sharma Transports", "AC Sleeper", "40", "Blanket, Reading light", "120"}};
    static final String[][] ROUTES = {   // src, dst, duration(min), base fare, departures
        {"Chennai", "Bengaluru", "360", "750", "06:30,14:00,22:00"}, {"Chennai", "Coimbatore", "480", "850", "07:00,21:30,23:00"},
        {"Chennai", "Madurai", "520", "900", "20:30,22:00"}, {"Chennai", "Hyderabad", "780", "1400", "17:00,19:30"},
        {"Chennai", "Pondicherry", "180", "350", "06:00,12:00,17:30"}, {"Chennai", "Trichy", "330", "600", "21:00,23:00"},
        {"Bengaluru", "Coimbatore", "360", "700", "08:00,22:30"}, {"Bengaluru", "Hyderabad", "600", "1200", "19:00,21:00"},
        {"Bengaluru", "Kochi", "640", "1300", "18:30,20:30"}, {"Coimbatore", "Kochi", "270", "450", "07:30,15:00"},
        {"Madurai", "Coimbatore", "270", "500", "06:00,16:30"}, {"Hyderabad", "Vijayawada", "360", "600", "08:00,22:00"}};
    static final String[][] STOPS = {
        {"Chennai", "Koyambedu CMBT", "Guindy", "Tambaram"}, {"Bengaluru", "Majestic", "Silk Board", "Electronic City"},
        {"Coimbatore", "Gandhipuram", "Singanallur", "Ukkadam"}, {"Madurai", "Mattuthavani", "Periyar", "Thirumangalam"},
        {"Hyderabad", "MGBS", "Ameerpet", "LB Nagar"}, {"Pondicherry", "New Bus Stand", "White Town", "Muthialpet"},
        {"Trichy", "Chatram Stand", "Central Bus Stand", "Thillai Nagar"}, {"Kochi", "Vyttila Hub", "Edappally", "Ernakulam South"},
        {"Vijayawada", "Pandit Nehru Stand", "Benz Circle", "Governorpet"}};

    static void seed(Connection c) throws SQLException {
        exec(c, "INSERT INTO users(username,password,role,wallet) VALUES('admin',?,'ADMIN',5000)", hash("admin123"));
        String[] demo = {"priya", "arjun", "meena"};
        for (String u : demo) exec(c, "INSERT INTO users(username,password,role,wallet) VALUES(?,?,'USER',1000)", u, hash("demo123"));
        exec(c, "INSERT INTO coupons VALUES('WELCOME10',10),('SAVE20',20),('FESTIVE15',15)");
        for (String[] s : STOPS) for (int i = 1; i < s.length; i++) exec(c, "INSERT INTO stops(city,name) VALUES(?,?)", s[0], s[i]);
        for (String[] b : BUSES) exec(c, "INSERT INTO buses(name,type,total_seats,ladies_rows,amenities) VALUES(?,?,?,2,?)", b[0], b[1], Integer.parseInt(b[2]), b[3]);
        int n = 0;
        try (PreparedStatement p = c.prepareStatement("INSERT INTO trips(bus_id,source,dest,travel_date,depart_time,duration_min,fare) VALUES(?,?,?,?,?,?,?)")) {
            for (int d = 0; d < 10; d++)
                for (String[] r : ROUTES)
                    for (int dir = 0; dir < 2; dir++)
                        for (String t : r[4].split(",")) {
                            int bi = n++ % BUSES.length;
                            double fare = Math.round(Double.parseDouble(r[3]) * Integer.parseInt(BUSES[bi][4]) / 100.0 / 10.0) * 10.0;
                            p.setInt(1, bi + 1); p.setString(2, dir == 0 ? r[0] : r[1]); p.setString(3, dir == 0 ? r[1] : r[0]);
                            p.setString(4, LocalDate.now().plusDays(d).toString()); p.setString(5, t); p.setInt(6, Integer.parseInt(r[2])); p.setDouble(7, fare);
                            p.addBatch();
                        }
            p.executeBatch();
        }
        // demo reviews
        String[] cm = {"Clean bus and on time.", "Comfortable seats, friendly staff.", "Smooth ride, would book again.", "Good value for the price.", "AC was a bit cold but great trip.", "Driver was careful and punctual."};
        Random rnd = new Random(7);
        for (int b = 1; b <= BUSES.length; b++)
            for (int u = 2; u <= 4; u++)
                if (rnd.nextInt(4) != 0) exec(c, "INSERT INTO reviews(bus_id,user_id,rating,comment,created_at) VALUES(?,?,?,?,?)", b, u, 3 + rnd.nextInt(3), cm[rnd.nextInt(cm.length)], now());
        // demo bookings so the seat maps look alive
        String[] names = {"Karthik", "Divya", "Suresh", "Anitha", "Rahul", "Lakshmi", "Vikram", "Deepa", "Manoj", "Sneha", "Ravi", "Nithya", "Arun", "Kavya"};
        List<Object[]> trips = rows(c, "SELECT t.id,t.fare,b.total_seats,b.ladies_rows FROM trips t JOIN buses b ON b.id=t.bus_id WHERE t.travel_date<=? ORDER BY t.id", LocalDate.now().plusDays(3).toString());
        for (Object[] t : trips) {
            if (rnd.nextInt(100) >= 45) continue;
            int total = I(t[2]), lady = I(t[3]), cnt = 3 + rnd.nextInt(Math.max(1, total / 3));
            Set<Integer> used = new HashSet<>();
            String ref = "BG" + (100000 + rnd.nextInt(900000));
            while (used.size() < cnt) used.add(1 + rnd.nextInt(total));
            for (int seat : used) {
                boolean ladies = (seat - 1) / 4 < lady; boolean female = ladies || rnd.nextBoolean();
                exec(c, "INSERT INTO bookings(ref,trip_id,user_id,seat_no,passenger,age,gender,amount,booked_at,pay_method) VALUES(?,?,?,?,?,?,?,?,?,'UPI')",
                        ref + (seat % 3), I(t[0]), 2 + rnd.nextInt(3), seat, names[rnd.nextInt(names.length)], 20 + rnd.nextInt(40), female ? "Female" : "Male", D(t[1]), now());
            }
        }
        notify(c, 1, "Welcome to BusGo admin console. Demo data has been loaded.");
    }

    // ================= users, wallet, notifications =================
    static Object[] login(String u, String p) throws SQLException {
        List<Object[]> r = rows("SELECT id,username,role FROM users WHERE username=? AND password=?", u, hash(p));
        return r.isEmpty() ? null : new Object[]{I(r.get(0)[0]), r.get(0)[1], r.get(0)[2]};
    }

    static void register(String u, String p) throws SQLException {
        try (Connection c = get()) {
            c.setAutoCommit(false);
            try {
                exec(c, "INSERT INTO users(username,password,role,wallet) VALUES(?,?,'USER',0)", u, hash(p));
                int id = I(rows(c, "SELECT id FROM users WHERE username=?", u).get(0)[0]);
                credit(c, id, 500, "Welcome bonus");
                notify(c, id, "Welcome to BusGo, " + u + "! Rs. 500 has been added to your wallet as a welcome bonus.");
                c.commit();
            } catch (SQLException e) { c.rollback(); throw new SQLException("Username already taken."); }
        }
    }

    static void notify(Connection c, int uid, String msg) throws SQLException { exec(c, "INSERT INTO notifications(user_id,message,created_at) VALUES(?,?,?)", uid, msg, now()); }

    static void credit(Connection c, int uid, double amt, String note) throws SQLException {
        exec(c, "UPDATE users SET wallet=wallet+? WHERE id=?", amt, uid);
        exec(c, "INSERT INTO wallet_tx(user_id,amount,note,created_at) VALUES(?,?,?,?)", uid, amt, note, now());
    }

    static double wallet(int uid) throws SQLException { return D(rows("SELECT wallet FROM users WHERE id=?", uid).get(0)[0]); }

    static void topUp(int uid, double amt) throws SQLException {
        try (Connection c = get()) {
            c.setAutoCommit(false);
            credit(c, uid, amt, "Wallet top-up"); notify(c, uid, String.format("Rs. %.2f added to your wallet.", amt));
            c.commit();
        }
    }
    static List<Object[]> walletTx(int uid) throws SQLException { return rows("SELECT created_at,note,amount FROM wallet_tx WHERE user_id=? ORDER BY id DESC LIMIT 100", uid); }
    static List<Object[]> notifications(int uid) throws SQLException { return rows("SELECT id,message,created_at,is_read FROM notifications WHERE user_id=? ORDER BY id DESC LIMIT 100", uid); }
    static int unread(int uid) throws SQLException { return I(rows("SELECT COUNT(*) FROM notifications WHERE user_id=? AND is_read=0", uid).get(0)[0]); }
    static void markAllRead(int uid) throws SQLException { try (Connection c = get()) { exec(c, "UPDATE notifications SET is_read=1 WHERE user_id=?", uid); } }

    // ================= search & seats =================
    static List<String> cities() throws SQLException {
        List<String> l = new ArrayList<>();
        for (Object[] r : rows("SELECT source FROM trips UNION SELECT dest FROM trips ORDER BY 1")) l.add((String) r[0]);
        return l;
    }
    static List<String> stops(String city) throws SQLException {
        List<String> l = new ArrayList<>();
        for (Object[] r : rows("SELECT name FROM stops WHERE city=? ORDER BY id", city)) l.add((String) r[0]);
        if (l.isEmpty()) l.add(city + " Bus Stand");
        return l;
    }

    /** cols: 0 tripId, 1 bus, 2 type, 3 depart, 4 fare, 5 seatsLeft, 6 amenities, 7 busId, 8 avgRating, 9 reviewCount, 10 durationMin */
    static List<Object[]> searchTrips(String src, String dst, String date) throws SQLException {
        return rows("SELECT t.id,b.name,b.type,t.depart_time,t.fare,b.total_seats-(SELECT COUNT(*) FROM bookings k WHERE k.trip_id=t.id AND k.status='CONFIRMED'),"
                + "b.amenities,b.id,COALESCE((SELECT AVG(rating) FROM reviews r WHERE r.bus_id=b.id),0),(SELECT COUNT(*) FROM reviews r WHERE r.bus_id=b.id),t.duration_min "
                + "FROM trips t JOIN buses b ON b.id=t.bus_id WHERE t.source=? AND t.dest=? AND t.travel_date=? AND t.status='ACTIVE' ORDER BY t.depart_time", src, dst, date);
    }

    /** @return {totalSeats, ladiesRows} */
    static int[] busInfo(int tripId) throws SQLException {
        List<Object[]> r = rows("SELECT b.total_seats,b.ladies_rows FROM trips t JOIN buses b ON b.id=t.bus_id WHERE t.id=?", tripId);
        return r.isEmpty() ? new int[]{0, 0} : new int[]{I(r.get(0)[0]), I(r.get(0)[1])};
    }
    static Set<Integer> bookedSeats(int tripId) throws SQLException {
        Set<Integer> s = new HashSet<>();
        for (Object[] r : rows("SELECT seat_no FROM bookings WHERE trip_id=? AND status='CONFIRMED'", tripId)) s.add(I(r[0]));
        return s;
    }
    /** @return discount percent, -1 if invalid, 0 if blank */
    static int couponPercent(String code) throws SQLException {
        if (code == null || code.isBlank()) return 0;
        List<Object[]> r = rows("SELECT percent FROM coupons WHERE code=?", code.trim().toUpperCase());
        return r.isEmpty() ? -1 : I(r.get(0)[0]);
    }

    // ================= booking (one transaction: seats + wallet + notification) =================
    static String book(int tripId, int userId, List<Integer> seats, List<String[]> pax, String coupon, String method, String boarding, String dropping) throws SQLException {
        int pct = Math.max(0, couponPercent(coupon));
        String ref = "BG" + (100000 + new Random().nextInt(900000));
        try (Connection c = get()) {
            c.setAutoCommit(false);
            try {
                List<Object[]> t = rows(c, "SELECT t.fare,b.ladies_rows,t.source,t.dest,t.travel_date,t.depart_time,t.status FROM trips t JOIN buses b ON b.id=t.bus_id WHERE t.id=?", tripId);
                if (t.isEmpty() || !"ACTIVE".equals(t.get(0)[6])) throw new SQLException("This trip is no longer available.");
                int lady = I(t.get(0)[1]);
                for (int i = 0; i < seats.size(); i++)
                    if ((seats.get(i) - 1) / 4 < lady && !"Female".equals(pax.get(i)[2])) throw new SQLException("Seat " + seats.get(i) + " is reserved for women passengers.");
                double each = D(t.get(0)[0]) * (100 - pct) / 100.0, total = each * seats.size();
                if ("WALLET".equals(method)) {
                    double bal = D(rows(c, "SELECT wallet FROM users WHERE id=?", userId).get(0)[0]);
                    if (bal < total) throw new SQLException("Insufficient wallet balance.");
                    credit(c, userId, -total, "Ticket " + ref);
                }
                for (int i = 0; i < seats.size(); i++)
                    exec(c, "INSERT INTO bookings(ref,trip_id,user_id,seat_no,passenger,age,gender,amount,booked_at,pay_method,boarding,dropping) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                            ref, tripId, userId, seats.get(i), pax.get(i)[0], Integer.parseInt(pax.get(i)[1]), pax.get(i)[2], each, now(), method, boarding, dropping);
                notify(c, userId, String.format("Booking %s confirmed: %s to %s on %s at %s, seat(s) %s. Paid Rs. %.2f via %s.", ref, t.get(0)[2], t.get(0)[3], t.get(0)[4], t.get(0)[5], seats, total, method));
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw new SQLException(e.getMessage() != null && e.getMessage().contains("UNIQUE") ? "One or more seats were just booked by someone else. Please refresh and pick again." : e.getMessage());
            }
        }
        return ref;
    }

    static final String BOOKING_COLS = "k.id,k.ref,t.source||' -> '||t.dest,t.travel_date,t.depart_time,k.seat_no,k.passenger,k.amount,k.status,k.refund ";
    static List<Object[]> myBookings(int userId) throws SQLException {
        return rows("SELECT " + BOOKING_COLS + "FROM bookings k JOIN trips t ON t.id=k.trip_id WHERE k.user_id=? ORDER BY k.id DESC", userId);
    }
    static List<Object[]> findByRef(String ref, int userId) throws SQLException {
        return rows("SELECT " + BOOKING_COLS + "FROM bookings k JOIN trips t ON t.id=k.trip_id WHERE k.user_id=? AND UPPER(k.ref) LIKE ? ORDER BY k.id DESC", userId, "%" + ref.trim().toUpperCase() + "%");
    }

    /** cols: 0 seat, 1 passenger, 2 age, 3 gender, 4 source, 5 dest, 6 date, 7 time, 8 bus, 9 type, 10 amount, 11 boarding, 12 dropping, 13 booked_at, 14 pay_method, 15 duration */
    static List<Object[]> ticketRows(String ref, int userId) throws SQLException {
        return rows("SELECT k.seat_no,k.passenger,k.age,k.gender,t.source,t.dest,t.travel_date,t.depart_time,b.name,b.type,k.amount,k.boarding,k.dropping,k.booked_at,k.pay_method,t.duration_min "
                + "FROM bookings k JOIN trips t ON t.id=k.trip_id JOIN buses b ON b.id=t.bus_id WHERE k.ref=? AND k.user_id=? AND k.status='CONFIRMED' ORDER BY k.seat_no", ref, userId);
    }

    /** Refund policy: 24h+ = 90%, 6-24h = 50%, under 6h = 0%. Refund goes to the BusGo wallet instantly. */
    static double cancel(int bookingId, int userId) throws SQLException {
        try (Connection c = get()) {
            c.setAutoCommit(false);
            try {
                List<Object[]> r = rows(c, "SELECT k.amount,k.status,t.travel_date,t.depart_time,k.ref,k.seat_no FROM bookings k JOIN trips t ON t.id=k.trip_id WHERE k.id=? AND k.user_id=?", bookingId, userId);
                if (r.isEmpty()) throw new SQLException("Booking not found.");
                if (!"CONFIRMED".equals(r.get(0)[1])) throw new SQLException("Booking is already cancelled.");
                LocalDateTime dep = LocalDateTime.of(LocalDate.parse((String) r.get(0)[2]), LocalTime.parse((String) r.get(0)[3]));
                Duration left = Duration.between(LocalDateTime.now(), dep);
                if (left.isNegative()) throw new SQLException("Bus has already departed.");
                double amt = D(r.get(0)[0]), refund = amt * (left.toHours() >= 24 ? 0.90 : left.toHours() >= 6 ? 0.50 : 0.0);
                exec(c, "UPDATE bookings SET status='CANCELLED', refund=? WHERE id=?", refund, bookingId);
                if (refund > 0) credit(c, userId, refund, "Refund " + r.get(0)[4] + " seat " + r.get(0)[5]);
                notify(c, userId, String.format("Seat %s of booking %s cancelled. Refund of Rs. %.2f credited to your wallet.", r.get(0)[5], r.get(0)[4], refund));
                c.commit();
                return refund;
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    // ================= reviews =================
    static void addReview(int userId, int busId, int rating, String comment) throws SQLException {
        try (Connection c = get()) { exec(c, "INSERT OR REPLACE INTO reviews(bus_id,user_id,rating,comment,created_at) VALUES(?,?,?,?,?)", busId, userId, rating, comment, now()); }
    }
    static List<Object[]> reviews(int busId) throws SQLException {
        return rows("SELECT u.username,r.rating,r.comment,r.created_at FROM reviews r JOIN users u ON u.id=r.user_id WHERE r.bus_id=? ORDER BY r.id DESC LIMIT 50", busId);
    }
    /** @return {busId, busName} of a booking owned by the user */
    static Object[] busOfBooking(int bookingId, int userId) throws SQLException {
        List<Object[]> r = rows("SELECT b.id,b.name FROM bookings k JOIN trips t ON t.id=k.trip_id JOIN buses b ON b.id=t.bus_id WHERE k.id=? AND k.user_id=?", bookingId, userId);
        return r.isEmpty() ? null : new Object[]{I(r.get(0)[0]), r.get(0)[1]};
    }

    // ================= admin =================
    static void addBus(String name, String type, int seats, int ladiesRows, String amenities) throws SQLException {
        try (Connection c = get()) { exec(c, "INSERT INTO buses(name,type,total_seats,ladies_rows,amenities) VALUES(?,?,?,?,?)", name, type, seats, ladiesRows, amenities); }
    }
    static List<String> buses() throws SQLException {
        List<String> l = new ArrayList<>();
        for (Object[] r : rows("SELECT id,name,type,total_seats FROM buses ORDER BY id")) l.add(r[0] + " - " + r[1] + " (" + r[2] + ", " + r[3] + " seats)");
        return l;
    }
    static void addTrips(int busId, String src, String dst, String date, String time, double fare, int durationMin, int days) throws SQLException {
        try (Connection c = get()) {
            c.setAutoCommit(false);
            for (int d = 0; d < days; d++)
                exec(c, "INSERT INTO trips(bus_id,source,dest,travel_date,depart_time,duration_min,fare) VALUES(?,?,?,?,?,?,?)", busId, src, dst, LocalDate.parse(date).plusDays(d).toString(), time, durationMin, fare);
            c.commit();
        }
    }
    /** cols: 0 id, 1 route, 2 date, 3 time, 4 bus, 5 fare, 6 sold, 7 status */
    static List<Object[]> adminTrips(boolean includePast) throws SQLException {
        return rows("SELECT t.id,t.source||' -> '||t.dest,t.travel_date,t.depart_time,b.name,t.fare,(SELECT COUNT(*) FROM bookings k WHERE k.trip_id=t.id AND k.status='CONFIRMED'),t.status "
                + "FROM trips t JOIN buses b ON b.id=t.bus_id WHERE (?=1 OR t.travel_date>=?) ORDER BY t.travel_date,t.depart_time,t.id LIMIT 400", includePast ? 1 : 0, LocalDate.now().toString());
    }
    static void updateTrip(int id, String date, String time, double fare) throws SQLException {
        try (Connection c = get()) {
            c.setAutoCommit(false);
            List<Object[]> old = rows(c, "SELECT travel_date,depart_time,source,dest FROM trips WHERE id=?", id);
            exec(c, "UPDATE trips SET travel_date=?, depart_time=?, fare=? WHERE id=?", date, time, fare, id);
            if (!old.get(0)[0].equals(date) || !old.get(0)[1].equals(time))
                for (Object[] u : rows(c, "SELECT DISTINCT user_id FROM bookings WHERE trip_id=? AND status='CONFIRMED'", id))
                    notify(c, I(u[0]), "Your trip " + old.get(0)[2] + " to " + old.get(0)[3] + " has been rescheduled to " + date + " at " + time + ".");
            c.commit();
        }
    }
    /** Cancels the trip, refunds every confirmed booking 100% to wallets and notifies passengers. @return seats refunded */
    static int cancelTrip(int id) throws SQLException {
        try (Connection c = get()) {
            c.setAutoCommit(false);
            try {
                List<Object[]> t = rows(c, "SELECT source,dest,travel_date,depart_time FROM trips WHERE id=?", id);
                List<Object[]> bs = rows(c, "SELECT id,user_id,amount,ref FROM bookings WHERE trip_id=? AND status='CONFIRMED'", id);
                Map<Integer, Double> perUser = new LinkedHashMap<>();
                for (Object[] b : bs) {
                    exec(c, "UPDATE bookings SET status='CANCELLED', refund=? WHERE id=?", D(b[2]), I(b[0]));
                    credit(c, I(b[1]), D(b[2]), "Full refund - trip cancelled (" + b[3] + ")");
                    perUser.merge(I(b[1]), D(b[2]), Double::sum);
                }
                for (Map.Entry<Integer, Double> e : perUser.entrySet())
                    notify(c, e.getKey(), String.format("Trip %s to %s on %s %s was cancelled by the operator. Rs. %.2f has been refunded to your wallet.", t.get(0)[0], t.get(0)[1], t.get(0)[2], t.get(0)[3], e.getValue()));
                exec(c, "UPDATE trips SET status='CANCELLED' WHERE id=?", id);
                c.commit();
                return bs.size();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }
    static void deleteTrip(int id) throws SQLException {
        try (Connection c = get()) {
            if (I(rows(c, "SELECT COUNT(*) FROM bookings WHERE trip_id=?", id).get(0)[0]) > 0) throw new SQLException("This trip has bookings. Cancel it instead of deleting.");
            exec(c, "DELETE FROM trips WHERE id=?", id);
        }
    }
    /** cols: 0 seat, 1 passenger, 2 age, 3 gender, 4 boarding, 5 dropping, 6 ref, 7 bookedBy */
    static List<Object[]> manifest(int tripId) throws SQLException {
        return rows("SELECT k.seat_no,k.passenger,k.age,k.gender,k.boarding,k.dropping,k.ref,u.username FROM bookings k JOIN users u ON u.id=k.user_id WHERE k.trip_id=? AND k.status='CONFIRMED' ORDER BY k.seat_no", tripId);
    }
    /** cols: 0 ref, 1 passenger, 2 route, 3 date, 4 seat, 5 status, 6 amount, 7 bookedBy */
    static List<Object[]> searchBookings(String q) throws SQLException {
        String like = "%" + q.trim().toUpperCase() + "%";
        return rows("SELECT k.ref,k.passenger,t.source||' -> '||t.dest,t.travel_date,k.seat_no,k.status,k.amount,u.username FROM bookings k JOIN trips t ON t.id=k.trip_id JOIN users u ON u.id=k.user_id "
                + "WHERE UPPER(k.ref) LIKE ? OR UPPER(k.passenger) LIKE ? OR UPPER(u.username) LIKE ? ORDER BY k.id DESC LIMIT 200", like, like, like);
    }
    static List<Object[]> routeReport() throws SQLException {
        return rows("SELECT t.source||' -> '||t.dest, SUM(k.status='CONFIRMED'), SUM(CASE WHEN k.status='CONFIRMED' THEN k.amount ELSE k.amount-k.refund END) "
                + "FROM bookings k JOIN trips t ON t.id=k.trip_id GROUP BY 1 ORDER BY 3 DESC LIMIT 12");
    }
}
