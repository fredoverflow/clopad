import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Java {
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
                .map(field -> field.getName() + ": " + withoutJavaLang(field.getType())));
    }

    public static String sortedMethods(Class<?> clazz, IntPredicate modifiersFilter, String suffix) {
        Stream<Method> stream = Arrays.stream(clazz.getMethods());
        if (clazz != Object.class) {
            stream = stream.filter(method -> !rootMethods.contains(method));
        }
        return textBlock(suffix, stream.filter(method -> modifiersFilter.test(method.getModifiers()))
                .sorted(Comparator.comparing(Method::getName).thenComparing(Method::getParameterCount))
                .map(method -> method.getName() + parameters(method) + ": " + withoutJavaLang(method.getReturnType())));
    }

    private static final HashSet<Method> rootMethods = new HashSet<>(Arrays.asList(Object.class.getDeclaredMethods()));

    private static String parameters(Executable executable) {
        return Arrays.stream(executable.getParameters())
                .map(Java::parameter)
                .collect(Collectors.joining(", ", "(", ")"));
    }

    private static String parameter(Parameter parameter) {
        String name = parameter.getName();
        String type = withoutJavaLang(parameter.getType());
        if (SYNTHESIZED_PARAMETER.matcher(name).matches()) {
            return type;
        } else {
            return name + ": " + type;
        }
    }

    private static final Pattern SYNTHESIZED_PARAMETER = Pattern.compile("arg\\d+");

    private static String withoutJavaLang(Class<?> type) {
        String name = type.getTypeName();
        if (name.startsWith("java.lang.")) {
            return name.substring(10);
        } else {
            return name;
        }
    }

    private static String textBlock(String suffix, Stream<String> stream) {
        StringBuilder builder = new StringBuilder();
        stream.forEach(string -> builder.append(string).append("\n"));
        return (builder.length() == 0) ? "" : builder.append(suffix).toString();
    }
}
