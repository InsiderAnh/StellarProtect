package io.github.insideranh.stellarprotect.enums;

import lombok.Getter;

@Getter
public enum MoneyVarType {

    VAULT(0),
    PAY(1);

    private final int id;

    MoneyVarType(int id) {
        this.id = id;
    }

    public static MoneyVarType getById(int id) {
        for (MoneyVarType moneyVarType : MoneyVarType.values()) {
            if (moneyVarType.getId() == id) {
                return moneyVarType;
            }
        }
        return null;
    }

}