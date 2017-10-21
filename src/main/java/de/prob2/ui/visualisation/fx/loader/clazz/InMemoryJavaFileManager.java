package de.prob2.ui.visualisation.fx.loader.clazz;

import javax.tools.*;

/**
 * Created by Christoph Heinzen on 27.04.17.
 */
public class InMemoryJavaFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

    private final InMemoryClassloader classLoader;

    InMemoryJavaFileManager(JavaCompiler compiler, InMemoryClassloader classLoader, DiagnosticCollector<JavaFileObject> diagnostics) {
        super(compiler.getStandardFileManager(diagnostics, null, null));
        this.classLoader = classLoader;
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling ) {
        InMemoryJavaFileObject fileObject = new InMemoryJavaFileObject(className);
        classLoader.addClassFile(fileObject);
        return fileObject;
    }
}
