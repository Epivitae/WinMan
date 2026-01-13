package cn.ac.cns.winman;

import cn.ac.cns.winman.core.SyncManager;
import cn.ac.cns.winman.ui.WinManStyle;
import cn.ac.cns.winman.utils.VersionUtils;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GUI;
import ij.plugin.PlugIn;
import ij.plugin.frame.PlugInFrame;
import ij.process.ImageProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;

/**
 * WinMan - Window Manager & Workflow Accelerator
 * v2.0.0 - Refactored Structure
 * @author Kui Wang
 */
public class WinMan extends PlugInFrame implements PlugIn, ActionListener {

    private static final long serialVersionUID = 1L;

    // Components
    private JTextField filterField;
    private JButton btnCloseMatch, btnKeepMatch, btnCloseAll;
    private JButton btnTile, btnCascade;
    private JButton btnAutoContrast, btnResetZoom;
    private JToggleButton btnSyncView;
    private JProgressBar memoryBar;

    private static WinMan instance;
    private Timer memoryTimer;
    
    // Logic Modules
    private SyncManager syncManager;

    public WinMan() {
        super("Windows Manager");
        if (instance != null) {
            instance.toFront();
            return;
        }
        instance = this;
        
        // 初始化逻辑模块
        syncManager = new SyncManager();
        
        // UI 初始化
        String version = VersionUtils.getVersion(getClass());
        setCustomIcon("/icons/winman_icon.png");
        
        setLayout(new BorderLayout());
        setBackground(WinManStyle.BG_BODY_GRAY);

        // --- A. Header ---
        add(createHeaderPanel(version), BorderLayout.NORTH);

        // --- B. Main Body ---
        JPanel bodyPanel = new JPanel(new GridBagLayout());
        bodyPanel.setBackground(WinManStyle.BG_BODY_GRAY);
        bodyPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        bodyPanel.setPreferredSize(new Dimension(260, 320)); // 保持 v2.0 的高度

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        c.weightx = 1.0;
        c.gridx = 0;

        // Group 1: Filter
        c.gridy = 0;
        bodyPanel.add(createFilterPanel(), c);

        // Group 2: Window Layout
        c.gridy = 1;
        bodyPanel.add(createWindowToolsPanel(), c);

        // Group 3: Image Tools (包含 Sync View)
        c.gridy = 2;
        bodyPanel.add(createImageToolsPanel(), c);
        
        // Group 4: Close All
        c.gridy = 3;
        c.weighty = 0;
        c.anchor = GridBagConstraints.SOUTH;
        c.insets = new Insets(20, 0, 5, 0);
        
        btnCloseAll = createFlatButton("Close ALL Images", false);
        btnCloseAll.setForeground(WinManStyle.CLOSE_ALL_RED);
        btnCloseAll.setFont(WinManStyle.FONT_BTN_BOLD);
        btnCloseAll.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnCloseAll.setBackground(WinManStyle.CLOSE_ALL_BG_HOVER); }
            public void mouseExited(MouseEvent e) { btnCloseAll.setBackground(WinManStyle.BTN_BG_NORMAL); }
        });
        btnCloseAll.addActionListener(this);
        btnCloseAll.setMargin(new Insets(10, 0, 10, 0));
        bodyPanel.add(btnCloseAll, c);

        add(bodyPanel, BorderLayout.CENTER);

        // --- C. Footer ---
        add(createMemoryFooter(), BorderLayout.SOUTH);

        startMemoryMonitor();

        // Finalize
        pack();
        GUI.center(this);
        setResizable(false);
        setVisible(true);
    }

    // --- Panel Generators ---

    private JPanel createHeaderPanel(String version) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(WinManStyle.BG_HEADER_WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, WinManStyle.BTN_BORDER),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        URL imgURL = getClass().getResource("/icons/winman_icon.png");
        if (imgURL != null) {
            ImageIcon originalIcon = new ImageIcon(imgURL);
            Image resizedImg = originalIcon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
            p.add(new JLabel(new ImageIcon(resizedImg)), BorderLayout.WEST);
        }

        String html = "<html>" +
                "<div style='font-family: SansSerif;'>" +
                "<span style='font-size:14px; font-weight:bold; color:rgb(" + 
                WinManStyle.TEXT_BLUE.getRed() + "," + WinManStyle.TEXT_BLUE.getGreen() + "," + WinManStyle.TEXT_BLUE.getBlue() + 
                ");'>WinMan Manager</span><br>" +
                "<span style='font-size:9px; color:gray;'>v" + version + " | © 2026 cns.ac.cn</span>" +
                "</div></html>";
        p.add(new JLabel(html), BorderLayout.CENTER);
        return p;
    }

    private JPanel createFilterPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(WinManStyle.BG_BODY_GRAY);
        p.setBorder(WinManStyle.createFlatTitledBorder("Filter Selection"));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(8, 8, 8, 8);
        
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        filterField = new JTextField();
        filterField.setFont(WinManStyle.FONT_INPUT);
        filterField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(WinManStyle.BTN_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));
        p.add(filterField, c);

        c.gridy = 1; c.gridwidth = 1; c.weightx = 0.5;
        c.insets = new Insets(0, 8, 8, 4);
        btnCloseMatch = createFlatButton("Close Match", true);
        p.add(btnCloseMatch, c);

        c.gridx = 1;
        c.insets = new Insets(0, 4, 8, 8);
        btnKeepMatch = createFlatButton("Keep Match", true);
        p.add(btnKeepMatch, c);
        return p;
    }

    private JPanel createWindowToolsPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(WinManStyle.BG_BODY_GRAY);
        p.setBorder(WinManStyle.createFlatTitledBorder("Window Layout"));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(8, 8, 8, 4);
        c.weightx = 0.5;

        btnTile = createFlatButton("Tile", true);
        p.add(btnTile, c);

        c.gridx = 1;
        c.insets = new Insets(8, 4, 8, 8);
        btnCascade = createFlatButton("Cascade", true);
        p.add(btnCascade, c);
        return p;
    }

    private JPanel createImageToolsPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(WinManStyle.BG_BODY_GRAY);
        p.setBorder(WinManStyle.createFlatTitledBorder("Image Tools"));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;

        // Row 1
        c.gridy = 0; c.gridx = 0;
        c.insets = new Insets(8, 8, 4, 4);
        btnAutoContrast = createFlatButton("Auto Contrast", true);
        btnAutoContrast.setToolTipText("Auto adjust B&C for all images");
        p.add(btnAutoContrast, c);

        c.gridx = 1;
        c.insets = new Insets(8, 4, 4, 8);
        btnResetZoom = createFlatButton("Reset Zoom", true);
        btnResetZoom.setToolTipText("Zoom 100% for all images");
        p.add(btnResetZoom, c);
        
        // Row 2: Sync View
        c.gridy = 1; c.gridx = 0; c.gridwidth = 2;
        c.insets = new Insets(4, 8, 8, 8);
        
        btnSyncView = new JToggleButton("Sync View (Pan/Zoom/Slice)");
        btnSyncView.setFont(WinManStyle.FONT_BTN_NORMAL);
        btnSyncView.setFocusPainted(false);
        btnSyncView.setBackground(WinManStyle.BTN_BG_NORMAL);
        btnSyncView.setForeground(Color.DARK_GRAY);
        btnSyncView.setBorder(BorderFactory.createLineBorder(WinManStyle.BTN_BORDER, 1));
        btnSyncView.setMargin(new Insets(6, 0, 6, 0));
        btnSyncView.setToolTipText("Synchronize Slice, Zoom and Pan across open images");
        
        btnSyncView.addActionListener(e -> {
            if (btnSyncView.isSelected()) {
                btnSyncView.setBackground(WinManStyle.BTN_SYNC_ON);
                btnSyncView.setText("Sync View [ON]");
                syncManager.start(); 
                IJ.showStatus("Sync ON");
            } else {
                btnSyncView.setBackground(WinManStyle.BTN_BG_NORMAL);
                btnSyncView.setText("Sync View (Pan/Zoom/Slice)");
                syncManager.stop(); 
                IJ.showStatus("Sync OFF");
            }
        });
        p.add(btnSyncView, c);
        return p;
    }

    private JPanel createMemoryFooter() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(WinManStyle.BG_BODY_GRAY);
        p.setBorder(BorderFactory.createEmptyBorder(5, 15, 10, 15)); 

        memoryBar = new JProgressBar(0, 100);
        memoryBar.setStringPainted(true);
        memoryBar.setString("Memory: Calculating...");
        memoryBar.setFont(WinManStyle.FONT_MEMORY);
        memoryBar.setForeground(new Color(100, 180, 100)); 
        memoryBar.setBackground(Color.WHITE);
        memoryBar.setBorder(BorderFactory.createLineBorder(WinManStyle.BTN_BORDER));
        memoryBar.setToolTipText("Click to force Garbage Collection");
        memoryBar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        memoryBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                IJ.showStatus("Cleaning Memory...");
                System.gc();
                updateMemory();
                IJ.showStatus("Memory Cleaned.");
            }
        });
        p.add(memoryBar, BorderLayout.CENTER);
        return p;
    }

    // --- Logic & Events ---

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        String keyword = filterField.getText().trim();

        SwingUtilities.invokeLater(() -> {
            if (src == btnCloseMatch) {
                if (checkInput(keyword)) runFilter(keyword, false);
            } else if (src == btnKeepMatch) {
                if (checkInput(keyword)) runFilter(keyword, true);
            } else if (src == btnCloseAll) {
                if (IJ.showMessageWithCancel("Confirm", "Close ALL images?")) {
                    IJ.run("Close All");
                }
            } else if (src == btnTile) {
                IJ.run("Tile");
            } else if (src == btnCascade) {
                IJ.run("Cascade");
            } else if (src == btnAutoContrast) {
                runBatchCommand("Auto Contrast");
            } else if (src == btnResetZoom) {
                runBatchCommand("Reset Zoom");
            }
        });
    }

    private void runBatchCommand(String command) {
        int[] wList = WindowManager.getIDList();
        if (wList == null) { IJ.showStatus("No images open."); return; }
        int count = 0;
        for (int id : wList) {
            ImagePlus imp = WindowManager.getImage(id);
            if (imp != null) {
                if ("Auto Contrast".equals(command)) {
                    ImageProcessor ip = imp.getProcessor();
                    ip.resetMinAndMax();
                    imp.updateAndDraw(); 
                    IJ.run(imp, "Enhance Contrast", "saturated=0.35");
                    count++;
                } else if ("Reset Zoom".equals(command)) {
                    if (imp.getWindow() != null && imp.getCanvas() != null) {
                        imp.getCanvas().zoom100Percent();
                    }
                    count++;
                }
            }
        }
        IJ.showStatus(command + " applied to " + count + " images.");
    }

    private void startMemoryMonitor() {
        memoryTimer = new Timer(2000, e -> updateMemory());
        memoryTimer.start();
        updateMemory(); 
    }

    private void updateMemory() {
        long max = Runtime.getRuntime().maxMemory();
        long total = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();
        long used = total - free;
        int percent = (int) ((used * 100) / max);
        int usedMB = (int) (used / 1024 / 1024);
        int maxMB = (int) (max / 1024 / 1024);

        memoryBar.setValue(percent);
        memoryBar.setString("RAM: " + usedMB + "M / " + maxMB + "M (" + percent + "%)");
        if (percent > 85) memoryBar.setForeground(WinManStyle.MEM_TEXT_RED); 
        else memoryBar.setForeground(WinManStyle.MEM_TEXT_GREEN); 
    }

    private boolean checkInput(String k) {
        if (k.isEmpty()) { IJ.error("Please enter a keyword."); return false; }
        return true;
    }

    private void runFilter(String keyword, boolean keepMode) {
        int[] wList = WindowManager.getIDList();
        if (wList == null) return;
        for (int i=0; i<wList.length; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            if (imp != null) {
                boolean match = imp.getTitle().toLowerCase().contains(keyword.toLowerCase());
                if (keepMode ? !match : match) { imp.changes=false; imp.close(); }
            }
        }
    }

    private JButton createFlatButton(String text, boolean addListener) {
        JButton btn = new JButton(text);
        btn.setFont(WinManStyle.FONT_BTN_NORMAL);
        btn.setFocusPainted(false); 
        btn.setBackground(WinManStyle.BTN_BG_NORMAL);
        btn.setForeground(Color.DARK_GRAY);
        btn.setBorder(BorderFactory.createLineBorder(WinManStyle.BTN_BORDER, 1));
        btn.setMargin(new Insets(6, 12, 6, 12)); 
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if(btn.isEnabled()) btn.setBackground(WinManStyle.BTN_BG_HOVER); }
            public void mouseExited(MouseEvent e) { btn.setBackground(WinManStyle.BTN_BG_NORMAL); }
        });
        if (addListener) btn.addActionListener(this);
        return btn;
    }

    private void setCustomIcon(String path) {
        URL imgURL = getClass().getResource(path);
        if (imgURL != null) setIconImage(new ImageIcon(imgURL).getImage());
    }

    @Override public void run(String arg) {}
    
    @Override public void windowClosing(WindowEvent e) {
        if (memoryTimer != null) memoryTimer.stop(); 
        if (syncManager != null) syncManager.stop(); // 停止同步监听
        super.windowClosing(e);
        instance = null;
    }
}