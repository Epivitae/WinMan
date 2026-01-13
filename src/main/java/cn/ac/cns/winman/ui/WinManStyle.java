// Placeholder for WinManStyle.java
package cn.ac.cns.winman.ui;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import java.awt.Font;

/**
 * WinMan UI 样式配置中心
 * 集中管理颜色、字体和边框样式
 */
public class WinManStyle {

    // --- 调色板 ---
    public static final Color BG_HEADER_WHITE = Color.WHITE;
    public static final Color BG_BODY_GRAY    = new Color(245, 247, 249);
    public static final Color TEXT_BLUE       = new Color(51, 102, 204);
    public static final Color BTN_BG_NORMAL   = Color.WHITE;
    public static final Color BTN_BG_HOVER    = new Color(235, 240, 245);
    public static final Color BTN_BORDER      = new Color(200, 200, 200);
    public static final Color BTN_SYNC_ON     = new Color(220, 240, 255);
    public static final Color MEM_TEXT_GREEN  = new Color(60, 160, 60);
    public static final Color MEM_TEXT_RED    = new Color(200, 50, 50);
    public static final Color CLOSE_ALL_RED   = new Color(220, 50, 50);
    public static final Color CLOSE_ALL_BG_HOVER = new Color(255, 235, 235);

    // --- 字体配置 ---
    public static final Font FONT_GRP_TITLE  = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_BTN_NORMAL = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_BTN_BOLD   = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_INPUT      = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_MEMORY     = new Font("SansSerif", Font.PLAIN, 10);

    // --- 样式工厂方法 ---
    
    /**
     * 创建扁平化的带标题边框
     */
    public static Border createFlatTitledBorder(String title) {
        Border line = BorderFactory.createLineBorder(BTN_BORDER, 1);
        TitledBorder tb = BorderFactory.createTitledBorder(line, title);
        tb.setTitleColor(TEXT_BLUE);
        tb.setTitleFont(FONT_GRP_TITLE);
        return tb;
    }
}