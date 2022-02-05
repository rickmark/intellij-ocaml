package com.or.ide.structure;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.treeView.smartTree.ActionPresentation;
import com.intellij.ide.util.treeView.smartTree.Filter;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class NestedFunctionsFilter implements Filter {
    @Override
    public boolean isVisible(TreeElement treeNode) {
        if (treeNode instanceof StructureViewElement) {
            StructureViewElement viewElement = (StructureViewElement) treeNode;
            return viewElement.getLevel() < 2;
        }
        return true;
    }

    @Override
    public boolean isReverted() {
        return true;
    }

    @Override
    public @NotNull ActionPresentation getPresentation() {
        return new ActionPresentation() {
            @Override
            public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getText() {
                return "Show nested Functions";
            }

            @Override
            public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String getDescription() {
                return "Show nested functions";
            }

            @Override
            public @NotNull Icon getIcon() {
                return AllIcons.General.InspectionsEye;
            }
        };
    }

    @Override public @NotNull String getName() {
        return "ShowNestedFunctions";
    }
}
