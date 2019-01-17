package com.mojang.launcher.game;

public enum GameInstanceStatus {
    PREPARING("Preparing..."),
    DOWNLOADING("Downloading..."),
    INSTALLING("Installing..."),
    LAUNCHING("Launching..."),
    PLAYING("Playing..."),
    IDLE("Idle");
    
    private final String name;

    private GameInstanceStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String toString() {
        return this.name;
    }
}

