package model;

import java.util.List;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.MethodSpec;

public class CreateMethod {

	public MethodSpec createMethod(String modifier, boolean isStatic, Object returnType, String methodName,
			String statement) {
		if (!isStatic) {
			return MethodSpec.methodBuilder("main").addModifiers(Modifier.valueOf(modifier)).returns(void.class)
					.addParameter(String[].class, "args").addStatement(statement).build();
		} else {
			return MethodSpec.methodBuilder("main").addModifiers(Modifier.valueOf(modifier), Modifier.STATIC)
					.returns(void.class).addParameter(String[].class, "args").addStatement(statement).build();
		}
	}

	public MethodSpec createMethod(String modifier, boolean isStatic, Object returnType, String methodName, List<Object> parameters,
			String statement) {
		if (!isStatic) {
			return MethodSpec.methodBuilder("main").addModifiers(Modifier.valueOf(modifier)).returns(void.class)
					.addParameter(String[].class, "args").addStatement(statement).build();
		} else {
			return MethodSpec.methodBuilder("main").addModifiers(Modifier.valueOf(modifier), Modifier.STATIC)
					.returns(void.class).addParameter(String[].class, "args").addStatement(statement).build();
		}
	}

}
