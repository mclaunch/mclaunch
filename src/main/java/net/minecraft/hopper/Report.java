package net.minecraft.hopper;

public class Report {
    private int id;
    private boolean published;
    private String token;

    public int getId() {
        return this.id;
    }

    public boolean isPublished() {
        return this.published;
    }

    public String getToken() {
        return this.token;
    }

    public boolean canBePublished() {
        return this.getToken() != null;
    }
}

