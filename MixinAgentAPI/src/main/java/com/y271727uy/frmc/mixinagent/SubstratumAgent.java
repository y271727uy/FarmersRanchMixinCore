package com.y271727uy.frmc.mixinagent;

import java.lang.instrument.Instrumentation;

public final class SubstratumAgent {
    private SubstratumAgent() {
    }

    public static void premain(String arguments, Instrumentation instrumentation) {
        agentmain(arguments, instrumentation);
    }

    public static void agentmain(String arguments, Instrumentation instrumentation) {
        InstrumentationAccess.bind(instrumentation, Substratum.registry());
        Substratum.install(false);
    }
}
