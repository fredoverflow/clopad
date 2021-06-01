import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.function.IntPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Java {
    public static String classChain(Class<?> clazz) {
        ArrayList<Class<?>> result = classChainList(clazz);
        if (result.size() < 2) return "";

        return result.stream()
                .map(Java::shrinkLangPackages)
                .collect(Collectors.joining(" -> ", "", "\n"));
    }

    private static ArrayList<Class<?>> classChainList(Class<?> clazz) {
        ArrayList<Class<?>> result = new ArrayList<>();
        while (clazz != null) {
            result.add(clazz);
            clazz = clazz.getSuperclass();
        }
        return result;
    }

    public static String allInterfaces(Class<?> clazz, int columns) {
        HashSet<Class<?>> result = new HashSet<>();
        for (Class<?> ancestor : classChainList(clazz)) {
            insertAllInterfaces(ancestor, result);
        }
        if (result.isEmpty()) return "";

        String prefix;
        String indent;
        if (clazz.isInterface()) {
            prefix = "extends* ";
            indent = "         ";
        } else {
            prefix = "implements* ";
            indent = "            ";
        }
        Stream<String> stream = result.stream()
                .sorted(Comparator.comparing(Class::getSimpleName))
                .map(Java::shrinkLangPackages);
        return join2D(columns, stream::iterator, prefix, ", ", indent, "\n");
    }

    private static void insertAllInterfaces(Class<?> clazz, HashSet<Class<?>> result) {
        for (Class<?> directInterface : clazz.getInterfaces()) {
            if (result.add(directInterface)) {
                insertAllInterfaces(directInterface, result);
            }
        }
    }

    private static String join2D(int columns, Iterable<String> strings, String prefix, String delimiter, String indent, String suffix) {
        StringBuilder builder = new StringBuilder(prefix);
        int width = prefix.length();
        String delim = "";
        for (String s : strings) {
            builder.append(delim);
            width += delim.length();
            if (width + s.length() > columns) {
                builder.append("\n").append(indent);
                width = indent.length();
            }
            builder.append(s);
            width += s.length();
            delim = delimiter;
        }
        return builder.append(suffix).toString();
    }

    public static String sortedConstructors(Class<?> clazz, IntPredicate modifiersFilter, String suffix) {
        return textBlock(suffix, Arrays.stream(clazz.getConstructors())
                .filter(constructor -> modifiersFilter.test(constructor.getModifiers()))
                .sorted(Comparator.comparing(Constructor::getParameterCount))
                .map(constructor -> clazz.getSimpleName() + parameters(constructor) + exceptionTypes(constructor)));
    }

    public static String sortedFields(Class<?> clazz, IntPredicate modifiersFilter, String suffix) {
        return textBlock(suffix, Arrays.stream(clazz.getFields())
                .filter(method -> modifiersFilter.test(method.getModifiers()))
                .sorted(Comparator.comparing(Field::getName))
                .map(field -> field.getName() + ": " + shrinkLangPackages(field.getType())));
    }

    public static String sortedMethods(Class<?> clazz, IntPredicate modifiersFilter, String suffix) {
        Stream<Method> stream = Arrays.stream(clazz.getMethods());
        if (clazz != Object.class) {
            stream = stream.filter(method -> !rootMethods.contains(method));
        }
        return textBlock(suffix, stream.filter(method -> modifiersFilter.test(method.getModifiers()))
                .sorted(Comparator.comparing(Method::getName).thenComparing(Method::getParameterCount))
                .map(method -> method.getName() + parameters(method) + ": " + shrinkLangPackages(method.getReturnType()) + exceptionTypes(method)));
    }

    private static final HashSet<Method> rootMethods = new HashSet<>(Arrays.asList(Object.class.getDeclaredMethods()));

    private static String parameters(Executable executable) {
        return Arrays.stream(executable.getParameters())
                .map(Java::parameter)
                .collect(Collectors.joining(", ", "(", ")"));
    }

    private static String parameter(Parameter parameter) {
        String name = parameter.getName();
        String type = shrinkLangPackages(parameter.getType());
        if (SYNTHESIZED_PARAMETER.matcher(name).matches()) {
            return type;
        } else {
            return name + ": " + type;
        }
    }

    private static String exceptionTypes(Executable executable) {
        Class<?>[] exceptionTypes = executable.getExceptionTypes();
        if (exceptionTypes.length == 0) return "";

        return Arrays.stream(exceptionTypes)
                .map(Java::shrinkLangPackages)
                .collect(Collectors.joining(", ", " (", ")"));
    }

    private static final Pattern SYNTHESIZED_PARAMETER = Pattern.compile("arg\\d+");

    private static String shrinkLangPackages(Class<?> type) {
        String name = type.getTypeName();
        Matcher matcher = JAVA_LANG.matcher(name);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        matcher = CLOJURE_LANG.matcher(name);
        if (matcher.matches()) {
            return '©' + matcher.group(1);
        }
        return name;
    }

    private static final Pattern JAVA_LANG = Pattern.compile("java[.]lang[.]([^.]+)");
    private static final Pattern CLOJURE_LANG = Pattern.compile("clojure[.]lang[.](.+)");

    public static String expandClojureLangPackage(String lexeme) {
        return (lexeme.charAt(0) == '©') ? "clojure.lang." + lexeme.substring(1) : lexeme;
    }

    private static String textBlock(String suffix, Stream<String> stream) {
        StringBuilder builder = new StringBuilder();
        stream.forEach(string -> builder.append(string).append("\n"));
        return (builder.length() == 0) ? "" : builder.append(suffix).toString();
    }
}
