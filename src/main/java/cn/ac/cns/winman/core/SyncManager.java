package cn.ac.cns.winman.core;

import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.gui.RoiListener;

import javax.swing.SwingUtilities;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * 负责多窗口同步的核心逻辑 (Slice, Zoom, Pan, ROI)
 * v2.1.0 - Added ROI Synchronization
 */
public class SyncManager implements ImageListener, RoiListener, MouseMotionListener, MouseWheelListener {
    
    private boolean active = false;
    private boolean updating = false; // 防止死循环锁

    public void start() {
        if (active) return;
        active = true;
        // 1. 监听图片开关
        ImagePlus.addImageListener(this); 
        // 2. 监听 ROI 变化 (核心新增)
        Roi.addRoiListener(this);
        // 3. 绑定鼠标事件 (Pan/Zoom)
        attachToOpenWindows();
    }

    public void stop() {
        if (!active) return;
        active = false;
        ImagePlus.removeImageListener(this);
        Roi.removeRoiListener(this);
        detachFromOpenWindows();
    }

    // --- 绑定逻辑 (用于 Pan/Zoom) ---
    
    private void attachToOpenWindows() {
        int[] list = WindowManager.getIDList();
        if (list == null) return;
        for (int id : list) {
            attach(WindowManager.getImage(id));
        }
    }
    
    private void detachFromOpenWindows() {
        int[] list = WindowManager.getIDList();
        if (list == null) return;
        for (int id : list) {
            detach(WindowManager.getImage(id));
        }
    }
    
    private void attach(ImagePlus imp) {
        if (imp == null || imp.getWindow() == null || imp.getCanvas() == null) return;
        ImageCanvas ic = imp.getCanvas();
        ic.removeMouseMotionListener(this);
        ic.removeMouseWheelListener(this);
        ic.addMouseMotionListener(this);
        ic.addMouseWheelListener(this);
    }
    
    private void detach(ImagePlus imp) {
        if (imp == null || imp.getCanvas() == null) return;
        imp.getCanvas().removeMouseMotionListener(this);
        imp.getCanvas().removeMouseWheelListener(this);
    }

    // --- 同步核心算法 (Slice, Zoom, Pan) ---
    
    private void syncView(ImagePlus source) {
        if (updating || source == null) return;
        try {
            updating = true;
            int currentSlice = source.getCurrentSlice();
            ImageCanvas srcCanvas = source.getCanvas();
            if (srcCanvas == null) return;
            
            Rectangle srcRect = srcCanvas.getSrcRect();
            double mag = srcCanvas.getMagnification();

            int[] list = WindowManager.getIDList();
            if (list == null) return;

            for (int id : list) {
                ImagePlus target = WindowManager.getImage(id);
                // 排除自己，且只同步尺寸完全一致的窗口
                if (isValidTarget(source, target)) {
                    
                    // 1. Sync Slice (时间/Z轴)
                    if (target.getStackSize() > 1 && target.getCurrentSlice() != currentSlice) {
                         if (currentSlice <= target.getStackSize()) {
                             target.setSlice(currentSlice);
                         }
                    }
                    // 2. Sync Pan & Zoom (平移和缩放)
                    ImageCanvas tgtCanvas = target.getCanvas();
                    if (tgtCanvas != null) {
                        if (tgtCanvas.getMagnification() != mag || !tgtCanvas.getSrcRect().equals(srcRect)) {
                            tgtCanvas.setMagnification(mag);
                            tgtCanvas.setSourceRect(new Rectangle(srcRect));
                            tgtCanvas.repaint();
                        }
                    }
                }
            }
        } finally {
            updating = false;
        }
    }

    // --- ROI 同步核心算法 (新增) ---

    private void syncRoi(ImagePlus source, Roi roi) {
        if (updating || source == null) return;
        try {
            updating = true;
            int[] list = WindowManager.getIDList();
            if (list == null) return;

            for (int id : list) {
                ImagePlus target = WindowManager.getImage(id);
                if (isValidTarget(source, target)) {
                    if (roi == null) {
                        target.deleteRoi(); // 如果源取消了选区，目标也取消
                    } else {
                        // 克隆 ROI，防止同一个对象被多个图引用导致混乱
                        target.setRoi((Roi) roi.clone());
                    }
                }
            }
        } finally {
            updating = false;
        }
    }

    // 辅助检查：是否为合法的同步目标
    private boolean isValidTarget(ImagePlus source, ImagePlus target) {
        return target != null && target != source && 
               target.getWidth() == source.getWidth() && 
               target.getHeight() == source.getHeight();
    }

    // --- 接口实现 ---
    
    // 1. ImageListener (监听窗口开关/更新)
    @Override public void imageOpened(ImagePlus imp) { attach(imp); }
    @Override public void imageClosed(ImagePlus imp) { detach(imp); }
    @Override public void imageUpdated(ImagePlus imp) { 
        if (active && imp == WindowManager.getCurrentImage()) {
            syncView(imp);
        }
    }

    // 2. RoiListener (监听 ROI 变化 - 核心)
    @Override
    public void roiModified(ImagePlus imp, int id) {
        if (!active) return;
        // 只有当前操作的窗口才触发广播，防止死循环
        if (imp == WindowManager.getCurrentImage()) {
            // 事件 ID: CREATED(1), MOVED(2), MODIFIED(3), EXTENDED(4), COMPLETED(5), DELETED(6)
            // 我们统统同步
            syncRoi(imp, imp.getRoi());
        }
    }
    
    // 3. Mouse Listeners (监听拖拽/滚轮)
    @Override public void mouseDragged(MouseEvent e) {
        if (active) syncView(WindowManager.getCurrentImage());
    }
    
    @Override public void mouseWheelMoved(MouseWheelEvent e) {
        SwingUtilities.invokeLater(() -> {
            if (active) syncView(WindowManager.getCurrentImage());
        });
    }
    
    @Override public void mouseMoved(MouseEvent e) {}
}