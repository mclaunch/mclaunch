package net.minecraft.hopper;

import net.minecraft.hopper.Report;

public class PublishRequest {
    private String token;
    private int report_id;

    public PublishRequest(Report report) {
        this.report_id = report.getId();
        this.token = report.getToken();
    }
}

