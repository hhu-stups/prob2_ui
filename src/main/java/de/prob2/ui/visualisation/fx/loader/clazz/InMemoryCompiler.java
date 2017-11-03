package de.prob2.ui.visualisation.fx.loader.clazz;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by Christoph Heinzen on 27.04.17.
 */
public class InMemoryCompiler {

    public Class<?> compile(String className, File javaClassFile, /*String classpath,*/ InMemoryClassloader classloader)
            throws InMemoryCompilerException, IOException, ClassNotFoundException{
        String classpath = System.getProperty("java.class.path");

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (JavaFileManager inMemoryFileManager = new InMemoryJavaFileManager(compiler, classloader, diagnostics);
             StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {

            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjects(javaClassFile);

            //create compiler-task
            JavaCompiler.CompilationTask task = compiler.getTask(null, inMemoryFileManager,
                            diagnostics, Arrays.asList("-classpath", classpath), null, units);

            //compile and throw exception when an error occurs
            if (!task.call()) {
                throw new InMemoryCompilerException(className, diagnostics);
            }

        }
        return Class.forName(className, true, classloader);
    }
}
