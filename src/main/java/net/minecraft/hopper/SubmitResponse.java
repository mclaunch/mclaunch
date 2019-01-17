package net.minecraft.hopper;

import net.minecraft.hopper.Crash;
import net.minecraft.hopper.Problem;
import net.minecraft.hopper.Report;
import net.minecraft.hopper.Response;

public class SubmitResponse
extends Response {
    private Report report;
    private Crash crash;
    private Problem problem;

    public Report getReport() {
        return this.report;
    }

    public Crash getCrash() {
        return this.crash;
    }

    public Problem getProblem() {
        return this.problem;
    }
}

