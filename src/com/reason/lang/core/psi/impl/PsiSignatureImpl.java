package com.reason.lang.core.psi.impl;

import com.intellij.psi.tree.*;
import com.reason.lang.*;
import com.reason.lang.core.*;
import com.reason.lang.core.psi.*;
import com.reason.lang.core.type.*;
import jpsplugin.com.reason.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.stream.*;

public class PsiSignatureImpl extends CompositeTypePsiElement<ORTypes> implements PsiSignature {
    PsiSignatureImpl(@NotNull ORTypes types, @NotNull IElementType elementType) {
        super(types, elementType);
    }

    @Override
    public boolean isFunction() {
        return getItems().size() > 1;
    }

    @Override
    public @NotNull String asText(@Nullable ORLanguageProperties toLang) {
        List<PsiSignatureItem> items = getItems();
        if (items.isEmpty()) {
            return "";
        }

        boolean isFunction = 1 < items.size();

        String signatureText;
        if (toLang == null || toLang.equals(getLanguage())) {
            signatureText = getText();
        } else {
            StringBuilder sb = new StringBuilder();

            List<String> conversions = items.stream().map(item -> item.asText(toLang)).collect(Collectors.toList());
            String result = conversions.remove(items.size() - 1);

            if (isFunction) {
                sb.append(Joiner.join(toLang.getParameterSeparator(), conversions));
                sb.append(toLang.getFunctionSeparator());
            }
            sb.append(result);
            signatureText = sb.toString();
        }

        String text = signatureText.replaceAll("\\s+", " ");

        return text
                .replaceAll("\\( ", "(")
                .replaceAll(", \\)", ")");
    }

    @Override
    public @NotNull List<PsiSignatureItem> getItems() {
        return ORUtil.findImmediateChildrenOfClass(this, PsiSignatureItem.class);
    }

    @Override
    public @NotNull String toString() {
        return "PsiSignature";
    }
}