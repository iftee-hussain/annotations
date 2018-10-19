package com.example.annotation.processor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.google.auto.service.AutoService;

@SupportedAnnotationTypes("com.example.annotation.processor.Singletone")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class SingletoneProcessor extends AbstractProcessor{

	Map<String, String> mapOfClasses = null;

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

		//set of class elements with singletone annotation
		Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(Singletone.class);
		Set<? extends Element> annotatedPElements = roundEnv.getElementsAnnotatedWith(Prototype.class);
		mapOfClasses = createClassTypeMap(annotatedElements, annotatedPElements);
		//set of field elements with wired annotation
		Set<? extends Element> annotatedWiredElements = roundEnv.getElementsAnnotatedWith(Wired.class);
		if (annotatedElements.size() > 0) {
			String className = ""; // full class name with package
			String simpleClassName = ""; //classname
			String wired = ""; // modified sorce code when wired and prototype annotations are applied
			//looping through classes with singletone annotation
			for (Element elementObj : annotatedElements) {
				String packageName = elementObj.getEnclosingElement().toString();
				simpleClassName = elementObj.getSimpleName().toString();
				className = packageName + "." + elementObj.getSimpleName();
				System.out.println("Annotation Processor Bulding : " + className);
				ArrayList<Element> wiredElementList = new ArrayList<>();
				boolean write = true;
				if (annotatedWiredElements.size() > 0) {
					for (Element element : annotatedWiredElements) {
						//check if this is wired
						Wired wiredAnnotation = element.getAnnotation(Wired.class);
						if (wiredAnnotation != null) {
							// check annotated object belongs to the same class
							if (element.getEnclosingElement().getSimpleName().toString().equals(simpleClassName)) {
								VariableElement v = (VariableElement) element;
								String elementName = v.asType().toString();
								if(mapOfClasses.get(elementName) == null ){
									processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@Wired must be applied to a singletone or prototype class");
									write = false ;
									break;
								}
								else
									wiredElementList.add(element);
							}

						}
					}

				}


				System.out.println("*****************************************************************");
				System.out.println("*****************************************************************");
				// modify source code for wired and prototype
				if(wiredElementList.size()>0 && write)
					wired = wiredProcessor(simpleClassName, wiredElementList);

				try {
					// modify source code for singletone and write to file
					if(write)
						writeBuilderFile(className, wired);

					wiredElementList.clear();
					wired = "";
				} catch (IOException e) {
					e.printStackTrace();
					return true;
				}

			}

		}

		return false;
	}

	private String wiredProcessor(String className, ArrayList<Element> wiredElementList) {
		StringBuilder sb = new StringBuilder();
		String constructorParameters = "";
		String constructorBody = "";
		String singleConstructorBody = "this(";
		for (Element element : wiredElementList) {
			VariableElement v = (VariableElement) element;
			String elementName = v.asType().toString();
			int lastDot = elementName.lastIndexOf('.');
			String simpleClassName = elementName.substring(lastDot + 1);
			String variableName = v.getSimpleName().toString();
			String parameterName = simpleClassName + " " + variableName;
			sb.append("\tprivate  " + parameterName + ";");
			sb.append("\n");
			constructorBody += "this." + variableName + " = " + variableName + ";\n\t\t";
			constructorParameters += parameterName;
			constructorParameters += ", ";
			if(mapOfClasses.get(elementName).equals("singletone"))
				singleConstructorBody += simpleClassName + ".getInstance(),";
			else if(mapOfClasses.get(elementName).equals("prototype"))
				singleConstructorBody += "new "+simpleClassName + "(),";


		}


		constructorParameters = constructorParameters.substring(0, constructorParameters.length() - 2);
		singleConstructorBody = singleConstructorBody.substring(0, singleConstructorBody.length() - 1);
		sb.append("\n\tprivate " + className + "(){");
		if (wiredElementList.size() > 0) {
			sb.append("\n\t\t" + singleConstructorBody);
			sb.append(");");
		}
		sb.append("\n\t}\n");

		if (wiredElementList.size() > 0) {
			sb.append("\n\tprotected " + className + "(" + constructorParameters + "){\n");
			sb.append("\t\t" + constructorBody + "\n\t}\n");
		}

		return sb.toString();
	}

	private Map<String, String> createClassTypeMap(Set<? extends Element> annotatedElements,
												   Set<? extends Element> annotatedPElements) {
		Map<String, String> map = new HashMap<>();
		for (Element element : annotatedElements) {
			map.put(element.asType().toString(), "singletone");
		}

		for (Element element : annotatedPElements) {
			map.put(element.asType().toString(), "prototype");
		}
		return map;
	}

	private void writeBuilderFile(String className, String wired) throws IOException {

		String packageName = null;
		int lastDot = className.lastIndexOf('.');
		if (lastDot > 0) {
			packageName = className.substring(0, lastDot);
		}

		String simpleClassName = className.substring(lastDot + 1);
		String builderClassName = className;
		String builderSimpleClassName = builderClassName.substring(lastDot + 1);

		JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(builderClassName);
		try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

			if (packageName != null) {
				out.print("package ");
				out.print(packageName);
				out.println(";");
				out.println();
			}

			out.print("public class ");
			out.print(builderSimpleClassName);
			out.println(" {");
			out.println();

			out.print("    private static final ");
			out.print(simpleClassName);
			out.print(" instance = new ");
			out.print(simpleClassName);
			out.println("();");
			out.println();

			if (wired.length() > 0)
				out.print(wired);

			out.print("    public static ");
			out.print(simpleClassName);
			out.println(" getInstance() {");
			out.println("		return instance;");
			out.println("    }");
			out.println();

			out.print("    public void doSomething(){ ");
			out.println();
			out.println("    }");
			out.println();

			out.println("}");

		}
	}



}