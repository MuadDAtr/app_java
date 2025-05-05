import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class ReservationWindow extends JFrame {

    // Elementy interfejsu
    private JLabel[][] deskLabels = new JLabel[4][4];
    private JTextField nameEdit;
    private JSpinner deskSpin;
    private JSpinner hourSpin;
    private JButton reserveBtn;
    private JButton cancelBtn;

    public ReservationWindow() {
        setTitle("Program do rezerwacji biurek");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 600);
        setLocationRelativeTo(null);

        // Główny panel z układem pionowym
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Tytuł i siatka rezerwacyjna
        JLabel gridTitle = new JLabel("Rozkład biurek");
        gridTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        gridTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(gridTitle);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel deskGridPanel = new JPanel(new GridLayout(4, 4, 5, 5));
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                deskLabels[i][j] = new JLabel("0", SwingConstants.CENTER);
                deskLabels[i][j].setBorder(BorderFactory.createLineBorder(Color.BLACK));
                deskLabels[i][j].setPreferredSize(new Dimension(50, 50));
                deskGridPanel.add(deskLabels[i][j]);
            }
        }
        mainPanel.add(deskGridPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Formularz wprowadzania danych: nazwisko, numer biurka, godzina
        JPanel formPanel = new JPanel(new FlowLayout());
        formPanel.add(new JLabel("Nazwisko:"));
        nameEdit = new JTextField(10);
        formPanel.add(nameEdit);

        formPanel.add(new JLabel("Biurko (1-16):"));
        deskSpin = new JSpinner(new SpinnerNumberModel(1, 1, 16, 1));
        formPanel.add(deskSpin);

        formPanel.add(new JLabel("Godzina (0-23):"));
        hourSpin = new JSpinner(new SpinnerNumberModel(0, 0, 23, 1));
        formPanel.add(hourSpin);
        mainPanel.add(formPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Panel przycisków: rezerwuj oraz anuluj rezerwację
        JPanel buttonPanel = new JPanel(new FlowLayout());
        reserveBtn = new JButton("Rezerwuj");
        cancelBtn = new JButton("Anuluj rezerwację");
        buttonPanel.add(reserveBtn);
        buttonPanel.add(cancelBtn);
        mainPanel.add(buttonPanel);

        add(mainPanel);

        // Podłączenie zdarzeń przycisków
        reserveBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleReservation();
            }
        });
        cancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCancellation();
            }
        });
    }

    // Metoda obsługująca rezerwację
    private void handleReservation() {
        String name = nameEdit.getText().trim();
        int deskNumber = (Integer) deskSpin.getValue();
        int hour = (Integer) hourSpin.getValue();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Wprowadź swoje nazwisko!", "Błąd", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!poprawnaGodzina(hour)) {
            JOptionPane.showMessageDialog(this, "Podano niepoprawną godzinę!", "Błąd", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Obliczenie numeru pliku – np. "5.txt" dla biurka nr 5
        String deskFile = deskNumber + ".txt";

        boolean success = reserveDeskHour(deskFile, hour, name);
        if (success) {
            JOptionPane.showMessageDialog(this, "Rezerwacja powiodła się!", "Sukces", JOptionPane.INFORMATION_MESSAGE);
            // Aktualizacja siatki rezerwacji: ustawiamy nazwisko w odpowiedniej komórce
            int row = (deskNumber - 1) / 4;
            int col = (deskNumber - 1) % 4;
            deskLabels[row][col].setText(name);
        } else {
            JOptionPane.showMessageDialog(this, "Wybrana godzina jest już zajęta!", "Błąd", JOptionPane.WARNING_MESSAGE);
        }
    }

    // Metoda obsługująca anulowanie rezerwacji
    private void handleCancellation() {
        String name = nameEdit.getText().trim();
        int deskNumber = (Integer) deskSpin.getValue();
        int hour = (Integer) hourSpin.getValue();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Wprowadź swoje nazwisko!", "Błąd", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!poprawnaGodzina(hour)) {
            JOptionPane.showMessageDialog(this, "Podano niepoprawną godzinę!", "Błąd", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String deskFile = deskNumber + ".txt";
        boolean cancelled = cancelDeskReservation(deskFile, hour, name);
        if (cancelled) {
            JOptionPane.showMessageDialog(this, "Rezerwacja została anulowana.", "Anulowano", JOptionPane.INFORMATION_MESSAGE);
            int row = (deskNumber - 1) / 4;
            int col = (deskNumber - 1) % 4;
            deskLabels[row][col].setText("0");
        } else {
            JOptionPane.showMessageDialog(this, "Nie znaleziono rezerwacji dla podanych danych!", "Błąd", JOptionPane.WARNING_MESSAGE);
        }
    }

    // Funkcja sprawdzająca, czy podana godzina mieści się w zakresie 0-23.
    public static boolean poprawnaGodzina(int h) {
        return h >= 0 && h < 24;
    }

    // Funkcja dokonująca rezerwacji godziny w pliku dla wybranego biurka.
    public static boolean reserveDeskHour(String deskFile, int hour, String name) {
        String[] hours = new String[24];
        File file = new File(deskFile);

        // Jeśli plik nie istnieje, tworzymy go z domyślnymi wartościami ("0")
        if (!file.exists()) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                for (int i = 0; i < 24; i++) {
                    pw.println("0");
                    hours[i] = "0";
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                for (int i = 0; i < 24; i++) {
                    String line = br.readLine();
                    hours[i] = (line == null) ? "0" : line;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // Rezerwacja nie powiodła się, jeśli wybrana godzina jest zajęta
        if (!hours[hour].equals("0")) {
            return false;
        } else {
            hours[hour] = name;
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                for (int i = 0; i < 24; i++) {
                    pw.println(hours[i]);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }

    // Funkcja anulująca rezerwację godziny.
    public static boolean cancelDeskReservation(String deskFile, int hour, String name) {
        String[] hours = new String[24];
        File file = new File(deskFile);
        if (!file.exists()) {
            return false;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            for (int i = 0; i < 24; i++) {
                String line = br.readLine();
                hours[i] = (line == null) ? "0" : line;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (!hours[hour].equals(name)) {
            return false;
        } else {
            hours[hour] = "0";
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                for (int i = 0; i < 24; i++) {
                    pw.println(hours[i]);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ReservationWindow window = new ReservationWindow();
            window.setVisible(true);
        });
    }
}
