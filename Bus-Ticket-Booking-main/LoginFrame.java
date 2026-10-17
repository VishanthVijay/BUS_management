import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class LoginFrame extends JFrame {
    boolean register;
    final JTextField user = Ui.field(18);
    final JPasswordField pass = new JPasswordField(18);
    final JLabel title = Ui.label("Welcome back", Font.BOLD, 26, Ui.TEXT),
            sub = Ui.label("Sign in to book your next journey", Font.PLAIN, 13, Ui.MUTED),
            msg = Ui.label(" ", Font.PLAIN, 12, Ui.BAD);
    final Ui.Btn go = new Ui.Btn("Sign in", Ui.PRIMARY, Ui.PRIMARY_DARK),
            toggle = new Ui.Btn("New here? Create an account", new Color(0, 0, 0, 0), Ui.SOFT);

    LoginFrame() {
        setTitle("BusGo - Sign in"); setDefaultCloseOperation(EXIT_ON_CLOSE); setResizable(false);
        toggle.fg = Ui.PRIMARY;
        go.addActionListener(e -> submit()); user.addActionListener(e -> submit()); pass.addActionListener(e -> submit());
        toggle.addActionListener(e -> { mode(!register); say(" ", false); });
        JPanel root = new JPanel(new GridLayout(1, 2));
        root.add(left()); root.add(right());
        setContentPane(root); setSize(940, 580); setLocationRelativeTo(null);
        getRootPane().setDefaultButton(go);
    }

    JPanel left() {
        Ui.Grad g = new Ui.Grad(new GridBagLayout(), Ui.DEEP, Ui.PRIMARY); g.decor = true;
        JPanel box = new JPanel(); box.setOpaque(false); box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(Ui.left(new Ui.BusIcon(320))); box.add(Ui.vgap(22));
        box.add(Ui.left(Ui.label("BusGo", Font.BOLD, 42, Color.WHITE)));
        box.add(Ui.left(Ui.label("Travel smarter. Book in seconds.", Font.PLAIN, 15, new Color(0xA5F3FC)))); box.add(Ui.vgap(20));
        for (String s : new String[]{"Live seat map with window, aisle and ladies seats", "UPI, card or wallet checkout", "E-ticket PDF with QR code", "Instant refunds to your wallet"}) {
            box.add(Ui.left(Ui.label("\u2022  " + s, Font.PLAIN, 13, Color.WHITE))); box.add(Ui.vgap(8));
        }
        g.add(box); return g;
    }

    JPanel right() {
        pass.setFont(Ui.font(Font.PLAIN, 13)); pass.setBorder(Ui.fieldBorder()); pass.setBackground(Ui.FIELD_BG); pass.setForeground(Ui.TEXT); pass.setCaretColor(Ui.TEXT);
        go.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44)); toggle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        JPanel box = new JPanel(); box.setOpaque(false); box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(Ui.left(title)); box.add(Ui.vgap(4)); box.add(Ui.left(sub)); box.add(Ui.vgap(26));
        box.add(Ui.left(Ui.labeled("USERNAME", user))); box.add(Ui.vgap(14));
        box.add(Ui.left(Ui.labeled("PASSWORD", pass))); box.add(Ui.vgap(8));
        box.add(Ui.left(msg)); box.add(Ui.vgap(10));
        box.add(Ui.left(go)); box.add(Ui.vgap(8)); box.add(Ui.left(toggle)); box.add(Ui.vgap(16));
        box.add(Ui.left(Ui.label("Demo logins:  admin / admin123   or   priya / demo123", Font.PLAIN, 11, Ui.MUTED)));
        box.setPreferredSize(new Dimension(350, box.getPreferredSize().height));
        JPanel wrap = new JPanel(new GridBagLayout()); wrap.setBackground(Ui.CARD); wrap.add(box);
        return wrap;
    }

    void mode(boolean reg) {
        register = reg;
        title.setText(reg ? "Create account" : "Welcome back");
        sub.setText(reg ? "Join BusGo and get Rs. 500 wallet bonus" : "Sign in to book your next journey");
        go.setText(reg ? "Create account" : "Sign in");
        toggle.setText(reg ? "Already registered? Sign in" : "New here? Create an account");
    }

    void say(String t, boolean good) { msg.setForeground(good ? Ui.OK : Ui.BAD); msg.setText(t); }

    void submit() {
        String u = user.getText().trim(), p = new String(pass.getPassword());
        try {
            if (u.isEmpty() || p.length() < 4) { say("Enter a username and a password of 4+ characters.", false); return; }
            if (register) { Db.register(u, p); mode(false); say("Account created! Please sign in.", true); return; }
            Object[] r = Db.login(u, p);
            if (r == null) { say("Wrong username or password.", false); return; }
            dispose();
            new BusApp((int) r[0], (String) r[1], (String) r[2]).setVisible(true);
        } catch (SQLException e) { say(e.getMessage(), false); }
    }
}
