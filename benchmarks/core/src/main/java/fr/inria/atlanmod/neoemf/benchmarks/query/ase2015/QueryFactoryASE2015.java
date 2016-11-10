/*
 * Copyright (c) 2013-2016 Atlanmod INRIA LINA Mines Nantes.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Atlanmod INRIA LINA Mines Nantes - initial API and implementation
 */

package fr.inria.atlanmod.neoemf.benchmarks.query.ase2015;

import fr.inria.atlanmod.neoemf.benchmarks.query.Query;
import fr.inria.atlanmod.neoemf.benchmarks.query.QueryFactory;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.gmt.modisco.java.ASTNode;
import org.eclipse.gmt.modisco.java.AbstractTypeDeclaration;
import org.eclipse.gmt.modisco.java.Block;
import org.eclipse.gmt.modisco.java.BodyDeclaration;
import org.eclipse.gmt.modisco.java.ClassDeclaration;
import org.eclipse.gmt.modisco.java.Comment;
import org.eclipse.gmt.modisco.java.CompilationUnit;
import org.eclipse.gmt.modisco.java.Javadoc;
import org.eclipse.gmt.modisco.java.MethodDeclaration;
import org.eclipse.gmt.modisco.java.Model;
import org.eclipse.gmt.modisco.java.Statement;
import org.eclipse.gmt.modisco.java.TagElement;
import org.eclipse.gmt.modisco.java.TextElement;
import org.eclipse.gmt.modisco.java.TypeAccess;
import org.eclipse.gmt.modisco.java.TypeDeclaration;
import org.eclipse.gmt.modisco.java.VisibilityKind;
import org.eclipse.gmt.modisco.java.emf.meta.JavaPackage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.isNull;

public class QueryFactoryASE2015 extends QueryFactory {

    public static Query<Integer> queryGrabatsASE2015(Resource resource) {
        return () -> {
            List<ClassDeclaration> listResult = new BasicEList<>();
            for (EObject eObj : getAllInstances(resource, JavaPackage.eINSTANCE.getTypeDeclaration())) {
                TypeDeclaration typeDecl = (TypeDeclaration) eObj;
                for (BodyDeclaration method : typeDecl.getBodyDeclarations()) {
                    if ((method instanceof MethodDeclaration)) {
                        MethodDeclaration methDecl = (MethodDeclaration) method;
                        if (!isNull(methDecl.getModifier()) && methDecl.getModifier().isStatic() && !isNull(methDecl.getReturnType()) && methDecl.getReturnType().getType() == typeDecl) {
                            listResult.add((ClassDeclaration) typeDecl);
                        }
                    }
                }
            }
            return listResult.size();
        };
    }

    public static Query<Integer> queryThrownExceptionsAse2015(Resource resource) {
        return () -> {
            List<? extends EObject> classes = getAllInstances(resource, JavaPackage.eINSTANCE.getClassDeclaration());
            List<TypeAccess> thrownExceptions = new BasicEList<>();
            for (EObject object : classes) {
                if (object instanceof ClassDeclaration) {
                    for (BodyDeclaration declaration : ((ClassDeclaration) object).getBodyDeclarations()) {
                        if (declaration instanceof MethodDeclaration) {
                            for (TypeAccess type : ((MethodDeclaration) declaration).getThrownExceptions()) {
                                if (!thrownExceptions.contains(type)) {
                                    thrownExceptions.add(type);
                                }
                            }
                        }
                    }
                }
            }
            return thrownExceptions.size();
        };
    }

    public static Query<Integer> querySpecificInvisibleMethodDeclarationsAse2015(Resource resource) {
        return () -> {
            List<MethodDeclaration> methodDeclarations = new BasicEList<>();
            Model model = (Model) resource.getContents().get(0);
            if (model.getName().equals("org.eclipse.gmt.modisco.java.neoemf") || model.getName().equals("org.eclipse.jdt.core")) {
                try {
                    for (org.eclipse.gmt.modisco.java.Package pack : model.getOwnedElements()) {
                        getInvisibleMethods(pack, methodDeclarations);
                    }
                }
                catch (NullPointerException e) {
                    Query.LOG.error(e);
                }
                return methodDeclarations.size();
            }
            else {
                return methodDeclarations.size();
            }
        };
    }

    public static Query<Integer> queryCommentsTagContentASE2015(Resource resource) {
        return () -> {
            Set<TextElement> result = new HashSet<>();
            try {
                Model model = (Model) resource.getContents().get(0);
                if (model.getName().equals("org.eclipse.gmt.modisco.java.neoemf") || model.getName().equals("org.eclipse.jdt.core")) {
                    for (CompilationUnit cu : model.getCompilationUnits()) {
                        for (Comment com : cu.getCommentList()) {
                            if (com instanceof Javadoc) {
                                Javadoc jd = (Javadoc) com;
                                for (TagElement te : jd.getTags()) {
                                    for (ASTNode node : te.getFragments()) {
                                        if (node instanceof TextElement) {
                                            result.add((TextElement) node);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return result.size();
                }
                else {
                    return result.size();
                }
            }
            catch (NullPointerException e) {
                Query.LOG.error(e);
                return result.size();
            }
        };
    }

    @SuppressWarnings("unused")
    public static Query<Integer> queryAse2015BranchStatements(Resource resource) {
        return () -> {
            List<Statement> result = new BasicEList<>();
            Model model = (Model) resource.getContents().get(0);
            if (model.getName().equals("org.eclipse.gmt.modisco.java.neoemf") || model.getName().equals("org.eclipse.jdt.core")) {
                try {
                    for (org.eclipse.gmt.modisco.java.Package pack : model.getOwnedElements()) {
                        getAccessedTypes(pack, result);
                    }
                }
                catch (NullPointerException e) {
                    Query.LOG.error(e);
                }
                return result.size();
            }
            else {
                return result.size();
            }
        };
    }

    private static void getAccessedTypes(org.eclipse.gmt.modisco.java.Package pack, List<Statement> result) {
        for (AbstractTypeDeclaration el : pack.getOwnedElements()) {
            if (JavaPackage.eINSTANCE.getClassDeclaration().isInstance(el)) {
                getAccessedTypes((ClassDeclaration) el, result);
            }
        }
        for (org.eclipse.gmt.modisco.java.Package subPack : pack.getOwnedPackages()) {
            getAccessedTypes(subPack, result);
        }
    }

    private static void getAccessedTypes(ClassDeclaration cd, List<Statement> result) {
        for (BodyDeclaration method : cd.getBodyDeclarations()) {
            if ((method instanceof MethodDeclaration)) {
                MethodDeclaration meth = (MethodDeclaration) method;
                if (!isNull(meth.getBody())) {
                    getAccessedTypesFromBody(meth.getBody().getStatements(), result);
                }
            }
        }
    }

    private static void getAccessedTypesFromBody(List<Statement> statements, List<Statement> result) {
        for (Statement s : statements) {
            getAccessedTypesFromStatement(s, result);
        }
    }

    private static void getAccessedTypesFromStatement(Statement statement, List<Statement> result) {
        if (statement instanceof Block) {
            getAccessedTypesFromBody(((Block) statement).getStatements(), result);
        }
        result.add(statement);
    }

    private static void getInvisibleMethods(org.eclipse.gmt.modisco.java.Package pack, List<MethodDeclaration> result) {
        for (AbstractTypeDeclaration el : pack.getOwnedElements()) {
            if (JavaPackage.eINSTANCE.getClassDeclaration().isInstance(el)) {
                getInvisibleMethodsInClassDeclaration((ClassDeclaration) el, result);
            }
        }
        for (org.eclipse.gmt.modisco.java.Package subPack : pack.getOwnedPackages()) {
            getInvisibleMethods(subPack, result);
        }
    }

    private static void getInvisibleMethodsInClassDeclaration(ClassDeclaration cd, List<MethodDeclaration> result) {
        for (BodyDeclaration method : cd.getBodyDeclarations()) {
            if ((method instanceof MethodDeclaration) && !isNull(method.getModifier())) {
                if (method.getModifier().getVisibility() == VisibilityKind.PRIVATE) {
                    result.add((MethodDeclaration) method);
                }
                else if (method.getModifier().getVisibility() == VisibilityKind.PROTECTED) {
                    result.add((MethodDeclaration) method);
                }
            }
        }
    }
}