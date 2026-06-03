package cc.sighs.oelib.data.mvel.processor;

import cc.sighs.oelib.data.api.ExpressionFunction;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("cc.sighs.oelib.data.api.ExpressionFunction")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class ExpressionFunctionProcessor extends AbstractProcessor {

    private static final String BASE_GEN_PACKAGE = "cc.sighs.oelib.data.mvel.gen";
    private static final String REGISTRY_CLASS = BASE_GEN_PACKAGE + ".ExpressionFunctionRegistry";
    private static final String REGISTRAR_INTERFACE = BASE_GEN_PACKAGE + ".ExpressionFunctionsRegistrar";
    private static final String SUB_PACKAGE = ".oelib_mvel_gen";

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
            if (!(element instanceof ExecutableElement method)) continue;

            allMethods.add(method);

            if (!method.getModifiers().contains(Modifier.STATIC)) {
                messager.printMessage(Diagnostic.Kind.WARNING,
                        "@ExpressionFunction must be a static method: " + method.getSimpleName(), method);
                continue;
            }

            ExpressionFunction ann = method.getAnnotation(ExpressionFunction.class);
            String name = ann.value().isEmpty() ? method.getSimpleName().toString() : ann.value();

            if (functionsByName.containsKey(name)) {
                messager.printMessage(Diagnostic.Kind.WARNING,
                        "Duplicate @ExpressionFunction name found: '" + name + "'. Only the first one will be registered.", method);
                continue;
            }
            functionsByName.put(name, method);
        }

        if (functionsByName.isEmpty()) return false;

        try {
            generateRegistrar(functionsByName, allMethods);
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate registrar class: " + e.getMessage());
        }

        return false;
    }

    private void generateRegistrar(Map<String, ExecutableElement> functionsByName, List<ExecutableElement> allMethods) throws IOException {
        String hash = hashSuffix(allMethods);
        ExecutableElement sample = allMethods.getFirst();
        TypeElement owner = (TypeElement) sample.getEnclosingElement();

        String userPkg = processingEnv.getElementUtils().getPackageOf(owner).getQualifiedName().toString();
        String pkgName = userPkg + SUB_PACKAGE;
        String className = "GeneratedExpressionFunctions_" + hash;

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkgName).append(";\n\n");
        sb.append("import java.util.Set;\n");
        sb.append("import ").append(REGISTRY_CLASS).append(";\n");
        sb.append("import ").append(REGISTRAR_INTERFACE).append(";\n\n");

        sb.append("public final class ").append(className).append(" implements ExpressionFunctionsRegistrar {\n\n");

        sb.append("    @Override\n");
        sb.append("    public void register(ExpressionFunctionRegistry registry, Set<String> requiredFunctions) {\n");

        for (Map.Entry<String, ExecutableElement> entry : functionsByName.entrySet()) {
            String name = entry.getKey();
            ExecutableElement method = entry.getValue();
            TypeElement ownerClass = (TypeElement) method.getEnclosingElement();

            String params = method.getParameters().stream()
                    .map(p -> p.asType().toString() + ".class")
                    .collect(Collectors.joining(", "));

            sb.append("        if (requiredFunctions == null || requiredFunctions.contains(\"").append(name).append("\")) {\n");
            sb.append("            registry.register(\"").append(name).append("\", ")
                    .append(ownerClass.getQualifiedName()).append(".class, \"")
                    .append(method.getSimpleName()).append("\"");

            if (!params.isEmpty()) {
                sb.append(", ").append(params);
            }
            sb.append(");\n");
            sb.append("        }\n");
        }

        sb.append("    }\n");
        sb.append("}\n");

        var sourceFile = filer.createSourceFile(pkgName + "." + className);
        try (Writer writer = sourceFile.openWriter()) {
            writer.write(sb.toString());
        }

        generateServiceProvider(pkgName + "." + className);
    }

    private void generateServiceProvider(String fullClassName) throws IOException {
        FileObject resource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/" + REGISTRAR_INTERFACE);
        try (PrintWriter svc = new PrintWriter(resource.openWriter())) {
            svc.println(fullClassName);
        }
    }

    private String hashSuffix(List<ExecutableElement> methods) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (ExecutableElement m : methods) {
                TypeElement owner = (TypeElement) m.getEnclosingElement();
                String pkg = processingEnv.getElementUtils().getPackageOf(owner).getQualifiedName().toString();
                md.update(pkg.getBytes(StandardCharsets.UTF_8));
                md.update(owner.getQualifiedName().toString().getBytes(StandardCharsets.UTF_8));
                md.update(m.getSimpleName().toString().getBytes(StandardCharsets.UTF_8));
                m.getParameters().forEach(p -> md.update(p.asType().toString().getBytes(StandardCharsets.UTF_8)));
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