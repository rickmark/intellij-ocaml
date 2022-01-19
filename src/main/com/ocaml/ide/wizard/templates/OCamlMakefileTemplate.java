package com.ocaml.ide.wizard.templates;

import com.intellij.ide.util.projectWizard.*;
import com.intellij.openapi.diagnostic.*;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.*;
import com.intellij.openapi.vfs.*;
import com.intellij.platform.*;
import com.ocaml.*;
import com.ocaml.icons.*;
import com.ocaml.utils.files.*;
import com.ocaml.utils.logs.*;
import org.jetbrains.annotations.*;

import javax.swing.*;
import java.io.*;

/**
 * Allow the creation of a project with
 * - src
 * - src/hello_world.ml
 * - src/hello_world.mli
 * - src/test_hello_world.ml
 * - Makefile
 */
class OCamlMakefileTemplate implements ProjectTemplate, TemplateBuildInstructions {

    private static final Logger LOG = OCamlLogger.getTemplateInstance("makefile");

    @SuppressWarnings("UnstableApiUsage") @Override public @NotNull @NlsContexts.Label String getName() {
        return OCamlBundle.message("template.makefile.title");
    }

    @SuppressWarnings("UnstableApiUsage") @Override public @Nullable @NlsContexts.DetailedDescription String getDescription() {
        return OCamlBundle.message("template.makefile.description");
    }

    @Override public Icon getIcon() {
        return OCamlIcons.External.MAKEFILE;
    }

    @Override public @NotNull AbstractModuleBuilder createModuleBuilder() {
        throw new UnsupportedOperationException("OCamlMakefileTemplate#createModuleBuilder should not be called");
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    @Override public @Nullable ValidationInfo validateSettings() {
        return null;
    }

    @Override public void createFiles(ModifiableRootModel rootModel, VirtualFile sourceRoot) {
        File sourceRootFile = VfsUtilCore.virtualToIoFile(sourceRoot);
        OCamlFileUtils.createFile(sourceRootFile, "hello_world.mli", "val hello_world : unit -> unit", LOG);
        OCamlFileUtils.createFile(sourceRootFile, "hello_world.ml", "let hello_world () = Format.printf \"Hello, World!@.\"", LOG);
        OCamlFileUtils.createFile(sourceRootFile, "test_hello_world.ml","open Hello_world\n\nlet _ = hello_world ()", LOG);

        String makefileContent = OCamlFileUtils.loadFileContent("/templates/Makefile/Makefile", LOG);
        OCamlFileUtils.createFile(sourceRootFile.getParentFile(), "Makefile", makefileContent, LOG);
    }
}