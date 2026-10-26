package com.orudesk.orudesk;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith treats each package directly under com.orudesk.orudesk (identity, shared, later party...)
 * as a module. A module's top-level package is its public API; everything in its sub-packages is internal.
 * verify() fails if one module reaches into another's internals, or if modules depend on each other in a cycle.
 */
class ModularityTest {

    private final ApplicationModules modules = ApplicationModules.of(OrudeskApplication.class);

    @Test
    void modulesOnlyUseEachOthersPublicApi() {
        modules.verify();
    }

    /** Draws the modules and their dependencies into build/spring-modulith-docs (PlantUML). */
    @Test
    void writeDiagrams() {
        new Documenter(modules).writeModulesAsPlantUml();
    }
}
