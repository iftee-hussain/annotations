package com.example.annotation.processor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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
import javax.tools.JavaFileObject;

import com.google.auto.service.AutoService;

@SupportedAnnotationTypes("com.example.annotation.processor.Singletone")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class SingletoneProcessor extends AbstractProcessor {

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		//set of class elements with singletone annotation
		Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(Singletone.class);
		//set of field elements with wired annotation
		Set<? extends Element> annotatedWiredElements = roundEnv.getElementsAnnotatedWith(Wired.class);
		//set of field elements with prototype annotation
		Set<? extends Element> annotatedProtoypeElements = roundEnv.getElementsAnnotatedWith(Prototype.class);

		if (annotatedElements.size() > 0) {
			String className = ""; // full class name with package
			String simpleClassName = ""; //classname
			String wiredAndPrototype = ""; // modified sorce code when wired and prototype annotations are applied
			//looping through classes with singletone annotation
			for (Element elementObj : annotatedElements) {
				String packageName = elementObj.getEnclosingElement().toString();
				simpleClassName = elementObj.getSimpleName().toString();
				className = packageName + "." + elementObj.getSimpleName();
				System.out.println("Annotation Processor Bulding : " + className);
				ArrayList<Element> wiredElementList = new ArrayList<>();
				ArrayList<Element> prototypeElementList = new ArrayList<>();
				if (annotatedWiredElements.size() > 0) {
					for (Element element : annotatedWiredElements) {
						//check if this is wired
						Wired wiredAnnotation = element.getAnnotation(Wired.class);
						if (wiredAnnotation != null) {
							// check annotated object belongs to the same class
							if (element.getEnclosingElement().getSimpleName().toString().equals(simpleClassName)) {
								wiredElementList.add(element);
							}

						}
					}

				}

				if (annotatedProtoypeElements.size() > 0) {
					for (Element element : annotatedProtoypeElements) {
						//check if this is prototype
						Prototype prototypeAnnotation = element.getAnnotation(Prototype.class);
						if (prototypeAnnotation != null) {
							// check annotated object belongs to the same class
							if (element.getEnclosingElement().getSimpleName().toString().equals(simpleClassName)) {
								prototypeElementList.add(element);
							}

						}
					}

				}

				System.out.println("*****************************************************************");
				// modify source code for wired and prototype
				wiredAndPrototype += wiredAndPrototypeProcessor(simpleClassName, wiredElementList,
						prototypeElementList);

				try {
					// modify source code for singletone and write to file
					writeBuilderFile(className, wiredAndPrototype);
					wiredElementList.clear();
					prototypeElementList.clear();
					wiredAndPrototype = "";
				} catch (IOException e) {
					e.printStackTrace();
					return true;
				}

			}

		}

		return false;
	}

	/**
	 * This method creates source code if wired and prototype annotations are applied
	 * @param className
	 * @param wiredElementList
	 * @param prototypeElementList
	 * @return
	 */
	private synchronized String wiredAndPrototypeProcessor(String className, ArrayList<Element> wiredElementList,
														   ArrayList<Element> prototypeElementList) {
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
			singleConstructorBody += simpleClassName + ".getInstance(),";

		}

		for (Element element : prototypeElementList) {
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
			singleConstructorBody += "new " + simpleClassName + "(),";

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
	/**
	 * This method creates source code for singletone annotaion and write to file
	 * @param className
	 * @param wired
	 * @throws IOException
	 */
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
