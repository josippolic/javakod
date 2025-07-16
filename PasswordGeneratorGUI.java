import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class PasswordGeneratorGUI extends JFrame {
    private static final String COUNT_FILE = "password_counts.txt";
    private static final String LOG_FILE = "passwords5.txt";

    private static final String JOHN_LIST = "/home/korisnik/usr/share/wordlists/john.lst";
    private static final String WIFITE_LIST = "/home/korisnik/usr/share/wordlists/wifite.txt";
    private static final String ROCKYOU_LIST = "/home/korisnik/usr/share/wordlists/rockyou.txt";
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ@Ł!&#$/<>*ł?£&{}[]€;:-_÷^";

    private JTextArea outputArea;
    private JComboBox<String> wordlistBox;
    private JComboBox<String> modeBox;

    private Map<String, Integer> passwordCounts = new HashMap<>();
    private Set<String> blacklist = new HashSet<>();
    private Random random = new Random();

    public PasswordGeneratorGUI() {
        setTitle("Password Generator");
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        loadPasswordCounts();

        // GUI
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        wordlistBox = new JComboBox<>(new String[]{
            "john.lst", "wifite.txt", "rockyou.txt", "svi (sve liste)"
        });
        wordlistBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, wordlistBox.getPreferredSize().height));

        modeBox = new JComboBox<>(new String[]{
            "Ručno generiraj lozinku", 
            "Automatski generiraj (izbjegni wordlistu)", 
            "Potpuno unikatna lozinka"
        });
        modeBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, modeBox.getPreferredSize().height));

        JButton generateButton = new JButton("Generiraj lozinku");
        JButton resetButton = new JButton("Resetiraj");
        JButton exitButton = new JButton("Izlaz");

        // Gumbi u stupcu, centrirani
        JPanel buttonColumnPanel = new JPanel();
        buttonColumnPanel.setLayout(new BoxLayout(buttonColumnPanel, BoxLayout.Y_AXIS));

        generateButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        resetButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        buttonColumnPanel.add(generateButton);
        buttonColumnPanel.add(Box.createVerticalStrut(10));
        buttonColumnPanel.add(resetButton);
        buttonColumnPanel.add(Box.createVerticalStrut(10));
        buttonColumnPanel.add(exitButton);

        JPanel centerButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerButtonPanel.add(buttonColumnPanel);

        outputArea = new JTextArea(10, 50);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(outputArea);

        // Dodavanje elemenata u panel
        panel.add(new JLabel("Odaberi wordlist:"));
        panel.add(wordlistBox);
        panel.add(Box.createVerticalStrut(8));
        panel.add(new JLabel("Odaberi način:"));
        panel.add(modeBox);
        panel.add(Box.createVerticalStrut(15));
        panel.add(centerButtonPanel);
        panel.add(Box.createVerticalStrut(15));
        panel.add(scrollPane);

        add(panel, BorderLayout.CENTER);

        // Akcije
        generateButton.addActionListener(e -> generatePassword());
        resetButton.addActionListener(e -> resetForm());
        exitButton.addActionListener(e -> System.exit(0));
    }

    private void loadBlacklist(String selected) {
        blacklist.clear();
        try {
            if (selected.contains("john")) {
                blacklist.addAll(Files.readAllLines(Paths.get(JOHN_LIST)));
            } else if (selected.contains("wifite")) {
                blacklist.addAll(Files.readAllLines(Paths.get(WIFITE_LIST)));
            } else if (selected.contains("rockyou")) {
                blacklist.addAll(Files.readAllLines(Paths.get(ROCKYOU_LIST)));
            } else {
                blacklist.addAll(Files.readAllLines(Paths.get(JOHN_LIST)));
                blacklist.addAll(Files.readAllLines(Paths.get(WIFITE_LIST)));
                blacklist.addAll(Files.readAllLines(Paths.get(ROCKYOU_LIST)));
            }
        } catch (IOException e) {
            outputArea.append("⚠️  Nije moguće učitati wordlistu!\n");
        }
    }

    private String generateRandomPassword(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private double calculateEntropy(int length, int charsetSize) {
        return length * (Math.log(charsetSize) / Math.log(2));
    }

    private String getTimestamp() {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date());
    }

    private void generatePassword() {
        String selectedList = (String) wordlistBox.getSelectedItem();
        String mode = (String) modeBox.getSelectedItem();
        loadBlacklist(selectedList);

        outputArea.setText("");
        String password = "";
        int passlen = 0;
        boolean isUnique = false;

        if (mode.contains("Ručno") || mode.contains("Automatski")) {
            while (true) {
                String input = JOptionPane.showInputDialog(this, "Unesi dužinu lozinke (8-15):");
                if (input == null) return;
                try {
                    passlen = Integer.parseInt(input);
                    if (passlen >= 8 && passlen <= 15) break;
                } catch (Exception ignored) {}
                JOptionPane.showMessageDialog(this, "❌ Dužina mora biti između 8 i 15 znakova!");
            }

            do {
                password = generateRandomPassword(passlen);
            } while (blacklist.contains(password));

        } else if (mode.contains("unikatna")) {
            while (!isUnique) {
                passlen = random.nextInt(8) + 8;
                password = generateRandomPassword(passlen);
                if (!passwordCounts.containsKey(password) && !blacklist.contains(password)) {
                    isUnique = true;
                }
            }
        }

        double entropy = calculateEntropy(passlen, CHARACTERS.length());
        passwordCounts.put(password, passwordCounts.getOrDefault(password, 0) + 1);

        outputArea.append("🔐 Lozinka: " + password + "\n");
        outputArea.append("📊 Entropija: " + String.format("%.2f", entropy) + " bita\n");
        outputArea.append("🔁 Generirana: " + passwordCounts.get(password) + " puta\n");
        outputArea.append("⚠️  Blacklist status: " + (blacklist.contains(password) ? "DA" : "NE") + "\n");

        logPassword(password, entropy);
        savePasswordCounts();
    }

    private void logPassword(String password, double entropy) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            bw.write("---------------------------\n");
            bw.write("Datum: " + getTimestamp() + "\n");
            bw.write("Lozinka: " + password + "\n");
            bw.write("Entropija: " + String.format("%.2f", entropy) + " bita\n");
            bw.write("Broj generiranja: " + passwordCounts.get(password) + "\n");
            bw.write("Blacklist status: " + (blacklist.contains(password) ? "DA" : "NE") + "\n");
            bw.write("-----------------------------\n");
        } catch (IOException e) {
            outputArea.append("⚠️  Ne mogu zapisati u log!\n");
        }
    }

    private void savePasswordCounts() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(COUNT_FILE))) {
            for (Map.Entry<String, Integer> entry : passwordCounts.entrySet()) {
                bw.write(entry.getKey() + " " + entry.getValue() + "\n");
            }
        } catch (IOException e) {
            outputArea.append("⚠️  Ne mogu zapisati brojač!\n");
        }
    }

    private void loadPasswordCounts() {
        try {
            List<String> lines = Files.readAllLines(Paths.get(COUNT_FILE));
            for (String line : lines) {
                String[] parts = line.split(" ");
                if (parts.length == 2) {
                    passwordCounts.put(parts[0], Integer.parseInt(parts[1]));
                }
            }
        } catch (IOException ignored) {}
    }

    private void resetForm() {
        wordlistBox.setSelectedIndex(0);
        modeBox.setSelectedIndex(0);
        outputArea.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PasswordGeneratorGUI().setVisible(true));
    }
}
