package cn.ac.cns.winman;

import ij.IJ;
import ij.ImageJ;

public class WinMan_Debug {
    public static void main(final String... args) throws Exception {
        new ImageJ();
        IJ.open("http://imagej.net/images/clown.jpg");
        IJ.runPlugIn(WinMan.class.getName(), "");
    }
}
