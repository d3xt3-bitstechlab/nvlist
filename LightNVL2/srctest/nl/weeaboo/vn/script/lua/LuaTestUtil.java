package nl.weeaboo.vn.script.lua;

import nl.weeaboo.filesystem.IFileSystem;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.vn.IContext;
import nl.weeaboo.vn.IContextManager;
import nl.weeaboo.vn.script.IScriptContext;
import nl.weeaboo.vn.script.IScriptThread;
import nl.weeaboo.vn.script.lvn.ILvnParser;
import nl.weeaboo.vn.script.lvn.LvnParserFactory;

import org.junit.Assert;
import org.luaj.vm2.LuaTable;

public final class LuaTestUtil {

    public static final String SCRIPT_HELLOWORLD = "helloworld";
    public static final String SCRIPT_YIELD = "yield";
    public static final String SCRIPT_CREATECONTEXT = "createcontext";
    public static final String SCRIPT_SCRIPTLIB = "scriptlib";

    private LuaTestUtil() {
    }

    public static LuaRunState newRunState() {
        LuaRunState runState = new LuaRunState();
        runState.registerOnThread();
        return runState;
    }

    public static LuaScriptLoader newScriptLoader(IFileSystem fileSystem) {
        LuaScriptLoader loader = new LuaScriptLoader(LuaTestUtil.newLvnParser(), fileSystem);
        loader.initEnv();
        return loader;
    }

    public static ILvnParser newLvnParser() {
        return LvnParserFactory.getParser(Integer.toString(4));
    }

    public static void assertGlobal(String name, int val) {
        LuaTable globals = LuaRunState.getCurrent().getGlobalEnvironment();
        Assert.assertEquals(val, globals.get(name).optint(0));
    }
    public static void assertGlobal(String name, Object val) {
        LuaTable globals = LuaRunState.getCurrent().getGlobalEnvironment();
        Assert.assertEquals(val, globals.get(name).optuserdata(null));
    }

    public static <T> T getGlobal(String name, Class<T> type) {
        LuaTable globals = LuaRunState.getCurrent().getGlobalEnvironment();
        return globals.get(name).optuserdata(type, null);
    }
    public static boolean hasRunnableThreads(IScriptContext context) {
        for (IScriptThread thread : context.getThreads()) {
            if (thread.isRunnable()) {
                return true;
            }
        }
        return false;
    }

    public static void waitForAllThreads(IContextManager contextManager) {
        int iteration = 0;
        while (iteration < 10000) {
            boolean anyRunnableThreads = false;

            for (IContext context : contextManager.getActiveContexts()) {
                IScriptContext scriptContext = context.getScriptContext();
                if (hasRunnableThreads(scriptContext)) {
                    anyRunnableThreads = true;
                    scriptContext.updateThreads();
                }
            }

            if (!anyRunnableThreads) {
                return;
            }
        }
        throw new AssertionError("One or more threads refuse to die");
    }
    public static void waitForAllThreads(IScriptContext context) {
        int iteration = 0;
        while (hasRunnableThreads(context)) {
            context.updateThreads();

            if (++iteration >= 10000) {
                throw new AssertionError("One or more threads refuse to die");
            }
        }
    }

}
