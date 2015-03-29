package nl.weeaboo.vn.core.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import nl.weeaboo.vn.IContext;
import nl.weeaboo.vn.IContextFactory;
import nl.weeaboo.vn.IContextManager;
import nl.weeaboo.vn.IRenderEnv;

public class ContextManager implements IContextManager {

    private static final long serialVersionUID = BaseImpl.serialVersionUID;

    private final IContextFactory<Context> contextFactory;

    private final List<Context> contexts = new ArrayList<Context>();

    public ContextManager(IContextFactory<Context> contextFactory) {
        this.contextFactory = contextFactory;
    }

    //Functions
    @Override
    public final Context createContext() {
        Context context = contextFactory.newContext();
        register(context);
        return context;
    }

    private void register(Context context) {
        if (contexts.contains(context)) {
            return;
        }

        contexts.add(context);
    }

    private Context checkContains(IContext ctxt) {
        for (Context context : contexts) {
            if (context == ctxt) {
                return context;
            }
        }
        throw new IllegalArgumentException("Context (" + ctxt + ") is not contained by this contextmanager.");
    }

    //Getters
    @Override
    public Collection<Context> getContexts() {
        return Collections.unmodifiableCollection(contexts);
    }

    @Override
    public Collection<Context> getActiveContexts() {
        List<Context> active = new ArrayList<Context>(2);
        for (Context context : contexts) {
            if (context.isActive()) {
                active.add(context);
            }
        }
        return Collections.unmodifiableCollection(active);
    }

    @Override
    public boolean isContextActive(IContext ctxt) {
        Context context = checkContains(ctxt);
        return context.isActive();
    }

    //Setters
    @Override
    public void setContextActive(IContext ctxt, boolean active) {
        Context context = checkContains(ctxt);
        context.setActive(active);
    }

    @Override
    public void setRenderEnv(IRenderEnv env) {
        for (IContext context : contexts) {
            context.setRenderEnv(env);
        }
    }

}
