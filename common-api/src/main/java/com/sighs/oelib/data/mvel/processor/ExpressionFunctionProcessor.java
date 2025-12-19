package com.sighs.oelib.data.mvel.processor;

import com.sighs.oelib.data.api.ExpressionFunction;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@SupportedAnnotationTypes("com.mafuyu404.oelib.api.data.ExpressionFunction")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class ExpressionFunctionProcessor extends AbstractProcessor {

    private Messager messager;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        this.messager = env.getMessager();
        this.filer = env.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<String, ExecutableElement> functionsByName = new HashMap<>();
        List<ExecutableElement> allMethods = new ArrayList<>();

        for (Element element : roundEnv.getElementsAnnotatedWith(ExpressionFunction.class)) {
            if (!(element instanceof ExecutableElement method)) {
                continue;
            }
            allMethods.add(method);

            if (!method.getModifiers().contains(Modifier.STATIC)) {
                messager.printMessage(
                        Diagnostic.Kind.WARNING,
                        "@ExpressionFunction should annotate a static method (MVEL requires static): " + method.getSimpleName(),
                        method
                );
                continue;
            }

            ExpressionFunction ann = method.getAnnotation(ExpressionFunction.class);
            String name = ann.value().isEmpty() ? method.getSimpleName().toString() : ann.value();

            if (functionsByName.containsKey(name)) {
                messager.printMessage(
                        Diagnostic.Kind.WARNING,
                        "Duplicate ExpressionFunction name: '" + name + "'. Only the first one will be registered.",
                        method
                );
                continue;
            }
            functionsByName.put(name, method);
        }

        if (functionsByName.isEmpty()) {
            return false;
        }

        try {
            generateRegistrar(functionsByName, allMethods);
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate ExpressionFunctionsRegistrar: " + e.getMessage());
        }

        return false;
    }

    private void generateRegistrar(Map<String, ExecutableElement> functionsByName, List<ExecutableElement> allMethods) throws IOException {
        String hash = hashSuffix(allMethods);
        ExecutableElement sample = allMethods.getFirst();
        TypeElement owner = (TypeElement) sample.getEnclosingElement();
        PackageElement ownerPkg = processingEnv.getElementUtils().getPackageOf(owner);
        String pkg = ownerPkg.getQualifiedName().toString() + ".oelib_mvel_gen";
        String cls = "GeneratedExpressionFunctions_" + hash;

        JavaFileObject file = filer.createSourceFile(pkg + "." + cls);
        try (PrintWriter out = new PrintWriter(file.openWriter())) {
            out.println("package " + pkg + ";");
            out.println();
            out.println("import com.mafuyu404.oelib.data.mvel.gen.ExpressionFunctionRegistry;");
            out.println("import com.mafuyu404.oelib.data.mvel.gen.ExpressionFunctionsRegistrar;");
            for (ExecutableElement method : functionsByName.values()) {
                TypeElement methodOwner = (TypeElement) method.getEnclosingElement();
                out.println("import " + methodOwner.getQualifiedName().toString() + ";");
            }
            out.println("import java.util.Set;");
            out.println();
            out.println("public final class " + cls + " implements ExpressionFunctionsRegistrar {");
            out.println("    @Override");
            out.println("    public void register(ExpressionFunctionRegistry registry, Set<String> requiredFunctions) {");
            for (Map.Entry<String, ExecutableElement> e : functionsByName.entrySet()) {
                String name = e.getKey();
                ExecutableElement method = e.getValue();
                TypeElement methodOwner = (TypeElement) method.getEnclosingElement();

                String ownerSimple = methodOwner.getQualifiedName().toString().substring(
                        methodOwner.getQualifiedName().toString().lastIndexOf('.') + 1);

                List<? extends TypeMirror> params = method.getParameters()
                        .stream().map(VariableElement::asType).toList();

                StringBuilder paramTypesBuilder = new StringBuilder();
                if (!params.isEmpty()) {
                    for (int i = 0; i < params.size(); i++) {
                        TypeMirror tm = params.get(i);
                        String typeName = tm.toString();
                        int lastDot = typeName.lastIndexOf('.');
                        String simpleName = lastDot >= 0 ? typeName.substring(lastDot + 1) : typeName;
                        paramTypesBuilder.append(simpleName).append(".class");
                        if (i < params.size() - 1) paramTypesBuilder.append(", ");
                    }
                }

                out.println("        if (requiredFunctions == null || requiredFunctions.contains(\"" + name + "\")) {");
                out.println("            registry.register(\"" + name + "\", " + ownerSimple + ".class, \"" + method.getSimpleName() + "\"" +
                        (params.isEmpty() ? "" : ", " + paramTypesBuilder) + ");");
                out.println("        }");
            }
            out.println("    }");
            out.println("}");
        }

        String registrarService = "com.mafuyu404.oelib.data.mvel.gen.ExpressionFunctionsRegistrar";
        try (PrintWriter svc = new PrintWriter(
                filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/" + registrarService)
                        .openWriter())) {
            svc.println(pkg + "." + cls);
        }
    }

    private String hashSuffix(List<ExecutableElement> methods) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (ExecutableElement m : methods) {
                TypeElement owner = (TypeElement) m.getEnclosingElement();
                PackageElement pkg = processingEnv.getElementUtils().getPackageOf(owner);
                md.update(pkg.getQualifiedName().toString().getBytes(StandardCharsets.UTF_8));
                md.update(owner.getQualifiedName().toString().getBytes(StandardCharsets.UTF_8));
                md.update(m.getSimpleName().toString().getBytes(StandardCharsets.UTF_8));
                for (var p : m.getParameters()) {
                    md.update(p.asType().toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(6, digest.length); i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(methods.hashCode());
        }
    }
}