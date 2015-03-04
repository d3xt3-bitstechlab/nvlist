package nl.weeaboo.vn.script.lua;

import java.util.ArrayList;
import java.util.List;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.link.LuaLink;
import nl.weeaboo.vn.script.ScriptException;

import org.luaj.vm2.LuaThread;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.DebugLib;

public final class LuaScriptUtil {

    private static final int DEFAULT_STACK_LIMIT = 8;
    private static final String LVN_PATTERN = ".lvn:";

    private LuaScriptUtil() {
    }

    public static LuaScriptFunction toScriptFunction(Varargs args, int offset) {
        return new LuaScriptFunction(args.checkclosure(offset), args.subargs(offset+1));
    }

    public static ScriptException toScriptException(String message, LuaException e) {
        ScriptException se = new ScriptException(message + ": " + e.getMessage());
        se.setStackTrace(e.getStackTrace());
        return se;
    }

    public static LuaScriptThread createPersistentThread(LuaRunState runState) {
        LuaLink luaLink = new LuaLink(runState);
        luaLink.setPersistent(true);
        return new LuaScriptThread(luaLink);
    }

    public static String[] getLuaStack() {
        return getLuaStack(LuaThread.getRunning());
    }
    public static String[] getLuaStack(LuaThread thread) {
        if (thread == null) {
            return null;
        }

        List<String> result = new ArrayList<String>();
        for (int level = 0; level < DEFAULT_STACK_LIMIT; level++) {
            String line = DebugLib.fileline(thread, level);
            if (line == null) break;

            result.add(line);
        }
        return result.toArray(new String[result.size()]);
    }

    public static String getNearestLVNSrcloc(String[] stack) {
        if (stack == null) {
            return null;
        }

        for (String frame : stack) {
            if (frame.contains(LVN_PATTERN)) {
                return frame;
            }
        }
        return null;
    }

    public static String getNearestLVNSrcloc(LuaThread thread) {
        if (thread == null) {
            return null;
        }
        for (int level = 0; level < DEFAULT_STACK_LIMIT; level++) {
            String line = DebugLib.fileline(thread, level);
            if (line == null) break;

            if (line.contains(LVN_PATTERN)) {
                return line;
            }
        }
        return null;
    }

}
