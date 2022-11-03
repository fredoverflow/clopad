public class SpecialForm {
    public static String help(String lexeme) {
        switch (lexeme) {
            case ".":
                return "  (.instanceMember instance args*)\n" +
                        "  (.instanceMember Classname args*)\n" +
                        "  (Classname/staticMethod args*)\n" +
                        "  Classname/staticField\n" +
                        "Special Form\n" +
                        "  The instance member form works for both fields and methods.\n" +
                        "  They all expand into calls to the dot operator at macroexpansion time.";
            case "def":
                return "  (def symbol doc-string? init?)\n" +
                        "Special Form\n" +
                        "  Creates and interns a global var with the name\n" +
                        "  of symbol in the current namespace (*ns*) or locates such a var if\n" +
                        "  it already exists.  If init is supplied, it is evaluated, and the\n" +
                        "  root binding of the var is set to the resulting value.  If init is\n" +
                        "  not supplied, the root binding of the var is unaffected.";
            case "do":
                return "  (do exprs*)\n" +
                        "Special Form\n" +
                        "  Evaluates the expressions in order and returns the value of\n" +
                        "  the last. If no expressions are supplied, returns nil.";
            case "if":
                return "  (if test then else?)\n" +
                        "Special Form\n" +
                        "  Evaluates test. If not the singular values nil or false,\n" +
                        "  evaluates and yields then, otherwise, evaluates and yields else. If\n" +
                        "  else is not supplied it defaults to nil.";
            case "monitor-enter":
                return "  (monitor-enter x)\n" +
                        "Special Form\n" +
                        "  Synchronization primitive that should be avoided\n" +
                        "  in user code. Use the 'locking' macro.";
            case "monitor-exit":
                return "  (monitor-exit x)\n" +
                        "Special Form\n" +
                        "  Synchronization primitive that should be avoided\n" +
                        "  in user code. Use the 'locking' macro.";
            case "new":
                return "  (Classname. args*)\n" +
                        "  (new Classname args*)\n" +
                        "Special Form\n" +
                        "  The args, if any, are evaluated from left to right, and\n" +
                        "  passed to the constructor of the class named by Classname. The\n" +
                        "  constructed object is returned.";
            case "quote":
                return "  (quote form)\n" +
                        "Special Form\n" +
                        "  Yields the unevaluated form.";
            case "recur":
                return "  (recur exprs*)\n" +
                        "Special Form\n" +
                        "  Evaluates the exprs in order, then, in parallel, rebinds\n" +
                        "  the bindings of the recursion point to the values of the exprs.\n" +
                        "  Execution then jumps back to the recursion point, a loop or fn method.";
            case "set!":
                return "  (set! var-symbol expr)\n" +
                        "  (set! (. instance-expr instanceFieldName-symbol) expr)\n" +
                        "  (set! (. Classname-symbol staticFieldName-symbol) expr)\n" +
                        "Special Form\n" +
                        "  Used to set thread-local-bound vars, Java object instance\n" +
                        "  fields, and Java class static fields.";
            case "throw":
                return "  (throw expr)\n" +
                        "Special Form\n" +
                        "  The expr is evaluated and thrown, therefore it should\n" +
                        "  yield an instance of some derivee of Throwable.";
            case "try":
            case "catch":
            case "finally":
                return "  (try expr* catch-clause* finally-clause?)\n" +
                        "  catch-clause => (catch classname name expr*)\n" +
                        "finally-clause => (finally expr*)\n" +
                        "Special Form\n" +
                        "  Catches and handles Java exceptions.";
            case "var":
                return "  (var symbol)\n" +
                        "Special Form\n" +
                        "  The symbol must resolve to a var, and the Var object itself\n" +
                        "  (not its value) is returned. The reader macro #'x expands to (var x).";
        }
        return null;
    }
}
