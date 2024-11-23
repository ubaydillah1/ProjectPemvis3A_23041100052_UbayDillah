
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.swing.table.DefaultTableModel;
import java.sql.ResultSet;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;


/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author User
 */
public class ContentUser extends javax.swing.JFrame {
    Connection conn;
    private DefaultTableModel loanModel, historyModel;
    ArrayList<Integer> listIdLoans = new ArrayList<>();
    /**
     * Creates new form Content
     */
    public ContentUser() {
        if (Global.UserId == 0) {
            Main login = new Main();
            login.setVisible(true);       
            this.setVisible(false);
            return;
        }
        
        initComponents();
        setResizable(false);
        conn = connection.getConnection();
        System.out.println(Global.UserId);
        
        try {
            String sqlStatus = "SELECT * FROM user WHERE id = ?";
            PreparedStatement psCheck = conn.prepareStatement(sqlStatus);
            psCheck.setInt(1, Global.UserId);
            ResultSet rs = psCheck.executeQuery();
            
            if (rs.next()) {
                String status = rs.getString("status"); 
                String NIK = rs.getString("NIK");
                String name = rs.getString("name");
                String phone = rs.getString("no_telephone");
                int income = rs.getInt("income");
                
                nameText.setText(name);

                if ("pending".equalsIgnoreCase(status)) {
                     tabbedPane.addChangeListener(new ChangeListener() {
                        @Override
                        public void stateChanged(ChangeEvent e) {
                            boolean condition = false;
                            
                            if ((tabbedPane.getSelectedIndex() == 0 || tabbedPane.getSelectedIndex() == 1 || tabbedPane.getSelectedIndex() == 2 || tabbedPane.getSelectedIndex() == 3) 
                                && !condition) {

                                JOptionPane.showMessageDialog(null, 
                                    "Anda tidak memiliki akses ke halaman ini! Tunggu admin verifikasi!", 
                                    "Peringatan", 
                                    JOptionPane.WARNING_MESSAGE);

                                // Kembalikan ke tab Pengaturan (index 2)
                                tabbedPane.setSelectedIndex(4);
                            }
                        }
                    });
                     
                     if (NIK == null || NIK.isEmpty()) {
                        AfterRegisterProfile profile = new AfterRegisterProfile();
                        new Thread(() -> {
                           try {
                               Thread.sleep(100); 
                               SwingUtilities.invokeLater(() -> {
                                   this.setVisible(false);
                                   this.dispose();
                               });
                           } catch (InterruptedException ex) {
                               ex.printStackTrace();
                           }
                       }).start();
                        profile.setVisible(true);
                        return;
                     }
                }
                
                NIKSet.setText(NIK);
                nameSet.setText(name);
                phoneSet.setText(phone);
                incomeSet.setText(Integer.toString(income));  
            } else {
                JOptionPane.showMessageDialog(null, "Pengguna tidak ditemukan.", "Error", JOptionPane.ERROR_MESSAGE);
            }
            
        } catch(SQLException e) {
            System.out.println("Error : " + e.getMessage());
        }
        
        tabbedPane.setSelectedIndex(4);
        setupLoanStructure();
        loadLoanTable();
        setupTypeBox();
        setupHistoryStructure();
        LoadHistoryTable();
    }
    
    private void setupLoanStructure() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Nama");    
        model.addColumn("Pinjaman");
        model.addColumn("Bayar/bulan");
        model.addColumn("Dibayar");
        model.addColumn("Waktu minjam");
        model.addColumn("Tenggat");
        
        this.loanModel = model;
        loanTable.setModel(model);
        loanTable.setEnabled(false);
    }
    
    private void setupHistoryStructure() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Nama");    
        model.addColumn("Dibayar");
        model.addColumn("Status");
        model.addColumn("Tenor");
        model.addColumn("Waktu Bayar");
        
        this.historyModel = model;
        historyTable.setModel(model);
        historyTable.setEnabled(false);
    }
    
    private void loadLoanTable() {
      loanModel.setRowCount(0);

      try {
            String sql = "SELECT loans.name AS loan_name, loans.amount, user.name AS user_name, " +
                         "loans.monthly_payment, loans.paid_month, loans.now_date, loans.due_date " +
                         "FROM loans JOIN user ON loans.user_id = user.id WHERE user.id = ? And loans.status = 'active'";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, Global.UserId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                // Menambahkan baris ke loanModel
                loanModel.addRow(new Object[]{
                    rs.getString("loan_name"),         
                    rs.getInt("amount"),          
                    rs.getInt("monthly_payment"),  
                    rs.getInt("paid_month") + " Bulan",          
                    rs.getDate("now_date"),           
                    rs.getDate("due_date")           
                });
            }
        } catch (SQLException e) {
            System.out.println("Error Load Data Pinjaman: " + e.getMessage());
        }
    }
    
    private void LoadHistoryTable() {
      historyModel.setRowCount(0);
      
      if (historyModel == null) {
        System.out.println("History model belum diinisialisasi.");
        return;
    }

      try {
            String sql = "SELECT l.name, p.now_time, l.paid_month, l.status, p.payment_month , l.tenure_months " +
                         "FROM payments p JOIN loans l ON p.loans_id = l.id WHERE p.user_id = ? ORDER BY p.timeStamp DESC";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, Global.UserId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                historyModel.addRow(new Object[]{
                    rs.getString("l.name"),         
                    rs.getInt("p.payment_month") + " Bulan",          
                    rs.getString("l.status"),  
                    rs.getInt("l.tenure_months"),          
                    rs.getDate("p.now_time"),
                });
            }
        } catch (SQLException e) {
            System.out.println("Error Load Data Pinjaman: " + e.getMessage());
        }
    }
    
    private void setupTypeBox() {
        loanNameBox.removeAllItems();
        listIdLoans.clear();
        
        try {
            String query = "SELECT loans.id, loans.name FROM loans JOIN user ON loans.user_id = user.id WHERE user.id = ? AND loans.status = 'active'";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, Global.UserId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("loans.id");
                String nama = rs.getString("loans.name");
                listIdLoans.add(id);  
                loanNameBox.addItem(nama);
            }
            
        } catch (SQLException e) {
            System.out.println("Error Query Combo box Karyawan" + e.getMessage());
        }
    }
    
    
    public class Global {
        public static int UserId = 0;
    }
    
    private void resetProfile() {
        NIKSet.setText("");
        nameSet.setText("");
        phoneSet.setText("");
        incomeSet.setText("");
    }
    
    private boolean validateTextField(JTextField textField, String errorMessage) {
        if (textField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, errorMessage);
            return false;
        }
        return true;
    }
    
    private void setupBalance() {
         try {
            String sqlBalance = "SELECT balance from user WHERE id = ?";
            PreparedStatement psCheck = conn.prepareStatement(sqlBalance);
            psCheck.setInt(1, Global.UserId);
            ResultSet rs = psCheck.executeQuery();
            
            while(rs.next()) {
                int balance = rs.getInt("balance");
                
                balanceField.setText(Integer.toString(balance));
                
            }
            
        } catch(SQLException e) {
            System.out.println("Error : " + e.getMessage());
        }
    }
    
    public static String getCurrentDate() {
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return currentDate.format(formatter);
    }
    
    public String getLoanDueDate(int monthsToAdd) {
        LocalDate currentDate = LocalDate.now();

        // Menambahkan bulan yang diterima sebagai parameter
        LocalDate dueDate = currentDate.plusMonths(monthsToAdd);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return dueDate.format(formatter);
    }

    
    private void refreshBalance() {
        try {
           String sqlBalance = "SELECT balance FROM user WHERE id = ?";
           PreparedStatement psCheck = conn.prepareStatement(sqlBalance);
           psCheck.setInt(1, Global.UserId);
           ResultSet rs = psCheck.executeQuery();
           
           if (rs.next()) {
               int balance = rs.getInt("balance");
               balanceField.setText(Integer.toString(balance));
           }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        shadowRenderer1 = new raven.tabbed.ShadowRenderer();
        Body = new javax.swing.JPanel();
        tabbedPane = new raven.tabbed.TabbedPaneCustom();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        nameText = new javax.swing.JTextField();
        roundedPanel1 = new costume.RoundedPanel();
        roundedPanel2 = new costume.RoundedPanel();
        balanceField = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        roundedPanel3 = new costume.RoundedPanel();
        jLabel1 = new javax.swing.JLabel();
        depositeField = new javax.swing.JTextField();
        depositeButton = new javax.swing.JButton();
        roundedPanel4 = new costume.RoundedPanel();
        jLabel3 = new javax.swing.JLabel();
        withdrawField = new javax.swing.JTextField();
        withdrawButton = new javax.swing.JButton();
        roundedPanel6 = new costume.RoundedPanel();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        loanTable = new javax.swing.JTable();
        jLabel4 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        roundedPanel11 = new costume.RoundedPanel();
        jLabel13 = new javax.swing.JLabel();
        roundedPanel12 = new costume.RoundedPanel();
        jLabel14 = new javax.swing.JLabel();
        lendName = new javax.swing.JTextField();
        roundedPanel16 = new costume.RoundedPanel();
        jLabel19 = new javax.swing.JLabel();
        lendBox = new javax.swing.JComboBox<>();
        lendButton = new javax.swing.JButton();
        roundedPanel14 = new costume.RoundedPanel();
        jLabel16 = new javax.swing.JLabel();
        lendAmount = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        roundedPanel13 = new costume.RoundedPanel();
        jLabel15 = new javax.swing.JLabel();
        roundedPanel17 = new costume.RoundedPanel();
        jLabel20 = new javax.swing.JLabel();
        loanNameBox = new javax.swing.JComboBox<>();
        paymentButton = new javax.swing.JButton();
        roundedPanel18 = new costume.RoundedPanel();
        jumlahLabel = new javax.swing.JLabel();
        amountPaidField = new javax.swing.JTextField();
        roundedPanel19 = new costume.RoundedPanel();
        jLabel22 = new javax.swing.JLabel();
        monthBox = new javax.swing.JComboBox<>();
        jPanel2 = new javax.swing.JPanel();
        roundedPanel15 = new costume.RoundedPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        historyTable = new javax.swing.JTable();
        jLabel5 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        roundedPanel5 = new costume.RoundedPanel();
        jLabel7 = new javax.swing.JLabel();
        roundedPanel7 = new costume.RoundedPanel();
        jLabel8 = new javax.swing.JLabel();
        NIKSet = new javax.swing.JTextField();
        roundedPanel8 = new costume.RoundedPanel();
        jLabel9 = new javax.swing.JLabel();
        nameSet = new javax.swing.JTextField();
        roundedPanel9 = new costume.RoundedPanel();
        jLabel10 = new javax.swing.JLabel();
        phoneSet = new javax.swing.JTextField();
        roundedPanel10 = new costume.RoundedPanel();
        jLabel11 = new javax.swing.JLabel();
        incomeSet = new javax.swing.JTextField();
        jButton5 = new javax.swing.JButton();
        Logout = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new java.awt.CardLayout());

        Body.setBackground(new java.awt.Color(204, 204, 204));
        Body.setPreferredSize(new java.awt.Dimension(900, 767));
        Body.setLayout(new java.awt.BorderLayout());

        tabbedPane.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        tabbedPane.setPreferredSize(new java.awt.Dimension(700, 767));
        tabbedPane.setSelectedColor(new java.awt.Color(0, 204, 102));
        tabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabbedPaneStateChanged(evt);
            }
        });

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setPreferredSize(new java.awt.Dimension(900, 767));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 48)); // NOI18N
        jLabel2.setText("Selamat Datang");

        nameText.setEditable(false);
        nameText.setFont(new java.awt.Font("Segoe UI", 1, 48)); // NOI18N
        nameText.setText("Tuan");
        nameText.setBorder(null);

        roundedPanel1.setBackground(new java.awt.Color(0, 204, 102));
        roundedPanel1.setForeground(new java.awt.Color(0, 204, 102));
        roundedPanel1.setPreferredSize(new java.awt.Dimension(900, 620));
        roundedPanel1.setRoundTopLeft(50);
        roundedPanel1.setRoundTopRight(50);

        roundedPanel2.setForeground(new java.awt.Color(255, 255, 255));
        roundedPanel2.setRoundBottomLeft(24);
        roundedPanel2.setRoundBottomRight(24);
        roundedPanel2.setRoundTopLeft(24);
        roundedPanel2.setRoundTopRight(24);

        balanceField.setEditable(false);
        balanceField.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        balanceField.setText("********");

        jButton1.setBackground(new java.awt.Color(0, 153, 255));
        jButton1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jButton1.setForeground(new java.awt.Color(255, 255, 255));
        jButton1.setText("Lihat Uang");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout roundedPanel2Layout = new javax.swing.GroupLayout(roundedPanel2);
        roundedPanel2.setLayout(roundedPanel2Layout);
        roundedPanel2Layout.setHorizontalGroup(
            roundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel2Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(roundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE)
                    .addComponent(balanceField))
                .addGap(14, 14, 14))
        );
        roundedPanel2Layout.setVerticalGroup(
            roundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel2Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(balanceField, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(17, Short.MAX_VALUE))
        );

        roundedPanel3.setForeground(new java.awt.Color(255, 255, 255));
        roundedPanel3.setRoundBottomLeft(24);
        roundedPanel3.setRoundBottomRight(24);
        roundedPanel3.setRoundTopLeft(24);
        roundedPanel3.setRoundTopRight(24);

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("Tambah Uang");

        depositeField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                depositeFieldKeyTyped(evt);
            }
        });

        depositeButton.setBackground(new java.awt.Color(0, 153, 255));
        depositeButton.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        depositeButton.setForeground(new java.awt.Color(255, 255, 255));
        depositeButton.setText("Tambah");
        depositeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                depositeButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout roundedPanel3Layout = new javax.swing.GroupLayout(roundedPanel3);
        roundedPanel3.setLayout(roundedPanel3Layout);
        roundedPanel3Layout.setHorizontalGroup(
            roundedPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel3Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(roundedPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(depositeField, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(depositeButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 206, Short.MAX_VALUE))
                .addContainerGap(21, Short.MAX_VALUE))
        );
        roundedPanel3Layout.setVerticalGroup(
            roundedPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel3Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(depositeField, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(depositeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(25, Short.MAX_VALUE))
        );

        roundedPanel4.setForeground(new java.awt.Color(255, 255, 255));
        roundedPanel4.setRoundBottomLeft(24);
        roundedPanel4.setRoundBottomRight(24);
        roundedPanel4.setRoundTopLeft(24);
        roundedPanel4.setRoundTopRight(24);

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel3.setText("Tarik Uang");

        withdrawField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                withdrawFieldKeyTyped(evt);
            }
        });

        withdrawButton.setBackground(new java.awt.Color(255, 51, 51));
        withdrawButton.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        withdrawButton.setForeground(new java.awt.Color(255, 255, 255));
        withdrawButton.setText("Tarik");
        withdrawButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                withdrawButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout roundedPanel4Layout = new javax.swing.GroupLayout(roundedPanel4);
        roundedPanel4.setLayout(roundedPanel4Layout);
        roundedPanel4Layout.setHorizontalGroup(
            roundedPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel4Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(roundedPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(withdrawField, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(withdrawButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 206, Short.MAX_VALUE))
                .addContainerGap(21, Short.MAX_VALUE))
        );
        roundedPanel4Layout.setVerticalGroup(
            roundedPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel4Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(withdrawField, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(withdrawButton, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(25, Short.MAX_VALUE))
        );

        roundedPanel6.setForeground(new java.awt.Color(255, 255, 255));
        roundedPanel6.setRoundBottomLeft(20);
        roundedPanel6.setRoundBottomRight(20);
        roundedPanel6.setRoundTopLeft(20);
        roundedPanel6.setRoundTopRight(20);

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel6.setText("Pinjaman");

        loanTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Nama", "Pinjaman", "Bayar/bulan", "Dibayar", "Waktu Minjam", "Tenggat"
            }
        ));
        jScrollPane3.setViewportView(loanTable);

        javax.swing.GroupLayout roundedPanel6Layout = new javax.swing.GroupLayout(roundedPanel6);
        roundedPanel6.setLayout(roundedPanel6Layout);
        roundedPanel6Layout.setHorizontalGroup(
            roundedPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel6Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(roundedPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 512, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addContainerGap(15, Short.MAX_VALUE))
        );
        roundedPanel6Layout.setVerticalGroup(
            roundedPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, roundedPanel6Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(23, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout roundedPanel1Layout = new javax.swing.GroupLayout(roundedPanel1);
        roundedPanel1.setLayout(roundedPanel1Layout);
        roundedPanel1Layout.setHorizontalGroup(
            roundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel1Layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(roundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(roundedPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(roundedPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(roundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(roundedPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(roundedPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(24, 24, 24))
        );
        roundedPanel1Layout.setVerticalGroup(
            roundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel1Layout.createSequentialGroup()
                .addGap(42, 42, 42)
                .addGroup(roundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(roundedPanel1Layout.createSequentialGroup()
                        .addComponent(roundedPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(35, 35, 35)
                        .addComponent(roundedPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(roundedPanel1Layout.createSequentialGroup()
                        .addComponent(roundedPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(35, 35, 35)
                        .addComponent(roundedPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(154, Short.MAX_VALUE))
        );

        jLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/logoNaga (1).jpg"))); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(27, 27, 27)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(nameText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 300, Short.MAX_VALUE))
            .addComponent(roundedPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 895, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(nameText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(23, 23, 23)
                .addComponent(roundedPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        tabbedPane.addTab("Dashboard", jPanel1);

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));
        jPanel3.setLayout(new java.awt.GridBagLayout());

        roundedPanel11.setBackground(new java.awt.Color(0, 204, 102));
        roundedPanel11.setForeground(new java.awt.Color(0, 204, 102));
        roundedPanel11.setPreferredSize(new java.awt.Dimension(700, 470));
        roundedPanel11.setRoundBottomLeft(36);
        roundedPanel11.setRoundBottomRight(36);
        roundedPanel11.setRoundTopLeft(36);
        roundedPanel11.setRoundTopRight(36);

        jLabel13.setFont(new java.awt.Font("Segoe UI", 1, 48)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(255, 255, 255));
        jLabel13.setText("DragonLend Pinjam");

        roundedPanel12.setForeground(new java.awt.Color(255, 255, 255));
        roundedPanel12.setRoundBottomLeft(20);
        roundedPanel12.setRoundBottomRight(20);
        roundedPanel12.setRoundTopLeft(20);
        roundedPanel12.setRoundTopRight(20);

        jLabel14.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel14.setText("Nama Pinjaman");

        javax.swing.GroupLayout roundedPanel12Layout = new javax.swing.GroupLayout(roundedPanel12);
        roundedPanel12.setLayout(roundedPanel12Layout);
        roundedPanel12Layout.setHorizontalGroup(
            roundedPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel12Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jLabel14)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lendName, javax.swing.GroupLayout.PREFERRED_SIZE, 375, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(24, 24, 24))
        );
        roundedPanel12Layout.setVerticalGroup(
            roundedPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel12Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(roundedPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lendName, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel14))
                .addContainerGap(15, Short.MAX_VALUE))
        );

        roundedPanel16.setForeground(new java.awt.Color(255, 255, 255));
        roundedPanel16.setRoundBottomLeft(20);
        roundedPanel16.setRoundBottomRight(20);
        roundedPanel16.setRoundTopLeft(20);
        roundedPanel16.setRoundTopRight(20);

        jLabel19.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel19.setText("Jenis Pinjaman");

        lendBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1 Tahun cicilan - Bunga 10% ", "6 Bulan cicilan - Bunga 5% ", "3 Bulan cicilan - Bunga 2% " }));
        lendBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lendBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout roundedPanel16Layout = new javax.swing.GroupLayout(roundedPanel16);
        roundedPanel16.setLayout(roundedPanel16Layout);
        roundedPanel16Layout.setHorizontalGroup(
            roundedPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel16Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jLabel19)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lendBox, javax.swing.GroupLayout.PREFERRED_SIZE, 375, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(24, 24, 24))
        );
        roundedPanel16Layout.setVerticalGroup(
            roundedPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel16Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(roundedPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel19)
                    .addComponent(lendBox, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(11, Short.MAX_VALUE))
        );

        lendButton.setBackground(new java.awt.Color(0, 153, 255));
        lendButton.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lendButton.setForeground(new java.awt.Color(255, 255, 255));
        lendButton.setText("Pinjam dengan 1 klik");
        lendButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lendButtonActionPerformed(evt);
            }
        });

        roundedPanel14.setForeground(new java.awt.Color(255, 255, 255));
        roundedPanel14.setRoundBottomLeft(20);
        roundedPanel14.setRoundBottomRight(20);
        roundedPanel14.setRoundTopLeft(20);
        roundedPanel14.setRoundTopRight(20);

        jLabel16.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel16.setText("Jumlah Pinjaman");

        lendAmount.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                lendAmountKeyTyped(evt);
            }
        });

        javax.swing.GroupLayout roundedPanel14Layout = new javax.swing.GroupLayout(roundedPanel14);
        roundedPanel14.setLayout(roundedPanel14Layout);
        roundedPanel14Layout.setHorizontalGroup(
            roundedPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel14Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jLabel16)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lendAmount, javax.swing.GroupLayout.PREFERRED_SIZE, 375, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(24, 24, 24))
        );
        roundedPanel14Layout.setVerticalGroup(
            roundedPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel14Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(roundedPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lendAmount, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16))
                .addContainerGap(15, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout roundedPanel11Layout = new javax.swing.GroupLayout(roundedPanel11);
        roundedPanel11.setLayout(roundedPanel11Layout);
        roundedPanel11Layout.setHorizontalGroup(
            roundedPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel11Layout.createSequentialGroup()
                .addGap(41, 41, 41)
                .addGroup(roundedPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(roundedPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jLabel13)
                        .addComponent(roundedPanel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(roundedPanel16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(roundedPanel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(lendButton, javax.swing.GroupLayout.PREFERRED_SIZE, 212, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(75, Short.MAX_VALUE))
        );
        roundedPanel11Layout.setVerticalGroup(
            roundedPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel11Layout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(roundedPanel12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(roundedPanel14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(roundedPanel16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(27, 27, 27)
                .addComponent(lendButton, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(36, Short.MAX_VALUE))
        );

        jPanel3.add(roundedPanel11, new java.awt.GridBagConstraints());

        tabbedPane.addTab("Pinjam", jPanel3);

        jPanel4.setBackground(new java.awt.Color(255, 255, 255));
        jPanel4.setLayout(new java.awt.GridBagLayout());

        roundedPanel13.setBackground(new java.awt.Color(0, 204, 102));
        roundedPanel13.setForeground(new java.awt.Color(0, 204, 102));
        roundedPanel13.setPreferredSize(new java.awt.Dimension(650, 470));
        roundedPanel13.setRoundBottomLeft(36);
        roundedPanel13.setRoundBottomRight(36);
        roundedPanel13.setRoundTopLeft(36);
        roundedPanel13.setRoundTopRight(36);

        jLabel15.setFont(new java.awt.Font("Segoe UI", 1, 48)); // NOI18N
        jLabel15.setForeground(new java.awt.Color(255, 255, 255));
        jLabel15.setText("Bayar Pinjaman");

        roundedPanel17.setForeground(new java.awt.Color(255, 255, 255));
        roundedPanel17.setRoundBottomLeft(20);
        roundedPanel17.setRoundBottomRight(20);
        roundedPanel17.setRoundTopLeft(20);
        roundedPanel17.setRoundTopRight(20);

        jLabel20.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel20.setText("Nama Pinjaman");

        loanNameBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        loanNameBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loanNameBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout roundedPanel17Layout = new javax.swing.GroupLayout(roundedPanel17);
        roundedPanel17.setLayout(roundedPanel17Layout);
        roundedPanel17Layout.setHorizontalGroup(
            roundedPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel17Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jLabel20)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 18, Short.MAX_VALUE)
                .addComponent(loanNameBox, javax.swing.GroupLayout.PREFERRED_SIZE, 375, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(24, 24, 24))
        );
        roundedPanel17Layout.setVerticalGroup(
            roundedPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel17Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(roundedPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20)
                    .addComponent(loanNameBox, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(11, Short.MAX_VALUE))
        );

        paymentButton.setBackground(new java.awt.Color(0, 153, 255));
        paymentButton.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        paymentButton.setForeground(new java.awt.Color(255, 255, 255));
        paymentButton.setText("Bayar");
        paymentButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                paymentButtonActionPerformed(evt);
            }
        });

        roundedPanel18.setForeground(new java.awt.Color(255, 255, 255));
        roundedPanel18.setRoundBottomLeft(20);
        roundedPanel18.setRoundBottomRight(20);
        roundedPanel18.setRoundTopLeft(20);
        roundedPanel18.setRoundTopRight(20);

        jumlahLabel.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jumlahLabel.setText("Jumlah");

        amountPaidField.setEditable(false);

        javax.swing.GroupLayout roundedPanel18Layout = new javax.swing.GroupLayout(roundedPanel18);
        roundedPanel18.setLayout(roundedPanel18Layout);
        roundedPanel18Layout.setHorizontalGroup(
            roundedPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel18Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addComponent(jumlahLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(amountPaidField, javax.swing.GroupLayout.PREFERRED_SIZE, 372, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(25, 25, 25))
        );
        roundedPanel18Layout.setVerticalGroup(
            roundedPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel18Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(roundedPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jumlahLabel)
                    .addComponent(amountPaidField, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(12, Short.MAX_VALUE))
        );

        roundedPanel19.setForeground(new java.awt.Color(255, 255, 255));
        roundedPanel19.setRoundBottomLeft(20);
        roundedPanel19.setRoundBottomRight(20);
        roundedPanel19.setRoundTopLeft(20);
        roundedPanel19.setRoundTopRight(20);

        jLabel22.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel22.setText("Bulan");

        monthBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Pilih Berapa bulan", "1 Bulan", "2 Bulan", "3 Bulan", "4 Bulan", "5 Bulan", "6 Bulan", "7 Bulan", "8 Bulan", "9 Bulan", "10 Bulan", "11 Bulan", "12 Bulan" }));
        monthBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                monthBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout roundedPanel19Layout = new javax.swing.GroupLayout(roundedPanel19);
        roundedPanel19.setLayout(roundedPanel19Layout);
        roundedPanel19Layout.setHorizontalGroup(
            roundedPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel19Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jLabel22)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 104, Short.MAX_VALUE)
                .addComponent(monthBox, javax.swing.GroupLayout.PREFERRED_SIZE, 375, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(24, 24, 24))
        );
        roundedPanel19Layout.setVerticalGroup(
            roundedPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel19Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(roundedPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel22)
                    .addComponent(monthBox, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(11, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout roundedPanel13Layout = new javax.swing.GroupLayout(roundedPanel13);
        roundedPanel13.setLayout(roundedPanel13Layout);
        roundedPanel13Layout.setHorizontalGroup(
            roundedPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel13Layout.createSequentialGroup()
                .addGap(41, 41, 41)
                .addGroup(roundedPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel15)
                    .addComponent(roundedPanel17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(paymentButton, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(roundedPanel18, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(roundedPanel19, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(31, Short.MAX_VALUE))
        );
        roundedPanel13Layout.setVerticalGroup(
            roundedPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel13Layout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(roundedPanel17, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(roundedPanel19, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(roundedPanel18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26)
                .addComponent(paymentButton, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(35, Short.MAX_VALUE))
        );

        jPanel4.add(roundedPanel13, new java.awt.GridBagConstraints());

        tabbedPane.addTab("Bayar", jPanel4);

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setPreferredSize(new java.awt.Dimension(895, 800));
        jPanel2.setLayout(new java.awt.GridBagLayout());

        roundedPanel15.setBackground(new java.awt.Color(0, 204, 102));
        roundedPanel15.setForeground(new java.awt.Color(0, 204, 102));
        roundedPanel15.setPreferredSize(new java.awt.Dimension(600, 590));
        roundedPanel15.setRoundBottomLeft(36);
        roundedPanel15.setRoundBottomRight(36);
        roundedPanel15.setRoundTopLeft(36);
        roundedPanel15.setRoundTopRight(36);

        historyTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(historyTable);

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 48)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("Riwayat Bayar Hutang");

        javax.swing.GroupLayout roundedPanel15Layout = new javax.swing.GroupLayout(roundedPanel15);
        roundedPanel15.setLayout(roundedPanel15Layout);
        roundedPanel15Layout.setHorizontalGroup(
            roundedPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, roundedPanel15Layout.createSequentialGroup()
                .addContainerGap(48, Short.MAX_VALUE)
                .addGroup(roundedPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 509, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addGap(43, 43, 43))
        );
        roundedPanel15Layout.setVerticalGroup(
            roundedPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, roundedPanel15Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 456, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(40, Short.MAX_VALUE))
        );

        jPanel2.add(roundedPanel15, new java.awt.GridBagConstraints());

        tabbedPane.addTab("Riwayat", jPanel2);

        jPanel5.setBackground(new java.awt.Color(255, 255, 255));
        jPanel5.setForeground(new java.awt.Color(0, 204, 102));
        jPanel5.setLayout(new java.awt.GridBagLayout());

        roundedPanel5.setBackground(new java.awt.Color(0, 204, 102));
        roundedPanel5.setForeground(new java.awt.Color(0, 204, 102));
        roundedPanel5.setPreferredSize(new java.awt.Dimension(700, 500));
        roundedPanel5.setRoundBottomLeft(36);
        roundedPanel5.setRoundBottomRight(36);
        roundedPanel5.setRoundTopLeft(36);
        roundedPanel5.setRoundTopRight(36);

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 48)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(255, 255, 255));
        jLabel7.setText("Data Diri");

        roundedPanel7.setForeground(new java.awt.Color(255, 255, 255));
        roundedPanel7.setRoundBottomLeft(20);
        roundedPanel7.setRoundBottomRight(20);
        roundedPanel7.setRoundTopLeft(20);
        roundedPanel7.setRoundTopRight(20);

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel8.setText("NIK");

        NIKSet.setEditable(false);

        javax.swing.GroupLayout roundedPanel7Layout = new javax.swing.GroupLayout(roundedPanel7);
        roundedPanel7.setLayout(roundedPanel7Layout);
        roundedPanel7Layout.setHorizontalGroup(
            roundedPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel7Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(NIKSet, javax.swing.GroupLayout.PREFERRED_SIZE, 381, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(24, 24, 24))
        );
        roundedPanel7Layout.setVerticalGroup(
            roundedPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel7Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(roundedPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(NIKSet, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addContainerGap(15, Short.MAX_VALUE))
        );

        roundedPanel8.setForeground(new java.awt.Color(255, 255, 255));
        roundedPanel8.setRoundBottomLeft(20);
        roundedPanel8.setRoundBottomRight(20);
        roundedPanel8.setRoundTopLeft(20);
        roundedPanel8.setRoundTopRight(20);

        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel9.setText("Nama");

        nameSet.setEditable(false);

        javax.swing.GroupLayout roundedPanel8Layout = new javax.swing.GroupLayout(roundedPanel8);
        roundedPanel8.setLayout(roundedPanel8Layout);
        roundedPanel8Layout.setHorizontalGroup(
            roundedPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel8Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(nameSet, javax.swing.GroupLayout.PREFERRED_SIZE, 383, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(21, 21, 21))
        );
        roundedPanel8Layout.setVerticalGroup(
            roundedPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel8Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(roundedPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nameSet, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9))
                .addContainerGap(15, Short.MAX_VALUE))
        );

        roundedPanel9.setForeground(new java.awt.Color(255, 255, 255));
        roundedPanel9.setRoundBottomLeft(20);
        roundedPanel9.setRoundBottomRight(20);
        roundedPanel9.setRoundTopLeft(20);
        roundedPanel9.setRoundTopRight(20);

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel10.setText("Nomor Telepon");

        phoneSet.setEditable(false);
        phoneSet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                phoneSetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout roundedPanel9Layout = new javax.swing.GroupLayout(roundedPanel9);
        roundedPanel9.setLayout(roundedPanel9Layout);
        roundedPanel9Layout.setHorizontalGroup(
            roundedPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel9Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jLabel10)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(phoneSet, javax.swing.GroupLayout.PREFERRED_SIZE, 380, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(22, 22, 22))
        );
        roundedPanel9Layout.setVerticalGroup(
            roundedPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel9Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(roundedPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(phoneSet, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10))
                .addContainerGap(15, Short.MAX_VALUE))
        );

        roundedPanel10.setForeground(new java.awt.Color(255, 255, 255));
        roundedPanel10.setRoundBottomLeft(20);
        roundedPanel10.setRoundBottomRight(20);
        roundedPanel10.setRoundTopLeft(20);
        roundedPanel10.setRoundTopRight(20);

        jLabel11.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel11.setText("Pendapatan /Bulan");

        incomeSet.setEditable(false);
        incomeSet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                incomeSetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout roundedPanel10Layout = new javax.swing.GroupLayout(roundedPanel10);
        roundedPanel10.setLayout(roundedPanel10Layout);
        roundedPanel10Layout.setHorizontalGroup(
            roundedPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel10Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(incomeSet, javax.swing.GroupLayout.PREFERRED_SIZE, 380, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(23, Short.MAX_VALUE))
        );
        roundedPanel10Layout.setVerticalGroup(
            roundedPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel10Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(roundedPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(incomeSet, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        jButton5.setBackground(new java.awt.Color(0, 153, 255));
        jButton5.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jButton5.setForeground(new java.awt.Color(255, 255, 255));
        jButton5.setText("Update");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        Logout.setBackground(new java.awt.Color(255, 0, 51));
        Logout.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        Logout.setForeground(new java.awt.Color(255, 255, 255));
        Logout.setText("Logout");
        Logout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LogoutActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout roundedPanel5Layout = new javax.swing.GroupLayout(roundedPanel5);
        roundedPanel5.setLayout(roundedPanel5Layout);
        roundedPanel5Layout.setHorizontalGroup(
            roundedPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel5Layout.createSequentialGroup()
                .addGap(41, 41, 41)
                .addGroup(roundedPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(roundedPanel5Layout.createSequentialGroup()
                        .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(Logout, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(roundedPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addGap(53, 53, 53))
                    .addComponent(roundedPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(roundedPanel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(roundedPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(roundedPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(17, Short.MAX_VALUE))
        );
        roundedPanel5Layout.setVerticalGroup(
            roundedPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel5Layout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(roundedPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(roundedPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(roundedPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(roundedPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(roundedPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Logout, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(51, Short.MAX_VALUE))
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipadx = 11;
        gridBagConstraints.ipady = 45;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(69, 110, 97, 117);
        jPanel5.add(roundedPanel5, gridBagConstraints);

        tabbedPane.addTab("Pengaturan", jPanel5);

        Body.add(tabbedPane, java.awt.BorderLayout.PAGE_START);

        getContentPane().add(Body, "card2");

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void tabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabbedPaneStateChanged
        // TODO add your handling code here:
    }//GEN-LAST:event_tabbedPaneStateChanged

    private void withdrawButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_withdrawButtonActionPerformed
        // TODO add your handling code here:
        if (!validateTextField(withdrawField, "Input Harus diisi")) {
         return;
        }
        
        try {
            String sqlBalance = "SELECT balance FROM user WHERE id = ?";
            PreparedStatement psCheck = conn.prepareStatement(sqlBalance);
            psCheck.setInt(1, Global.UserId);
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {
                int currentBalance = rs.getInt("balance"); 

                int withdrawAmount = Integer.parseInt(withdrawField.getText()); 

                int newBalance = currentBalance - withdrawAmount;
                    
                if (newBalance < 0) {
                    JOptionPane.showMessageDialog(null, "Saldo tersisa " + currentBalance, "Warning", JOptionPane.ERROR_MESSAGE);
                    withdrawField.setText("");
                    return;
                }
                
                String sqlUpdate = "UPDATE user SET balance = ? WHERE id = ?";
                PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate);
                psUpdate.setDouble(1, newBalance);
                psUpdate.setInt(2, Global.UserId);
                psUpdate.executeUpdate();

                refreshBalance();
                withdrawField.setText("");
            } else {
                System.out.println("User tidak ditemukan.");
            }

            rs.close();
            psCheck.close();
        } catch (NumberFormatException e) {
            System.out.println("Error: Nilai di despositeField tidak valid.");
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }//GEN-LAST:event_withdrawButtonActionPerformed

    private void depositeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_depositeButtonActionPerformed
        // TODO add your handling code here:
        if (!validateTextField(depositeField, "Input harus diisi!")) {
         return;
        }
        
        
        try {
            String sqlBalance = "SELECT balance FROM user WHERE id = ?";
            PreparedStatement psCheck = conn.prepareStatement(sqlBalance);
            psCheck.setInt(1, Global.UserId);
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {
                int currentBalance = rs.getInt("balance"); 

                int depositAmount = Integer.parseInt(depositeField.getText()); 

                int newBalance = currentBalance + depositAmount;
                String sqlUpdate = "UPDATE user SET balance = ? WHERE id = ?";
                PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate);
                psUpdate.setDouble(1, newBalance);
                psUpdate.setInt(2, Global.UserId);
                psUpdate.executeUpdate();

                refreshBalance();
                depositeField.setText("");
            } else {
                System.out.println("User tidak ditemukan.");
            }

            rs.close();
            psCheck.close();
        } catch (NumberFormatException e) {
            System.out.println("Error: Nilai di despositeField tidak valid.");
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        
    }//GEN-LAST:event_depositeButtonActionPerformed

    private void phoneSetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_phoneSetActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_phoneSetActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        // TODO add your handling code here:
        EditProfile edit = new EditProfile();
        
        edit.setVisible(true);
        this.setVisible(false);
    }//GEN-LAST:event_jButton5ActionPerformed

    private void incomeSetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_incomeSetActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_incomeSetActionPerformed

    private void lendBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lendBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_lendBoxActionPerformed

    private void lendButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lendButtonActionPerformed
        // TODO add your handling code here:
        if (!validateTextField(lendName, "Nama Harus diisi")) {
         return;
        }
        
        if (!validateTextField(lendAmount, "Jumlah pinjaman Harus diisi")) {
         return;
        }   
        
        try {
            String sqlSelect = "SELECT user_limit, debt, balance from user where id = ?";
            PreparedStatement psSelect = conn.prepareStatement(sqlSelect);
            psSelect.setInt(1, Global.UserId);
            ResultSet rsSelect = psSelect.executeQuery();
            int debt = 0;
            int lendAmountValue = Integer.parseInt(lendAmount.getText());
            
            if (rsSelect.next()) {
                long limit = rsSelect.getLong("user_limit");
                debt = rsSelect.getInt("debt");
                
                if (lendAmountValue + debt > limit) {
                    JOptionPane.showMessageDialog(this, "Pinjaman gagal, Limit pinjaman anda : " + limit);
                    lendAmount.setText("");
                    return;
                }
            }
            
            String sqlInsert = "INSERT INTO loans (user_id, name, amount, interest_rate, tenure_months, monthly_payment, total_payment, payment_total, status, due_date, now_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement psInsert = conn.prepareStatement(sqlInsert);
            
            int indexBox = lendBox.getSelectedIndex();
            int interest_rate = 0;
            int tenure_months = 0;
            int monthly_payment = 0;
            int total_payment = 0;
            int payment_total = 0;
            int total_interest = 0;
            int loanAmount = Integer.parseInt(lendAmount.getText());
            String current_date = getCurrentDate();
            String due_date = "";

            if (indexBox == 0) {
                interest_rate = 10; 
                tenure_months = 12;
                total_interest = (loanAmount * interest_rate * tenure_months) / (100 * 12);
                total_payment = loanAmount + total_interest;
                monthly_payment = (int) Math.ceil((double) total_payment / tenure_months);
                due_date = getLoanDueDate(tenure_months);
            } else if (indexBox == 1) {
                interest_rate = 5; 
                tenure_months = 6;
                total_interest = (loanAmount * interest_rate * tenure_months) / (100 * 12);
                total_payment = loanAmount + total_interest;
                monthly_payment = (int) Math.ceil((double) total_payment / tenure_months);
                due_date = getLoanDueDate(tenure_months);
            } else {
                interest_rate = 2; 
                tenure_months = 3;
                total_interest = (loanAmount * interest_rate * tenure_months) / (100 * 12);
                total_payment = loanAmount + total_interest;
                monthly_payment = (int) Math.ceil((double) total_payment / tenure_months);
                due_date = getLoanDueDate(tenure_months);
            }
            
            psInsert.setInt(1, Global.UserId);
            psInsert.setString(2, lendName.getText());
            psInsert.setInt(3, loanAmount);
            psInsert.setInt(4, interest_rate);
            psInsert.setInt(5, tenure_months);
            psInsert.setInt(6, monthly_payment);
            psInsert.setInt(7, total_payment);
            psInsert.setInt(8, payment_total);
            psInsert.setString(9, "active");
            psInsert.setString(10, due_date);
            psInsert.setString(11, current_date);
            
            String sql = "SELECT balance FROM user WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            psSelect.setInt(1, Global.UserId);
            ResultSet rs = psSelect.executeQuery();

            int balance = 0;
            if (rs.next()) {
                balance = rs.getInt("balance"); 
            }

            String sqlUpdate = "UPDATE user SET balance = ?, debt = ? WHERE id = ?";
            PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate);
            psUpdate.setInt(1, balance + loanAmount); 
            psUpdate.setInt(2, debt + lendAmountValue);           
            psUpdate.setInt(3, Global.UserId);        
            psUpdate.executeUpdate();
            psInsert.executeUpdate();

            JOptionPane.showMessageDialog(null, "Ajuan pinjaman berhasil", "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            lendName.setText("");
            lendAmount.setText("");
            lendBox.setSelectedIndex(0);
            LoadHistoryTable();
            loadLoanTable();
            setupTypeBox();
            setupBalance();
            
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        
        try {
            String sqlSelect = "SELECT balance from user where id = ?";
            PreparedStatement psSelect = conn.prepareStatement(sqlSelect);
            psSelect.setInt(1, Global.UserId);
            ResultSet rsBalance = psSelect.executeQuery();
            
            if (rsBalance.next()) {
                String balance = Integer.toString(rsBalance.getInt("balance"));
                
                balanceField.setText(balance);
            }
            
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }//GEN-LAST:event_lendButtonActionPerformed

    private void LogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LogoutActionPerformed
        // TODO add your handling code here:
        Global.UserId = 0;
        Main login = new Main();
        login.setVisible(true);
        this.setVisible(false);
        
        resetProfile();
    }//GEN-LAST:event_LogoutActionPerformed

    private void monthBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_monthBoxActionPerformed
        // TODO add your handling code here:
        int idLoan = loanNameBox.getSelectedIndex();
        int monthNumber = monthBox.getSelectedIndex();

        try {
            String sql = "SELECT monthly_payment from loans where id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, listIdLoans.get(idLoan));
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int monthly = rs.getInt("monthly_payment");
                amountPaidField.setText(Integer.toString(monthly * monthNumber));
            }
        } catch (SQLException e) {
           
        }
    }//GEN-LAST:event_monthBoxActionPerformed

    private void paymentButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_paymentButtonActionPerformed
        // TODO add your handling code here:
        int idLoan = loanNameBox.getSelectedIndex();
        int monthNumber = monthBox.getSelectedIndex();
        int monthly_payment = 0;
        int paid_month = 0;
        int tenure = 0;
        int balance = 0;
        
        System.out.println(monthNumber);
        
        if (monthNumber == 0) {
            JOptionPane.showMessageDialog(null, "Isi Format dengan benar!", "Gagal", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (amountPaidField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Isi Format dengan benar!", "Gagal", JOptionPane.ERROR_MESSAGE);
        }
        
        System.out.println(idLoan);
        
         if (idLoan == -1 || loanNameBox.getItemCount() == 0) {
            idLoan = 0;  
        }
        
        try {
            String sqlUser = "SELECT balance from user where id = ?";
            PreparedStatement psUser = conn.prepareStatement(sqlUser);
            psUser.setInt(1, Global.UserId);
            ResultSet rsUser = psUser.executeQuery();
            
            if (rsUser.next()) {
                balance = rsUser.getInt("balance");
            }
            
            if (Integer.parseInt(amountPaidField.getText()) > balance) {
                JOptionPane.showMessageDialog(null, "Uang Tidak Cukup!", "Gagal", JOptionPane.ERROR_MESSAGE);
                loanNameBox.setSelectedIndex(0);
                monthBox.setSelectedIndex(0);
                amountPaidField.setText("");
                return;
            }
            
            String sql = "INSERT INTO payments (user_id, loans_id, now_time, payment_month) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, Global.UserId);
            ps.setInt(2, listIdLoans.get(idLoan));
            ps.setString(3, getCurrentDate());
            ps.setInt(4, monthNumber);
            ps.executeUpdate();
            
            String sqlSelect = "SELECT monthly_payment, paid_month, tenure_months from loans where id = ?";
            PreparedStatement psSelect = conn.prepareStatement(sqlSelect);
            psSelect.setInt(1, listIdLoans.get(idLoan));
            ResultSet rs = psSelect.executeQuery();
            
            if (rs.next()) {
                monthly_payment = rs.getInt("monthly_payment");
                paid_month = rs.getInt("paid_month");
                tenure = rs.getInt("tenure_months");
            }
            
            int charge = (paid_month + monthNumber) * monthly_payment;
            String status = "active";
            
            int updatedPaidMonth = paid_month + monthNumber;
            if (updatedPaidMonth >= tenure) {
                status = "completed";
                String sqlUpdateUser = "UPDATE user SET debt = ? WHERE id = ?";
                PreparedStatement psUpdateUser = conn.prepareStatement(sqlUpdateUser);
                int newDebt = 0;
                psUpdateUser.setInt(1, newDebt);
                psUpdateUser.setInt(2, Global.UserId);
                psUpdateUser.executeUpdate();
            }
            
            System.out.println("Paid_month : " + updatedPaidMonth);
            System.out.println("Payment_total : " + charge);
            
            String sqlUpdate = "UPDATE loans SET payment_total = ?, paid_month = ?, status = ? WHERE id = ?";
            PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate);
            psUpdate.setInt(1, charge);
            psUpdate.setInt(2, updatedPaidMonth);
            psUpdate.setString(3, status);
            psUpdate.setInt(4, listIdLoans.get(idLoan));
            psUpdate.executeUpdate();
            
            int newBalance = balance - (Integer.parseInt(amountPaidField.getText()));
            
            String sqlUpdateUser = "UPDATE user SET balance = ? WHERE id = ?";
            PreparedStatement psUpdateUser = conn.prepareStatement(sqlUpdateUser);
            psUpdateUser.setInt(1, newBalance);
            psUpdateUser.setInt(2, Global.UserId);
            psUpdateUser.executeUpdate();
            
            JOptionPane.showMessageDialog(null, "Pembayaran Berhasil!", "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            loanNameBox.setSelectedIndex(0);
            monthBox.setSelectedIndex(0);
            amountPaidField.setText("");
            
            LoadHistoryTable();
            loadLoanTable();
            setupTypeBox();
            setupBalance();
            
            amountPaidField.setText("");
            idLoan = 0;
            monthNumber = 0;
            monthly_payment = 0;
            paid_month = 0;
            tenure = 0;
            balance = 0;
            setupLoanStructure();
            loadLoanTable();
            setupTypeBox();
            setupHistoryStructure();
            LoadHistoryTable();
            
            System.out.println("Ini Dipanggilllllllll");
        } catch (SQLException e) {
           System.out.println("Error: " + e.getMessage());
        }
    }//GEN-LAST:event_paymentButtonActionPerformed

    private void loanNameBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loanNameBoxActionPerformed
        // TODO add your handling code here:
        int idLoan = loanNameBox.getSelectedIndex();
        int monthNumber = monthBox.getSelectedIndex();
        
        if (idLoan == -1) {
            idLoan = 0;
            return;
        }

        try {
            String sql = "SELECT monthly_payment from loans where id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, listIdLoans.get(idLoan));
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int monthly = rs.getInt("monthly_payment");
                amountPaidField.setText(Integer.toString(monthly * monthNumber));
            }
        } catch (SQLException e) {
           
        }
    }//GEN-LAST:event_loanNameBoxActionPerformed

    int count = 0;
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:

        try {
            String sqlBalance = "SELECT balance from user WHERE id = ?";
            PreparedStatement psCheck = conn.prepareStatement(sqlBalance);
            psCheck.setInt(1, Global.UserId);
            ResultSet rs = psCheck.executeQuery();
            
            while(rs.next()) {
                int balance = rs.getInt("balance");
                if (count % 2 == 0) {
                    balanceField.setText(Integer.toString(balance));
                } else {
                    balanceField.setText("*********");
                }
            }
            
        } catch(SQLException e) {
            System.out.println("Error : " + e.getMessage());
        }
        
        count++;
        
    }//GEN-LAST:event_jButton1ActionPerformed

    private void depositeFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_depositeFieldKeyTyped
        // TODO add your handling code here:
         char c = evt.getKeyChar();
         
         if (!Character.isDigit(c)) {
            evt.consume();
        }
    }//GEN-LAST:event_depositeFieldKeyTyped

    private void withdrawFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_withdrawFieldKeyTyped
        // TODO add your handling code here:
        char c = evt.getKeyChar();
         
         if (!Character.isDigit(c)) {
            evt.consume();
        }
    }//GEN-LAST:event_withdrawFieldKeyTyped

    private void lendAmountKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_lendAmountKeyTyped
        // TODO add your handling code here:
        char c = evt.getKeyChar();
         
         if (!Character.isDigit(c)) {
            evt.consume();
        }
    }//GEN-LAST:event_lendAmountKeyTyped

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ContentUser.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ContentUser.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ContentUser.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ContentUser.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ContentUser().setVisible(true);
            }
        });
    }
    
    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel Body;
    private javax.swing.JButton Logout;
    private javax.swing.JTextField NIKSet;
    private javax.swing.JTextField amountPaidField;
    private javax.swing.JTextField balanceField;
    private javax.swing.JButton depositeButton;
    private javax.swing.JTextField depositeField;
    private javax.swing.JTable historyTable;
    private javax.swing.JTextField incomeSet;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton5;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel jumlahLabel;
    private javax.swing.JTextField lendAmount;
    private javax.swing.JComboBox<String> lendBox;
    private javax.swing.JButton lendButton;
    private javax.swing.JTextField lendName;
    private javax.swing.JComboBox<String> loanNameBox;
    private javax.swing.JTable loanTable;
    private javax.swing.JComboBox<String> monthBox;
    private javax.swing.JTextField nameSet;
    private javax.swing.JTextField nameText;
    private javax.swing.JButton paymentButton;
    private javax.swing.JTextField phoneSet;
    private costume.RoundedPanel roundedPanel1;
    private costume.RoundedPanel roundedPanel10;
    private costume.RoundedPanel roundedPanel11;
    private costume.RoundedPanel roundedPanel12;
    private costume.RoundedPanel roundedPanel13;
    private costume.RoundedPanel roundedPanel14;
    private costume.RoundedPanel roundedPanel15;
    private costume.RoundedPanel roundedPanel16;
    private costume.RoundedPanel roundedPanel17;
    private costume.RoundedPanel roundedPanel18;
    private costume.RoundedPanel roundedPanel19;
    private costume.RoundedPanel roundedPanel2;
    private costume.RoundedPanel roundedPanel3;
    private costume.RoundedPanel roundedPanel4;
    private costume.RoundedPanel roundedPanel5;
    private costume.RoundedPanel roundedPanel6;
    private costume.RoundedPanel roundedPanel7;
    private costume.RoundedPanel roundedPanel8;
    private costume.RoundedPanel roundedPanel9;
    private raven.tabbed.ShadowRenderer shadowRenderer1;
    private raven.tabbed.TabbedPaneCustom tabbedPane;
    private javax.swing.JButton withdrawButton;
    private javax.swing.JTextField withdrawField;
    // End of variables declaration//GEN-END:variables
}
