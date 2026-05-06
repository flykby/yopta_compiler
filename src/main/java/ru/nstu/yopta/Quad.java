package ru.nstu.yopta;

/**
 * Тетрада: операция и три адреса (два операнда и результат), как во внутренней форме выражения.
 */
public final class Quad {

    private final String op;
    private final String arg1;
    private final String arg2;
    private final String result;

    public Quad(String op, String arg1, String arg2, String result) {
        this.op = op != null ? op : "";
        this.arg1 = arg1 != null ? arg1 : "";
        this.arg2 = arg2 != null ? arg2 : "";
        this.result = result != null ? result : "";
    }

    public String getOp() {
        return op;
    }

    public String getArg1() {
        return arg1;
    }

    public String getArg2() {
        return arg2;
    }

    public String getResult() {
        return result;
    }
}
