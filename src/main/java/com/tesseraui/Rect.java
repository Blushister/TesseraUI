package com.tesseraui;

public record Rect(int x, int y, int w, int h) {

    public boolean contains(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
