import javax.swing.*;
import java.awt.*;
import java.time.YearMonth;
import java.util.Random;

/** Simulated checkout: UPI / Card / Wallet, animated processing + success screens. */
public class PayDialog extends JDialog {
    static final String[] KEYS = {"UPI", "CARD", "WALLET"};
    String result;                               // chosen method, or null if cancelled/failed
    final double amount, balance; String method = "UPI";
    final CardLayout steps = new CardLayout(), forms = new CardLayout();
    final JPanel host = new JPanel(steps), formHost = new JPanel(forms);
    final JTextField upi = Ui.field(22), cardNo = Ui.field(22), cardName = Ui.field(22), exp = Ui.field(6), cvv = Ui.field(5);
    final JLabel err = Ui.label(" ", Font.PLAIN, 12, Ui.BAD), busyTitle = Ui.label(" ", Font.BOLD, 20, Ui.TEXT), busySub = Ui.label(" ", Font.PLAIN, 13, Ui.MUTED);
    final Ui.StatusIcon icon = new Ui.StatusIcon();
    final Ui.Btn pay;

    static String ask(Frame owner, double amount, double balance) { PayDialog d = new PayDialog(owner, amount, balance); d.setVisible(true); return d.result; }

    PayDialog(Frame owner, double amount, double balance) {
        super(owner, "Secure checkout", true);
        this.amount = amount; this.balance = balance;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); setResizable(false);
        pay = new Ui.Btn("Pay " + Ui.money(amount), Ui.ACCENT, Ui.ACCENT_DARK);
        pay.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46)); pay.addActionListener(e -> pay());

        Ui.Grad head = new Ui.Grad(new BorderLayout(), Ui.DEEP, Ui.VIOLET); head.decor = true; head.setBorder(Ui.pad(18, 24, 18, 24));
        JPanel hl = col(); hl.add(Ui.left(Ui.label("SECURE CHECKOUT", Font.BOLD, 11, new Color(0xC7D2FE)))); hl.add(Ui.vgap(4));
        hl.add(Ui.left(Ui.label("Amount to pay", Font.PLAIN, 13, Color.WHITE)));
        head.add(hl, BorderLayout.WEST); head.add(Ui.label(Ui.money(amount), Font.BOLD, 30, Ui.ACCENT), BorderLayout.EAST);

        formHost.setOpaque(false);
        JPanel fu = col(); fu.add(Ui.left(Ui.labeled("UPI ID", upi))); fu.add(Ui.vgap(6)); fu.add(Ui.left(Ui.label("Example: yourname@okbank", Font.PLAIN, 12, Ui.MUTED)));
        JPanel fc = col();
        fc.add(Ui.left(Ui.labeled("CARD NUMBER", cardNo))); fc.add(Ui.vgap(10)); fc.add(Ui.left(Ui.labeled("NAME ON CARD", cardName))); fc.add(Ui.vgap(10));
        JPanel r = new JPanel(new GridLayout(1, 2, 12, 0)); r.setOpaque(false); r.add(Ui.labeled("EXPIRY (MM/YY)", exp)); r.add(Ui.labeled("CVV", cvv));
        fc.add(Ui.left(r)); fc.add(Ui.vgap(8));
        fc.add(Ui.left(Ui.label("Demo: 4111 1111 1111 1111 succeeds, 4000 0000 0000 0002 is declined.", Font.PLAIN, 11, Ui.MUTED)));
        JPanel fw = col();
        fw.add(Ui.left(Ui.label("BusGo Wallet balance", Font.BOLD, 11, Ui.MUTED))); fw.add(Ui.vgap(4));
        fw.add(Ui.left(Ui.label(Ui.money(balance), Font.BOLD, 26, balance >= amount ? Ui.OK : Ui.BAD))); fw.add(Ui.vgap(6));
        fw.add(Ui.left(Ui.label(balance >= amount ? "Enough balance - pay instantly." : "Not enough balance. Add money from the Wallet page or pick UPI / Card.", Font.PLAIN, 12, balance >= amount ? Ui.MUTED : Ui.BAD)));
        formHost.add(top(fu), "0"); formHost.add(top(fc), "1"); formHost.add(top(fw), "2");
        formHost.setPreferredSize(new Dimension(10, 215)); formHost.setMaximumSize(new Dimension(Integer.MAX_VALUE, 215));

        JPanel form = col(); form.setBorder(Ui.pad(18, 24, 20, 24));
        form.add(Ui.left(Ui.label("PAYMENT METHOD", Font.BOLD, 11, Ui.MUTED))); form.add(Ui.vgap(8));
        form.add(Ui.left(BusApp.fixed(Ui.pills(new String[]{"UPI", "Card", "Wallet"}, i -> {
            method = KEYS[i]; forms.show(formHost, String.valueOf(i)); err.setText(" ");
            pay.setEnabled(!(method.equals("WALLET") && balance < amount));
        }))));
        form.add(Ui.vgap(16)); form.add(Ui.left(formHost)); form.add(Ui.vgap(10)); form.add(Ui.left(err)); form.add(Ui.vgap(6)); form.add(Ui.left(pay));
        form.add(Box.createVerticalGlue()); form.add(Ui.vgap(8)); form.add(Ui.left(Ui.label("Simulated payment - no real money is charged.", Font.PLAIN, 11, Ui.MUTED)));

        JPanel busy = new JPanel(new GridBagLayout()); busy.setOpaque(false);
        JPanel bc = col(); icon.setAlignmentX(Component.CENTER_ALIGNMENT); busyTitle.setAlignmentX(Component.CENTER_ALIGNMENT); busySub.setAlignmentX(Component.CENTER_ALIGNMENT);
        bc.setPreferredSize(new Dimension(440, 260)); bc.add(icon); bc.add(Ui.vgap(14)); bc.add(busyTitle); bc.add(Ui.vgap(6)); bc.add(busySub); busy.add(bc);

        host.setBackground(Ui.CARD); host.add(form, "form"); host.add(busy, "busy");
        JPanel root = new JPanel(new BorderLayout()); root.setBackground(Ui.CARD); root.add(head, BorderLayout.NORTH); root.add(host, BorderLayout.CENTER);
        setContentPane(root); setSize(520, 520); setLocationRelativeTo(owner);
    }

    static JPanel top(JPanel p) { JPanel w = new JPanel(new BorderLayout()); w.setOpaque(false); w.add(p, BorderLayout.NORTH); return w; }
    static JPanel col() { JPanel p = new JPanel(); p.setOpaque(false); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); return p; }

    static boolean luhn(String n) {
        int sum = 0; boolean dbl = false;
        for (int i = n.length() - 1; i >= 0; i--) { int d = n.charAt(i) - '0'; if (dbl) { d *= 2; if (d > 9) d -= 9; } sum += d; dbl = !dbl; }
        return sum % 10 == 0;
    }

    String check() {
        if (method.equals("UPI")) return upi.getText().trim().matches("[A-Za-z0-9._-]{2,}@[A-Za-z]{2,}") ? null : "Enter a valid UPI ID, for example name@okbank";
        if (method.equals("WALLET")) return balance >= amount ? null : "Insufficient wallet balance";
        String n = cardNo.getText().replaceAll("\\s", "");
        if (!n.matches("\\d{16}") || !luhn(n)) return "Enter a valid 16-digit card number";
        if (cardName.getText().isBlank()) return "Enter the name on the card";
        String e = exp.getText().trim();
        if (!e.matches("(0[1-9]|1[0-2])/\\d{2}")) return "Expiry must look like 08/28";
        if (YearMonth.of(2000 + Integer.parseInt(e.substring(3)), Integer.parseInt(e.substring(0, 2))).isBefore(YearMonth.now())) return "This card has expired";
        if (!cvv.getText().trim().matches("\\d{3}")) return "CVV must be 3 digits";
        return null;
    }

    void pay() {
        String problem = check();
        if (problem != null) { err.setText(problem); return; }
        err.setText(" "); steps.show(host, "busy"); icon.mode(0);
        busyTitle.setText("Processing secure payment..."); busySub.setText("Please do not close this window");
        javax.swing.Timer t = new javax.swing.Timer(1900, e -> {
            if (!isDisplayable()) return;
            boolean declined = method.equals("CARD") && cardNo.getText().replaceAll("\\s", "").equals("4000000000000002");
            if (declined) {
                icon.mode(2); busyTitle.setText("Payment declined"); busySub.setText("The bank declined this card (simulated). Try another method.");
                javax.swing.Timer back = new javax.swing.Timer(2400, x -> { icon.stop(); steps.show(host, "form"); });
                back.setRepeats(false); back.start();
            } else {
                icon.mode(1); busyTitle.setText("Payment successful"); busySub.setText("Transaction ID  TXN" + (100000000 + new Random().nextInt(900000000)));
                javax.swing.Timer done = new javax.swing.Timer(1900, x -> { icon.stop(); result = method; dispose(); });
                done.setRepeats(false); done.start();
            }
        });
        t.setRepeats(false); t.start();
    }
}
