import java.lang.reflect.*;
import java.util.*;
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
                .map(Java::implicitJavaLang)
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

    public static String allInterfaces(Class<?> clazz) {
        LinkedHashSet<Class<?>> result = new LinkedHashSet<>();
        for (Class<?> ancestor : classChainList(clazz)) {
            insertAllInterfaces(ancestor, result);
        }
        if (result.isEmpty()) return "";

        return result.stream()
                .map(Java::implicitJavaLang)
                .collect(Collectors.joining(", ", clazz.isInterface() ? "extends* " : "implements* ", "\n"));
    }

    private static void insertAllInterfaces(Class<?> clazz, LinkedHashSet<Class<?>> result) {
        for (Class<?> directInterface : clazz.getInterfaces()) {
            if (result.add(directInterface)) {
                insertAllInterfaces(directInterface, result);
            }
        }
    }

    public static String sortedConstructors(Class<?> clazz, IntPredicate modifiersFilter, String suffix) {
        return textBlock(suffix, Arrays.stream(clazz.getConstructors())
                .filter(constructor -> modifiersFilter.test(constructor.getModifiers()))
                .sorted(Comparator.comparing(Constructor::getParameterCount))
                .map(constructor -> clazz.getSimpleName() + parameters(constructor)));
    }

    public static String sortedFields(Class<?> clazz, IntPredicate modifiersFilter, String suffix) {
        return textBlock(suffix, Arrays.stream(clazz.getFields())
                .filter(method -> modifiersFilter.test(method.getModifiers()))
                .sorted(Comparator.comparing(Field::getName))
                .map(field -> field.getName() + ": " + implicitJavaLang(field.getType())));
    }

    public static String sortedMethods(Class<?> clazz, IntPredicate modifiersFilter, String suffix) {
        Stream<Method> stream = Arrays.stream(clazz.getMethods());
        if (clazz != Object.class) {
            stream = stream.filter(method -> !rootMethods.contains(method));
        }
        return textBlock(suffix, stream.filter(method -> modifiersFilter.test(method.getModifiers()))
                .sorted(Comparator.comparing(Method::getName).thenComparing(Method::getParameterCount))
                .map(method -> method.getName() + parameters(method) + ": " + implicitJavaLang(method.getReturnType())));
    }

    private static final HashSet<Method> rootMethods = new HashSet<>(Arrays.asList(Object.class.getDeclaredMethods()));

    private static String parameters(Executable executable) {
        return Arrays.stream(executable.getParameters())
                .map(Java::parameter)
                .collect(Collectors.joining(", ", "(", ")"));
    }

    private static String parameter(Parameter parameter) {
        String name = parameter.getName();
        String type = implicitJavaLang(parameter.getType());
        if (SYNTHESIZED_PARAMETER.matcher(name).matches()) {
            return type;
        } else {
            return name + ": " + type;
        }
    }

    private static final Pattern SYNTHESIZED_PARAMETER = Pattern.compile("arg\\d+");

    private static String implicitJavaLang(Class<?> type) {
        String name = type.getTypeName();
        Matcher matcher = JAVA_LANG.matcher(name);
        return matcher.matches() ? matcher.group(1) : name;
    }

    private static final Pattern JAVA_LANG = Pattern.compile("java\\.lang\\.([^.]+)");

    private static String textBlock(String suffix, Stream<String> stream) {
        StringBuilder builder = new StringBuilder();
        stream.forEach(string -> builder.append(string).append("\n"));
        return (builder.length() == 0) ? "" : builder.append(suffix).toString();
    }
}
