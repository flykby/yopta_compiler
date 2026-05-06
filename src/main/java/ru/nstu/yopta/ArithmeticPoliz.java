package ru.nstu.yopta;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;

/**
 * Построение ПОЛИЗ (сортировочная станция) и вычисление для выражений только из целых литералов.
 * Операции {@code * / % **} одного уровня приоритета (выше {@code + -}).
 */
public final class ArithmeticPoliz {

    private static final Set<String> OP_TOKENS = Set.of("+", "-", "*", "/", "%", "**");

    private ArithmeticPoliz() {
    }

    /** true, если в цепочке нет идентификаторов — допускаются только числа, операторы и скобки. */
    public static boolean isIntegerLiteralExpression(List<Lexeme> tokens) {
        if (tokens == null) {
            return false;
        }
        for (Lexeme t : tokens) {
            if (t.getCode() == ArithmeticScanner.CODE_IDENT) {
                return false;
            }
        }
        return true;
    }

    public static String toPolizString(List<Lexeme> tokens) {
        List<String> out = toPolizTokens(tokens);
        return String.join(" ", out);
    }

    static List<String> toPolizTokens(List<Lexeme> tokens) {
        List<String> output = new ArrayList<>();
        Deque<Lexeme> stack = new ArrayDeque<>();
        for (Lexeme t : tokens) {
            int code = t.getCode();
            if (code == ArithmeticScanner.CODE_NUMBER) {
                output.add(t.getText());
            } else if (code == ArithmeticScanner.CODE_IDENT) {
                output.add(t.getText());
            } else if (isOperator(code)) {
                while (!stack.isEmpty() && isOperator(stack.peek().getCode())) {
                    int top = stack.peek().getCode();
                    if (precedence(top) >= precedence(code)) {
                        output.add(opSymbol(stack.pop().getCode()));
                    } else {
                        break;
                    }
                }
                stack.push(t);
            } else if (code == ArithmeticScanner.CODE_LPAREN) {
                stack.push(t);
            } else if (code == ArithmeticScanner.CODE_RPAREN) {
                while (!stack.isEmpty() && stack.peek().getCode() != ArithmeticScanner.CODE_LPAREN) {
                    output.add(opSymbol(stack.pop().getCode()));
                }
                if (!stack.isEmpty() && stack.peek().getCode() == ArithmeticScanner.CODE_LPAREN) {
                    stack.pop();
                }
            }
        }
        while (!stack.isEmpty()) {
            output.add(opSymbol(stack.pop().getCode()));
        }
        return output;
    }

    public static String evaluateIntegerPoliz(List<Lexeme> tokens) {
        if (!isIntegerLiteralExpression(tokens)) {
            return "";
        }
        List<String> rpn = toPolizTokens(tokens);
        Deque<Long> st = new ArrayDeque<>();
        try {
            for (String s : rpn) {
                if (isOperatorString(s)) {
                    if (st.size() < 2) {
                        return Messages.getString("arith.poliz.evalErr");
                    }
                    long b = st.pop();
                    long a = st.pop();
                    String err = evalBinary(a, b, s, st);
                    if (err != null) {
                        return err;
                    }
                } else {
                    st.push(Long.parseLong(s));
                }
            }
            if (st.size() != 1) {
                return Messages.getString("arith.poliz.evalErr");
            }
            return Long.toString(st.pop());
        } catch (ArithmeticException e) {
            return Messages.getString("arith.poliz.overflow");
        } catch (NumberFormatException e) {
            return Messages.getString("arith.poliz.evalErr");
        }
    }

    /** @return сообщение об ошибке или {@code null}, если значение записано в {@code st} */
    private static String evalBinary(long a, long b, String op, Deque<Long> st) {
        try {
            switch (op) {
                case "+" -> st.push(Math.addExact(a, b));
                case "-" -> st.push(Math.subtractExact(a, b));
                case "*" -> st.push(Math.multiplyExact(a, b));
                case "/" -> {
                    if (b == 0) {
                        return Messages.getString("arith.poliz.divZero");
                    }
                    st.push(a / b);
                }
                case "%" -> {
                    if (b == 0) {
                        return Messages.getString("arith.poliz.modZero");
                    }
                    st.push(a % b);
                }
                case "**" -> st.push(longPowExact(a, b));
                default -> {
                    return Messages.getString("arith.poliz.evalErr");
                }
            }
        } catch (ArithmeticException e) {
            return Messages.getString("arith.poliz.overflow");
        }
        return null;
    }

    /**
     * Целочисленное возведение в степень; {@code b < 0} — ошибка.
     * Переполнение — {@link ArithmeticException} (обрабатывается в {@link #evaluateIntegerPoliz}).
     */
    static long longPowExact(long base, long exp) {
        if (exp < 0) {
            throw new ArithmeticException("neg exp");
        }
        if (exp == 0) {
            return 1;
        }
        long result = 1;
        long b = base;
        long e = exp;
        while (e > 0) {
            if ((e & 1) == 1) {
                result = Math.multiplyExact(result, b);
            }
            e >>>= 1;
            if (e > 0) {
                b = Math.multiplyExact(b, b);
            }
        }
        return result;
    }

    private static boolean isOperatorString(String s) {
        return OP_TOKENS.contains(s);
    }

    private static boolean isOperator(int code) {
        return code == ArithmeticScanner.CODE_PLUS
                || code == ArithmeticScanner.CODE_MINUS
                || code == ArithmeticScanner.CODE_MULT
                || code == ArithmeticScanner.CODE_DIV
                || code == ArithmeticScanner.CODE_MOD
                || code == ArithmeticScanner.CODE_POW;
    }

    /** Уровень 2: * / % **; уровень 1: + -. */
    private static int precedence(int code) {
        if (code == ArithmeticScanner.CODE_MULT
                || code == ArithmeticScanner.CODE_DIV
                || code == ArithmeticScanner.CODE_MOD
                || code == ArithmeticScanner.CODE_POW) {
            return 2;
        }
        if (code == ArithmeticScanner.CODE_PLUS || code == ArithmeticScanner.CODE_MINUS) {
            return 1;
        }
        return 0;
    }

    private static String opSymbol(int code) {
        return switch (code) {
            case ArithmeticScanner.CODE_PLUS -> "+";
            case ArithmeticScanner.CODE_MINUS -> "-";
            case ArithmeticScanner.CODE_MULT -> "*";
            case ArithmeticScanner.CODE_DIV -> "/";
            case ArithmeticScanner.CODE_MOD -> "%";
            case ArithmeticScanner.CODE_POW -> "**";
            default -> "";
        };
    }
}
