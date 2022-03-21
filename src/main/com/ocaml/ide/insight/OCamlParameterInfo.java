package com.ocaml.ide.insight;

import com.intellij.lang.parameterInfo.*;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.SyntaxTraverser;
import com.ocaml.OCamlLanguage;
import com.ocaml.lang.utils.OCamlPsiUtils;
import com.ocaml.sdk.annot.OCamlInferredSignature;
import com.or.lang.OCamlTypes;
import com.or.lang.core.psi.PsiLowerSymbol;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class OCamlParameterInfo implements ParameterInfoHandler<PsiElement, OCamlParameterInfo.ParameterInfoArgumentList> {

    @Override
    public @Nullable PsiElement findElementForUpdatingParameterInfo(@NotNull UpdateParameterInfoContext context) {
        Pair<PsiElement, ParameterInfoArgumentList> pair = findArgumentList(context, context.getParameterListStart());
        System.out.println("update???"+pair);
        return pair == null ? null : pair.first;
    }

    @Override public @Nullable PsiElement findElementForParameterInfo(@NotNull CreateParameterInfoContext context) {
        Pair<PsiElement, ParameterInfoArgumentList> element = findArgumentList(context, context.getOffset() - 1);
        if (element == null) return null;
        context.setItemsToShow(new ParameterInfoArgumentList[]{ element.second });
        return element.first;
    }

    /*
     * This method is HELL. It had a hard time writing the code, and to help anyone trying
     * to improve this mess, I'm leaving some of my thoughts right now.
     *
     * findElementForParameterInfo is called before findElementForUpdatingParameterInfo.
     * Both are calling findArgumentList, and the second argument MUST BE THE SAME (at least
     * in the first update). Otherwise, the popup might not be shown.
     *
     * If the method is called on a comment / spaces, we are skipping them.
     *
     * If we are on ~name: ..., we are moving to "...".
     * Then we are using the Syntax Traverser to find a starting point
     * (ex: if we are inside parenthesis, we MAY have to go up, or we may not).
     *
     * Then, we are moving back starting from our starting point, in order to find
     * the method call.
     *
     * If needed, we are reading values that are AFTER our starting point, in order to inspect
     * every argument of the function.
     *
     * Once we got the function, we can use the annot service to have its type.
     * We are relying on this type to show arguments, or to add [] if they aren't
     * in the right order.
     */
    private @Nullable Pair<PsiElement, ParameterInfoArgumentList> findArgumentList(@NotNull ParameterInfoContext context, int parameterListStart) {
//        System.out.println("find at("+context.getOffset()+"/"+parameterListStart+") index ("+parameterListStart+")");
//        int offset = context.getOffset() - 1;
        PsiElement originalElement = context.getFile().findElementAt(parameterListStart);
        PsiElement startingElement = originalElement;
        if (startingElement == null) return null;
//        System.out.println("  found '"+startingElement.getText()+"' ("+startingElement+")");

        OCamlAnnotResultsService annot = startingElement.getProject().getService(OCamlAnnotResultsService.class);

        // If the method is called on a comment / spaces, we are skipping them.
        while (startingElement instanceof PsiWhiteSpace || startingElement instanceof PsiComment)
            startingElement = OCamlPsiUtils.skipMeaninglessPreviousSibling(startingElement);
        if (startingElement == null) return null;

        // find our starting point
        OCamlInferredSignature annotation = null;

        // ~name:
        PsiElement startElementCandidate = startingElement;
        // : => go to next
        if (startElementCandidate.getText().equals(OCamlTypes.TILDE.getSymbol())) {
            startElementCandidate = OCamlPsiUtils.skipMeaninglessNextSibling(startElementCandidate);
        }
        if (startElementCandidate != null && startElementCandidate.getNode().getElementType() == OCamlTypes.LIDENT) {
            startElementCandidate = startElementCandidate.getParent();
        }
        if (startElementCandidate instanceof PsiLowerSymbol) {
            startElementCandidate = OCamlPsiUtils.skipMeaninglessNextSibling(startElementCandidate);
        }
        if (startElementCandidate != null && startElementCandidate.getText().equals(OCamlTypes.COLON.getSymbol())) {
            startElementCandidate = OCamlPsiUtils.skipMeaninglessNextSibling(startElementCandidate);
        }
        // "replace"
        if (startElementCandidate != null) startingElement = startElementCandidate;

        List<PsiElement> psiElements = SyntaxTraverser.psiApi().parents(startingElement).toList();
        for (PsiElement candidate : psiElements) {
            annotation = annot.findAnnotationFor(candidate, true);
            startingElement = candidate;
            if (annotation != null) break;
        }
        if (annotation == null) return null;

//        System.out.println("  candidate is: '"+startingElement.getText()+"' ("+startingElement+")");
//        System.out.println("  annotation found: "+annotation);

        ArrayList<Pair<OCamlInferredSignature, PsiElement>> elements = new ArrayList<>();
        elements.add(new Pair<>(annotation, startingElement));

        int index;
        int firstFunctionIndex;
        PsiElement element;

        do {
            element = OCamlPsiUtils.skipMeaninglessPreviousSibling(startingElement);
            index = 0;
            firstFunctionIndex = updateFirstFunctionIndex(annotation, -1);

            // some elements are wrapped
            if (element == null) {
                element = OCamlPsiUtils.skipMeaninglessPreviousSibling(startingElement.getParent());
            }

            while (element != null) {
                // https://ocaml.org/manual/lablexamples.html
                // allow ~name:
                if (element.getText().equals(OCamlTypes.COLON.getSymbol())) {
//                    System.out.println("    > is ~name?");
                    element = OCamlPsiUtils.skipMeaninglessPreviousSibling(element);
                    if (element == null) break;
                    String name = element.getText();
                    element = OCamlPsiUtils.skipMeaninglessPreviousSibling(element);
                    if (element == null || !element.getText().equals(OCamlTypes.TILDE.getSymbol())) break;
                    element = OCamlPsiUtils.skipMeaninglessPreviousSibling(element);
                    if (element == null) break;
//                    System.out.println("    > ~name: <"+name+">");
                    OCamlInferredSignature first = elements.get(0).first;
//                    System.out.println("    > "+ first.type);
                    if (!first.type.contains(":")) first.type = name + ":" + first.type;
//                    System.out.println("    > "+ first.type);
                }

//                System.out.println("    look for element:"+element);
                annotation = annot.findAnnotationFor(element, true);
//                System.out.println("    has this:"+annotation);
                if (annotation == null) break;

                elements.add(0, new Pair<>(annotation, element));
                element = OCamlPsiUtils.skipMeaninglessPreviousSibling(element);
                index++;

                // we found another function
                firstFunctionIndex = updateFirstFunctionIndex(annotation, firstFunctionIndex);
            }

            // done
            if (firstFunctionIndex != -1) break;
            // try a new starting element
            startingElement = startingElement.getParent();
            if (startingElement == null) return null; // just in case
            annotation = annot.findAnnotationFor(startingElement, true);
            if (annotation == null) return null; // can't do anything

//            System.out.println("  *new* candidate is: '"+startingElement.getText()+"' ("+startingElement+")");
//            System.out.println("  *new* annotation found: "+annotation);
        } while (true);

//        System.out.println("  "+elements);
//        System.out.println("  fun is at index:"+firstFunctionIndex);
//        System.out.println("  we are at index:"+index);

        // start with the function
        while (firstFunctionIndex != 0) {
            elements.remove(0);
            firstFunctionIndex--;
            index--;
        }

//        System.out.println("  *new* "+elements);
//        System.out.println("  *new* fun is at index:"+firstFunctionIndex);
//        System.out.println("  *new* we are at index:"+index);

        Pair<OCamlInferredSignature, PsiElement> fun = elements.remove(0);
        List<String> parameters = elements.stream().map(pair -> pair.first.type).collect(Collectors.toList());
        List<String> names = new ArrayList<>();
        String function = fun.first.type;

        final String separator = OCamlLanguage.FUNCTION_SIGNATURE_SEPARATOR;

        // guess the types that we got after
        while (function.contains(separator)) {
            int i = function.indexOf(separator);

            // oh, no, this is a function
            String substring = function.substring(0, i);
            if (substring.startsWith("(")) {
                i = function.indexOf(')') + 1; // the separator is after ')'
                substring = function.substring(1, i-1); // we don't want '(' nor ')'
            }

            // handle
            names.add(substring);

            // next
            function = function.substring(i + separator.length());
        }

//        System.out.println("  names:"+names);
//        System.out.println("  params:"+parameters);

        // fill next
        int count = elements.size();
        element = count == 0 ? null : elements.get(count-1).second;
        if (element != null) {
            while (parameters.size() < names.size()) {
                element = OCamlPsiUtils.skipMeaninglessNextSibling(element);
                if (element == null) break;
                annotation = annot.findAnnotationFor(element, true);
                if (annotation == null) break;
                // add in the list
                parameters.add(annotation.type);
            }
        }

        System.out.println("  names:"+names);
        System.out.println("  params:"+parameters);

        // #parameters <= #names
        ArrayList<String> sorted = new ArrayList<>();
        HashMap<String, Pair<Integer, String>> defaultValues = new HashMap<>();
        int i;
        for (i = 0; i < parameters.size(); i++) {
            String v1 = parameters.get(i);
            String v2 = names.get(i);

            if (v2.startsWith("?")) {
                int commaV1 = v1.indexOf(":");
                int commaV2 = v2.indexOf(":");
                String nameV1 = commaV1 != -1 ? "?"+v1.substring(0, commaV1) : "";
                String nameV2 = commaV2 != -1 ? v2.substring(0, commaV2) : "";

//                System.out.println("  !!!!"+(nameV1.isEmpty() ? "<empty>" : nameV1)+"=>"+nameV2+".");

                // ?same: type should match same: 'type
                if (nameV1.equals(nameV2)) {
                    sorted.add(v2);
//                    System.out.println("  found and add "+v2);
                    continue;
                }

                // here, we may either skip this optional argument
                // or, this argument is present, but at a different position
                if (nameV1.isEmpty()) {
                    defaultValues.put(nameV2, new Pair<>(i, v2));
                    sorted.add("["+v1+"]");
//                    System.out.println("  add "+v1+" and save "+v2);
                    continue;
                }

                if (defaultValues.containsKey(nameV1)) {
                    Pair<Integer, String> removed = defaultValues.remove(nameV1);
                    sorted.add("["+removed.second+"]"); // added now
                    defaultValues.put(nameV2, new Pair<>(i, v2));
//                    System.out.println("  I found and added:"+removed.second);
                    continue;
                }

//                System.out.println("  nothing, add:"+v1);
                sorted.add(v1);
                continue;
            }

            if (v1.equals(v2)) sorted.add(v2);
            else { // got a problem
                int commaV1 = v1.indexOf(":");
                String nameV1 = commaV1 != -1 ? "?"+v1.substring(0, commaV1) : "";
                Pair<Integer, String> pair = defaultValues.remove(nameV1);
                if (pair != null) {
                    sorted.add("["+pair.second+"]");
                } else {
                    sorted.add("["+v1+"]");
                }
//                System.out.println("  compare:"+v1+" with "+v2);
//                System.out.println("  but:"+defaultValues);
            }
        }

        // add default values that were not given
//        System.out.println("  remain:"+defaultValues);
        for (Pair<Integer, String> pair : defaultValues.values()) {
            sorted.add("["+pair.second+"]");
            i++;
        }

        // missing
        for (; i < names.size() ; i++) {
            sorted.add(names.get(i));
        }
        System.out.println("  sorted:"+sorted);

        return new Pair<>(originalElement, new ParameterInfoArgumentList(sorted, index, false));
    }

    private int updateFirstFunctionIndex(OCamlInferredSignature annotation, int firstFunctionIndex) {
        if (firstFunctionIndex != -1)
            firstFunctionIndex++;

        if (annotation.type.contains(OCamlLanguage.FUNCTION_SIGNATURE_SEPARATOR)) {
            // fix: bypass operators i.e. Stdlib.( + ) for instance
            String name = annotation.name;
            if (name == null) return firstFunctionIndex; // not null tho
            int dot = name.indexOf('.');
            if (dot != -1) name = name.substring(dot+1);
            // aside from operators
            if (!name.startsWith("("))
                firstFunctionIndex = 0;
        }

        return firstFunctionIndex;
    }

    @Override
    public void showParameterInfo(@NotNull PsiElement element, @NotNull CreateParameterInfoContext context) {
        context.showHint(element, element.getTextOffset(), this);
        System.out.println("  show");
    }

    @Override
    public void updateParameterInfo(@NotNull PsiElement psiParameters, @NotNull UpdateParameterInfoContext context) {
        System.out.println("  update");
    }

    @Override public void updateUI(ParameterInfoArgumentList p, @NotNull ParameterInfoUIContext context) {
        System.out.println("  update UI");

        StringBuilder b = new StringBuilder();
        int i = p.currentArgumentIndex == 0 ? 0 : 1;
        int startHighLight = 0;
        int stopHighlight = 0;
        for (String name : p.names) {
            String newText = name+", ";
            int newLength = newText.length();

            // set highlight
            if (p.currentArgumentIndex == i) {
                startHighLight = b.length();
                stopHighlight = startHighLight + newLength;
            }

            // update
            b.append(newText);
            i++;
        }
        String text = b.toString();
        text = text.substring(0, text.length() - 2);

        context.setupUIComponentPresentation(text,
                startHighLight, stopHighlight, p.isDisabled,
                p.isStrikeout, false, context.getDefaultParameterColor());
    }

    public static final class ParameterInfoArgumentList {
        public final List<String> names;
        public final List<String> defaultValues;
        public final int currentArgumentIndex;
        public final boolean isDisabled;
        public final boolean isStrikeout;

        public ParameterInfoArgumentList(List<String> names, int currentArgumentIndex, boolean isStrikeout) {
            this(names, List.of(), currentArgumentIndex, false, isStrikeout);
        }

        public ParameterInfoArgumentList(List<String> names, List<String> defaultValues,
                                         int currentArgumentIndex, boolean isDisabled,
                                         boolean isStrikeout) {

            this.names = names;
            this.defaultValues = defaultValues;
            this.currentArgumentIndex = currentArgumentIndex;
            this.isDisabled = isDisabled;
            this.isStrikeout = isStrikeout;
        }
    }
}
