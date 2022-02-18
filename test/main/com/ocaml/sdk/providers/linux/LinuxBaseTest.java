package com.ocaml.sdk.providers.linux;

import com.ocaml.sdk.providers.BaseSdkProviderTest;
import com.ocaml.sdk.utils.OCamlSdkHomeManager;

public class LinuxBaseTest extends BaseSdkProviderTest {

    protected LinuxFolders folders;

    @Override protected void setUp() throws Exception {
        super.setUp();
        folders = new LinuxFolders();
    }

    @Override protected void tearDown() throws Exception {
        super.tearDown();
        folders = null;
    }

    protected void assertWSLHomeValid(String homePath) {
        assertTrue(OCamlSdkHomeManager.isValid(homePath));
    }

    protected void assertWSLHomeInvalid(String homePath) {
        assertFalse(OCamlSdkHomeManager.isValid(homePath));
    }

}
