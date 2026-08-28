package com.autosoft.workflow.graph;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.workflow.exec.RunContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 受控条件 DSL。禁止 eval / ScriptEngine。
 */
public final class ConditionDsl {

    private ConditionDsl() {
    }

    public static void validate(String expr) {
        parse(expr);
    }

    public static boolean evaluate(String expr, RunContext context) {
        return asBoolean(parse(expr).eval(context));
    }

    static Expr parse(String expr) {
        if (expr == null || expr.isBlank()) {
            throw new BizException(ResultCode.BAD_REQUEST, "condition 表达式不能为空");
        }
        Parser parser = new Parser(tokenize(expr));
        Expr parsed = parser.parseExpr();
        parser.expectEnd();
        return parsed;
    }

    private static List<Token> tokenize(String expr) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '(') {
                tokens.add(new Token(Kind.LPAREN, "("));
                i++;
                continue;
            }
            if (c == ')') {
                tokens.add(new Token(Kind.RPAREN, ")"));
                i++;
                continue;
            }
            if (c == '.') {
                tokens.add(new Token(Kind.DOT, "."));
                i++;
                continue;
            }
            if (c == '=' && i + 1 < expr.length() && expr.charAt(i + 1) == '=') {
                tokens.add(new Token(Kind.OP, "=="));
                i += 2;
                continue;
            }
            if (c == '!' && i + 1 < expr.length() && expr.charAt(i + 1) == '=') {
                tokens.add(new Token(Kind.OP, "!="));
                i += 2;
                continue;
            }
            if (c == '<' || c == '>') {
                if (i + 1 < expr.length() && expr.charAt(i + 1) == '=') {
                    tokens.add(new Token(Kind.OP, expr.substring(i, i + 2)));
                    i += 2;
                } else {
                    tokens.add(new Token(Kind.OP, String.valueOf(c)));
                    i++;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                int start = i + 1;
                int end = expr.indexOf(c, start);
                if (end < 0) {
                    throw new BizException(ResultCode.BAD_REQUEST, "字符串未闭合");
                }
                tokens.add(new Token(Kind.STRING, expr.substring(start, end)));
                i = end + 1;
                continue;
            }
            if (c == '-' || Character.isDigit(c)) {
                int start = i;
                i++;
                while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                    i++;
                }
                tokens.add(new Token(Kind.NUMBER, expr.substring(start, i)));
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                int start = i;
                i++;
                while (i < expr.length() && (Character.isLetterOrDigit(expr.charAt(i)) || expr.charAt(i) == '_')) {
                    i++;
                }
                String word = expr.substring(start, i);
                if ("true".equals(word) || "false".equals(word)) {
                    tokens.add(new Token(Kind.BOOL, word));
                } else {
                    tokens.add(new Token(Kind.IDENT, word));
                }
                continue;
            }
            throw new BizException(ResultCode.BAD_REQUEST, "非法 token: " + c);
        }
        tokens.add(new Token(Kind.EOF, ""));
        return tokens;
    }

    private static boolean asBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        throw new BizException(ResultCode.BAD_REQUEST, "条件结果不是布尔值");
    }

    private enum Kind {IDENT, NUMBER, STRING, BOOL, OP, LPAREN, RPAREN, DOT, EOF}

    private record Token(Kind kind, String text) {
    }

    interface Expr {
        Object eval(RunContext context);
    }

    private static final class Parser {
        private final List<Token> tokens;
        private int idx;

        Parser(List<Token> tokens) {
            this.tokens = tokens;
        }

        Expr parseExpr() {
            Token first = peek();
            if (first.kind == Kind.IDENT && ("empty".equals(first.text) || "daysUntil".equals(first.text))) {
                return parseFunc();
            }
            Expr left = parsePath();
            Token op = peek();
            if (op.kind != Kind.OP) {
                throw new BizException(ResultCode.BAD_REQUEST, "条件必须是比较或函数调用");
            }
            next();
            Expr right = parseValue();
            return new CompareExpr(left, op.text, right);
        }

        private Expr parseFunc() {
            String name = next().text;
            expect(Kind.LPAREN);
            Expr path = parsePath();
            expect(Kind.RPAREN);
            if ("empty".equals(name)) {
                return new EmptyExpr(path);
            }
            if ("daysUntil".equals(name)) {
                Token op = peek();
                if (op.kind != Kind.OP) {
                    throw new BizException(ResultCode.BAD_REQUEST, "daysUntil 必须与数字比较");
                }
                next();
                Expr right = parseValue();
                return new CompareExpr(new DaysUntilExpr(path), op.text, right);
            }
            throw new BizException(ResultCode.BAD_REQUEST, "未知函数: " + name);
        }

        private Expr parsePath() {
            Token ident = expect(Kind.IDENT);
            if ("empty".equals(ident.text) || "daysUntil".equals(ident.text)) {
                throw new BizException(ResultCode.BAD_REQUEST, "函数必须带括号");
            }
            String field = null;
            if (peek().kind == Kind.DOT) {
                next();
                field = expect(Kind.IDENT).text;
            }
            return new PathExpr(ident.text, field);
        }

        private Expr parseValue() {
            Token token = peek();
            if (token.kind == Kind.NUMBER) {
                next();
                return new LiteralExpr(new BigDecimal(token.text));
            }
            if (token.kind == Kind.STRING) {
                next();
                return new LiteralExpr(token.text);
            }
            if (token.kind == Kind.BOOL) {
                next();
                return new LiteralExpr(Boolean.parseBoolean(token.text));
            }
            if (token.kind == Kind.IDENT) {
                return parsePath();
            }
            throw new BizException(ResultCode.BAD_REQUEST, "非法字面量");
        }

        private Token peek() {
            return tokens.get(idx);
        }

        private Token next() {
            return tokens.get(idx++);
        }

        private Token expect(Kind kind) {
            Token token = next();
            if (token.kind != kind) {
                throw new BizException(ResultCode.BAD_REQUEST, "表达式语法错误，期望 " + kind);
            }
            return token;
        }

        void expectEnd() {
            if (peek().kind != Kind.EOF) {
                throw new BizException(ResultCode.BAD_REQUEST, "表达式存在多余内容");
            }
        }
    }

    private record PathExpr(String root, String field) implements Expr {
        @Override
        public Object eval(RunContext context) {
            Object source;
            if ("input".equals(root)) {
                source = context.input();
            } else {
                source = context.outputs().get(root);
            }
            if (field == null) {
                return source;
            }
            if (source instanceof Map<?, ?> map) {
                Object direct = map.get(field);
                if (direct != null) {
                    return direct;
                }
                return map.get(field.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT));
            }
            return null;
        }
    }

    private record LiteralExpr(Object value) implements Expr {
        @Override
        public Object eval(RunContext context) {
            return value;
        }
    }

    private record EmptyExpr(Expr inner) implements Expr {
        @Override
        public Object eval(RunContext context) {
            Object value = inner.eval(context);
            if (value == null) {
                return true;
            }
            if (value instanceof String s) {
                return s.isBlank();
            }
            if (value instanceof Map<?, ?> map) {
                return map.isEmpty();
            }
            if (value instanceof List<?> list) {
                return list.isEmpty();
            }
            return false;
        }
    }

    private record DaysUntilExpr(Expr inner) implements Expr {
        @Override
        public Object eval(RunContext context) {
            LocalDate date = toDate(inner.eval(context));
            if (date == null) {
                throw new BizException(ResultCode.BAD_REQUEST, "daysUntil 不是合法日期");
            }
            return BigDecimal.valueOf(ChronoUnit.DAYS.between(LocalDate.now(ZoneOffset.UTC), date));
        }
    }

    private record CompareExpr(Expr left, String op, Expr right) implements Expr {
        @Override
        public Object eval(RunContext context) {
            Object a = left.eval(context);
            Object b = right.eval(context);
            int cmp = compare(a, b, op);
            return switch (op) {
                case "==" -> cmp == 0;
                case "!=" -> cmp != 0;
                case "<" -> cmp < 0;
                case ">" -> cmp > 0;
                case "<=" -> cmp <= 0;
                case ">=" -> cmp >= 0;
                default -> throw new BizException(ResultCode.BAD_REQUEST, "不支持的比较符: " + op);
            };
        }
    }

    private static int compare(Object a, Object b, String op) {
        if (a == null || b == null) {
            if ("==".equals(op) || "!=".equals(op)) {
                return a == null && b == null ? 0 : 1;
            }
            throw new BizException(ResultCode.BAD_REQUEST, "空值不能做大小比较");
        }
        if (a instanceof Boolean || b instanceof Boolean) {
            return Boolean.compare(asBool(a), asBool(b));
        }
        BigDecimal na = tryNumber(a);
        BigDecimal nb = tryNumber(b);
        if (na != null && nb != null) {
            return na.compareTo(nb);
        }
        return String.valueOf(a).compareTo(String.valueOf(b));
    }

    private static boolean asBool(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        String text = String.valueOf(value);
        if ("true".equalsIgnoreCase(text) || "1".equals(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text) || "0".equals(text)) {
            return false;
        }
        throw new BizException(ResultCode.BAD_REQUEST, "无法比较布尔值");
    }

    private static BigDecimal tryNumber(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static LocalDate toDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof java.util.Date date) {
            return Instant.ofEpochMilli(date.getTime()).atZone(ZoneOffset.UTC).toLocalDate();
        }
        if (value instanceof Instant instant) {
            return LocalDate.ofInstant(instant, ZoneOffset.UTC);
        }
        if (value instanceof OffsetDateTime odt) {
            return odt.toLocalDate();
        }
        String text = String.valueOf(value).trim();
        if (text.length() >= 10 && text.charAt(4) == '-') {
            return LocalDate.parse(text.substring(0, 10));
        }
        return null;
    }
}
