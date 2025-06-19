import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;

public class InheritanceDBGUI {
    private JFrame frame;
    private JTextField nameField, ageField, addressField, idField;
    private JComboBox<String> typeBox;
    private JTextArea displayArea;
    private JTable dataTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;

    private Color pinkBackground = new Color(255, 228, 240);
    private Color buttonPink = new Color(255, 105, 180);
    private Color lightPink = new Color(255, 182, 193);

    private Connection connectDB() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/akademik", "root", "");
    }

    public InheritanceDBGUI() {
        frame = new JFrame("Data Mahasiswa & Dosen");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 560);
        frame.setLayout(new BorderLayout());

        setupUI();
        tampilkanData();

        frame.setVisible(true);
    }

    private void setupUI() {
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(pinkBackground);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        nameField = new JTextField(15);
        ageField = new JTextField(15);
        addressField = new JTextField(15);
        idField = new JTextField(15);
        typeBox = new JComboBox<>(new String[]{"Mahasiswa", "Dosen"});
        JButton addButton = new JButton("Tambah Data");
        addButton.setBackground(buttonPink);
        addButton.setForeground(Color.WHITE);
        addButton.addActionListener(e -> tambahData());

        gbc.gridx = 0; gbc.gridy = 0; inputPanel.add(new JLabel("Nama:"), gbc);
        gbc.gridx = 1; inputPanel.add(nameField, gbc);
        gbc.gridx = 2; inputPanel.add(new JLabel("Usia:"), gbc);
        gbc.gridx = 3; inputPanel.add(ageField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; inputPanel.add(new JLabel("Alamat:"), gbc);
        gbc.gridx = 1; inputPanel.add(addressField, gbc);
        gbc.gridx = 2; inputPanel.add(new JLabel("Tipe:"), gbc);
        gbc.gridx = 3; inputPanel.add(typeBox, gbc);

        gbc.gridx = 0; gbc.gridy = 2; inputPanel.add(new JLabel("NIM/NIP:"), gbc);
        gbc.gridx = 1; inputPanel.add(idField, gbc);
        gbc.gridx = 2; gbc.gridy = 2; inputPanel.add(addButton, gbc);

        frame.add(inputPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"No", "Nama", "Usia", "Alamat", "Tipe", "NIM/NIP"}, 0);
        dataTable = new JTable(tableModel);
        rowSorter = new TableRowSorter<>(tableModel);
        dataTable.setRowSorter(rowSorter);
        rowSorter.toggleSortOrder(1); // Urutkan berdasarkan nama

        JScrollPane tableScroll = new JScrollPane(dataTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Data:"));

        displayArea = new JTextArea(3, 50);
        displayArea.setFont(new Font("SansSerif", Font.BOLD, 14));
        displayArea.setEditable(false);
        displayArea.setBorder(BorderFactory.createTitledBorder("Status Input"));

        JTextField searchField = new JTextField(30);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterTable(); }
            public void removeUpdate(DocumentEvent e) { filterTable(); }
            public void changedUpdate(DocumentEvent e) { filterTable(); }
            private void filterTable() {
                String text = searchField.getText();
                if (text.trim().length() == 0) rowSorter.setRowFilter(null);
                else rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
            }
        });

        JPanel centerPanel = new JPanel(new BorderLayout());
        JPanel searchPanel = new JPanel();
        searchPanel.add(new JLabel("Cari:"));
        searchPanel.add(searchField);
        centerPanel.add(searchPanel, BorderLayout.NORTH);
        centerPanel.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, displayArea, tableScroll), BorderLayout.CENTER);

        frame.add(centerPanel, BorderLayout.CENTER);
    }

    private void tambahData() {
        String name = nameField.getText().trim();
        String ageText = ageField.getText().trim();
        String address = addressField.getText().trim();
        String idVal = idField.getText().trim();
        String type = (String) typeBox.getSelectedItem();

        if (name.isEmpty() || ageText.isEmpty() || address.isEmpty() || idVal.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Semua field harus diisi.");
            return;
        }

        try {
            int age = Integer.parseInt(ageText);
            Person person = type.equals("Mahasiswa") ? new Student(name, age, address, idVal)
                                                     : new Lecturer(name, age, address, idVal);

            Connection conn = connectDB();

            String checkQuery = type.equals("Mahasiswa")
                    ? "SELECT COUNT(*) FROM student WHERE nim = ?"
                    : "SELECT COUNT(*) FROM lecturer WHERE nip = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, idVal);
            ResultSet checkRs = checkStmt.executeQuery();
            if (checkRs.next() && checkRs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(frame, type + " dengan NIM/NIP tersebut sudah ada!", "Data Duplikat", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String insertPerson = "INSERT INTO person (name, age, address, type) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(insertPerson, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, person.getName());
            ps.setInt(2, person.getAge());
            ps.setString(3, person.getAddress());
            ps.setString(4, person.getType());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int personId = rs.getInt(1);
                String insertChild = person instanceof Student ?
                        "INSERT INTO student (person_id, nim) VALUES (?, ?)" :
                        "INSERT INTO lecturer (person_id, nip) VALUES (?, ?)";

                PreparedStatement psChild = conn.prepareStatement(insertChild);
                psChild.setInt(1, personId);
                psChild.setString(2, person.getId());
                psChild.executeUpdate();
            }

            displayArea.append(person.getType() + ": " + person.getName() + " berhasil disimpan.\n");
            nameField.setText(""); ageField.setText(""); addressField.setText(""); idField.setText("");
            tampilkanData();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Usia harus berupa angka.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage());
        }
    }

    private void tampilkanData() {
        tableModel.setRowCount(0);
        try (Connection conn = connectDB()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT p.id, p.name, p.age, p.address, p.type, s.nim, l.nip " +
                    "FROM person p " +
                    "LEFT JOIN student s ON p.id=s.person_id " +
                    "LEFT JOIN lecturer l ON p.id=l.person_id");
            int no = 1;
            while (rs.next()) {
                String name = rs.getString("name");
                int age = rs.getInt("age");
                String address = rs.getString("address");
                String type = rs.getString("type");
                String id = rs.getString("nim") != null ? rs.getString("nim") : rs.getString("nip");
                tableModel.addRow(new Object[]{no++, name, age, address, type, id});
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Gagal menampilkan data: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null, "Driver JDBC tidak ditemukan.");
            return;
        }
        SwingUtilities.invokeLater(InheritanceDBGUI::new);
    }
}
