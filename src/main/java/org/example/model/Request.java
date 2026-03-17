package org.example.model;

public record Request(String command, String args) {

    public static Request parse(String raw) {
        String line = raw.strip();
        int space = line.indexOf(' ');
        if (space == -1) {
            return new Request(line.toUpperCase(), "");
        }
        return new Request(line.substring(0, space).toUpperCase(), line.substring(space + 1));
    }
}
