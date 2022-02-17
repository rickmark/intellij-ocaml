package com.ocaml.utils.editor;

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.ocaml.lang.utils.OCamlPsiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class ExtendedEditorActionUtil {

    /**
     * Return the selected code
     *
     * @param editor the editor
     * @return null if we aren't in a situation in which we can find code to executed,
     * otherwise the selected code
     * @deprecated this method is returning a string, we need a list of psi tags
     */
    @Deprecated
    public static @Nullable String getSelectedCode(@NotNull Editor editor) {
        // simply returns the selected code
        // user goal are beyond our understanding
        String code = editor.getSelectionModel().getSelectedText();
        if (code != null && !code.isBlank()) return code;
        // look for the tag
        Pair<PsiElement, PsiFile> res = findSelectedElement(editor);
        if (res == null) return null;
        // select
        TextRange range = res.first.getNode().getTextRange();
        Caret currentCaret = editor.getCaretModel().getCurrentCaret();
        currentCaret.setSelection(range.getStartOffset(), range.getEndOffset());
        return res.first.getText();
    }

    private static @Nullable Pair<PsiElement, PsiFile> findSelectedElement(@NotNull Editor editor) {
        // rip, we need to find what the user want to send to the console
        Project project = editor.getProject();
        if (project == null) return null;
        // find psiFile
        Document document = editor.getDocument();
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (psiFile == null) return null;
        PsiElement elementAt = psiFile.findElementAt(editor.getCaretModel().getOffset());
        if (elementAt == null) { // did we reach the end of the file?
            // maybe we can go back a bit
            elementAt = psiFile.findElementAt(editor.getCaretModel().getOffset()-1);
            if (elementAt == null) return null;
        }
        PsiElement s = OCamlPsiUtils.findStatementBefore(elementAt);
        if (s == null) {
            s = OCamlPsiUtils.findStatementAfter(elementAt);
            if (s == null) return null;
        }
        return new Pair<>(s, psiFile);
    }

    public static @Nullable ArrayList<PsiElement> getSelectedElements(@NotNull Editor editor) {
        Pair<PsiElement, PsiFile> statement = findSelectedElement(editor);
        if (statement == null) return null;
        PsiElement current = statement.first;
        ArrayList<PsiElement> candidates = new ArrayList<>();

        // go up until we found the file
        while (current != statement.second) {
            // add every candidate
            candidates.add(0, current);
            current = current.getParent();
        }

        return candidates;
    }

}