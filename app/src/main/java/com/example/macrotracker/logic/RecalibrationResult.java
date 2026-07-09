package com.example.macrotracker.logic;

import com.example.macrotracker.models.TargetMacros;

import java.math.BigDecimal;
public class RecalibrationResult {

    public enum Status {SUCCESS, INSUFFICIENT_DATA}

    private final Status status;
    private final BigDecimal formulaTdee;
    private final BigDecimal realTdee;
    private final TargetMacros suggestedTarget;
    private final String message;

    private RecalibrationResult(Status status, BigDecimal formulaTdee, BigDecimal realTdee, TargetMacros suggestedTarget, String message) {
        this.status = status;
        this.formulaTdee = formulaTdee;
        this.realTdee = realTdee;
        this.suggestedTarget = suggestedTarget;
        this.message = message;
    }

    public static RecalibrationResult insufficientData(String reason) {
        return new RecalibrationResult(Status.INSUFFICIENT_DATA, null, null, null, reason);
    }

    public static RecalibrationResult success(BigDecimal formulaTdee, BigDecimal realTdee, TargetMacros suggestedTarget) {
        String message = realTdee.compareTo(formulaTdee) > 0
                ? "Metabolism higher than estimated, target calories will increase."
                : realTdee.compareTo(formulaTdee) < 0
                ? "Metabolism lower than estimated, target calories will decrease."
                : "Metabolism matches estimate, no change needed.";
        return new RecalibrationResult(Status.SUCCESS, formulaTdee, realTdee, suggestedTarget, message);
    }

    // Getters
    public Status getStatus() {return status;}
    public BigDecimal getFormulaTdee() {return formulaTdee;}
    public BigDecimal getRealTdee() {return realTdee;}
    public TargetMacros getSuggestedTarget() {return suggestedTarget;}
    public String getMessage() {return message;}
}
