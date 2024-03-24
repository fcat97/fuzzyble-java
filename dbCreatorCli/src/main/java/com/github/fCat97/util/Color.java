package com.github.fCat97.util;

public enum Color {
    RED("\u001B[31m"), GREEN("\u001B[32m"), YELLOW("\u001B[33m"), RESET("\u001B[0m");

    private final String color;

    Color(String color) {
        this.color = color;
    }

    public String getColor() {
        return this.color;
    }
}
