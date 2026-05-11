package com.tesseraui;

public final class TesseraInputState {
    public String text = "";
    public int cursor = 0;
    public int selStart = 0;
    public int scrollX = 0;
    public int scrollY = 0;   // used by TesseraTextArea (vertical scroll offset in px)
    public boolean focused = false;
    public long focusStartMs = 0;
}
