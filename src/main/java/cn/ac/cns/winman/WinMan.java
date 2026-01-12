package cn.ac.cns.winman;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GUI;
import ij.plugin.PlugIn;
import ij.plugin.frame.PlugInFrame;
import ij.process.ImageProcessor;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * WinMan - Window Manager
 * v1.0.0 - Official Release
 * @author Kui Wang
 */
public class WinMan extends PlugInFrame implements PlugIn, ActionListener {

    private static final long serialVersionUID = 1L;
    
    // --- 1. Centralized Color Palette (UI 颜色配置) ---
    private static final Color BG_HEADER_WHITE = Color.WHITE;
    private static final Color BG_BODY_GRAY    = new Color(245, 247, 249); 
    private static final Color TEXT_BLUE       = new Color(51, 102, 204); 
    private static final Color BTN_BG_NORMAL   = Color.WHITE;
    private static final Color BTN_BG_HOVER    = new Color(235, 240, 245); 
    private static final Color BTN_BORDER      = new Color(200, 200, 200); 

    // --- 2. Centralized Font Configuration (字体统一管理) ---
    // 修改这里可以改变全局字体大小
    private static final Font FONT_GRP_TITLE  = new Font("SansSerif", Font.BOLD, 13);  // 蓝色分组标题 (大)
    private static final Font FONT_BTN_NORMAL = new Font("SansSerif", Font.PLAIN, 12); // 普通按钮文字 (中)
    private static final Font FONT_BTN_BOLD   = new Font("SansSerif", Font.BOLD, 13);  // 重点按钮文字 (大粗)
    private static final Font FONT_INPUT      = new Font("SansSerif", Font.PLAIN, 13); // 输入框文字
    private static final Font FONT_MEMORY     = new Font("SansSerif", Font.PLAIN, 10); // 底部内存条 (小)

    // Components
    private JTextField filterField;
    private JButton btnCloseMatch, btnKeepMatch, btnCloseAll;
    private JButton btnTile, btnCascade;
    private JButton btnAutoContrast, btnResetZoom;
    private JProgressBar memoryBar;

    private static WinMan instance;
    private Timer memoryTimer;

    public WinMan() {
        super(""); // 保持简短的 OS 标题，确保有地方拖动
        if (instance != null) {
            instance.toFront();
            return;
        }
        instance = this;
        
        String version = getVersion();
        setCustomIcon("/icons/winman_icon.png");
        
        setLayout(new BorderLayout());
        setBackground(BG_BODY_GRAY); 

        // --- A. Header Section ---
        add(createHeaderPanel(version), BorderLayout.NORTH);

        // --- B. Main Body ---
        JPanel bodyPanel = new JPanel(new GridBagLayout());
        bodyPanel.setBackground(BG_BODY_GRAY); 
        bodyPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15)); 
        
        // 🔥【布局核心】硬性支撑
        // 宽度 300: 足够容纳按钮，且不占太多屏幕
        // 高度 480: 强制撑开纵向空间，防止挤压
        bodyPanel.setPreferredSize(new Dimension(260, 280)); 

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0); // 增加组件组之间的垂直间距
        c.weightx = 1.0;
        c.gridx = 0;

        // Group 1: Filter
        c.gridy = 0;
        bodyPanel.add(createFilterPanel(), c);

        // Group 2: Window Layout
        c.gridy = 1;
        bodyPanel.add(createWindowToolsPanel(), c);

        // Group 3: Image Tools
        c.gridy = 2;
        bodyPanel.add(createImageToolsPanel(), c);
        
        // Group 4: Close All (Spring to bottom)
        c.gridy = 3;
        c.weighty = 0; 
        c.anchor = GridBagConstraints.SOUTH; 
        c.insets = new Insets(20, 0, 5, 0); 
        
        btnCloseAll = createFlatButton("Close ALL Images", false);
        btnCloseAll.setForeground(new Color(220, 50, 50)); 
        btnCloseAll.setFont(FONT_BTN_BOLD); // 使用大号字体
        btnCloseAll.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnCloseAll.setBackground(new Color(255, 235, 235)); }
            public void mouseExited(MouseEvent e) { btnCloseAll.setBackground(BTN_BG_NORMAL); }
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
        p.setBackground(BG_HEADER_WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BTN_BORDER),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        URL imgURL = getClass().getResource("/icons/winman_icon.png");
        if (imgURL != null) {
            ImageIcon originalIcon = new ImageIcon(imgURL);
            Image resizedImg = originalIcon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
            p.add(new JLabel(new ImageIcon(resizedImg)), BorderLayout.WEST);
        }

        // HTML Header: 控制字体大小
        // 标题: 14px Bold Blue
        // 版本号: 9px Gray (更小，更精致)
        String html = "<html>" +
                "<div style='font-family: SansSerif;'>" +
                "<span style='font-size:14px; font-weight:bold; color:rgb(" + TEXT_BLUE.getRed() + "," + TEXT_BLUE.getGreen() + "," + TEXT_BLUE.getBlue() + ");'>WinMan Manager</span><br>" +
                "<span style='font-size:9px; color:gray;'>v" + version + " | © 2026 cns.ac.cn</span>" +
                "</div></html>";
        JLabel titleLbl = new JLabel(html);
        p.add(titleLbl, BorderLayout.CENTER);

        return p;
    }

    private JPanel createFilterPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_BODY_GRAY);
        p.setBorder(createFlatTitledBorder("Filter Selection"));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(8, 8, 8, 8); 
        
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        filterField = new JTextField();
        filterField.setFont(FONT_INPUT); // 使用统一配置
        filterField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BTN_BORDER, 1),
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
        p.setBackground(BG_BODY_GRAY);
        p.setBorder(createFlatTitledBorder("Window Layout"));

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
        p.setBackground(BG_BODY_GRAY);
        p.setBorder(createFlatTitledBorder("Image Tools"));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(8, 8, 8, 4);
        c.weightx = 0.5;

        btnAutoContrast = createFlatButton("Auto Contrast", true);
        btnAutoContrast.setToolTipText("Auto adjust B&C for all images");
        p.add(btnAutoContrast, c);

        c.gridx = 1;
        c.insets = new Insets(8, 4, 8, 8);
        btnResetZoom = createFlatButton("Reset Zoom", true);
        btnResetZoom.setToolTipText("Zoom 100% for all images");
        p.add(btnResetZoom, c);

        return p;
    }

    private JPanel createMemoryFooter() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_BODY_GRAY);
        p.setBorder(BorderFactory.createEmptyBorder(5, 15, 10, 15)); 

        memoryBar = new JProgressBar(0, 100);
        memoryBar.setStringPainted(true);
        memoryBar.setString("Memory: Calculating...");
        memoryBar.setFont(FONT_MEMORY); // 使用统一配置
        memoryBar.setForeground(new Color(100, 180, 100)); 
        memoryBar.setBackground(Color.WHITE);
        memoryBar.setBorder(BorderFactory.createLineBorder(BTN_BORDER));
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
                    imp.getWindow().getCanvas().zoom100Percent();
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
        if (percent > 85) memoryBar.setForeground(new Color(200, 50, 50)); 
        else memoryBar.setForeground(new Color(60, 160, 60)); 
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
        btn.setFont(FONT_BTN_NORMAL); // 使用统一配置
        btn.setFocusPainted(false); 
        btn.setBackground(BTN_BG_NORMAL);
        btn.setForeground(Color.DARK_GRAY);
        btn.setBorder(BorderFactory.createLineBorder(BTN_BORDER, 1));
        btn.setMargin(new Insets(6, 12, 6, 12)); 
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if(btn.isEnabled()) btn.setBackground(BTN_BG_HOVER); }
            public void mouseExited(MouseEvent e) { btn.setBackground(BTN_BG_NORMAL); }
        });
        if (addListener) btn.addActionListener(this);
        return btn;
    }
    private Border createFlatTitledBorder(String title) {
        Border line = BorderFactory.createLineBorder(BTN_BORDER, 1);
        TitledBorder tb = BorderFactory.createTitledBorder(line, title);
        tb.setTitleColor(TEXT_BLUE);
        tb.setTitleFont(FONT_GRP_TITLE); // 使用统一配置
        return tb;
    }
    private void setCustomIcon(String path) {
        URL imgURL = getClass().getResource(path);
        if (imgURL != null) setIconImage(new ImageIcon(imgURL).getImage());
    }
    private String getVersion() {
        try (InputStream s = getClass().getResourceAsStream("/version.properties")) {
            if (s == null) return "dev";
            Properties p = new Properties(); p.load(s);
            return p.getProperty("version", "dev");
        } catch (IOException e) { return "err"; }
    }
    @Override public void run(String arg) {}
    @Override public void windowClosing(WindowEvent e) {
        if (memoryTimer != null) memoryTimer.stop(); 
        super.windowClosing(e);
        instance = null;
    }
}