package cn.kurt6.unknown_echoes.client;

import com.mojang.logging.LogUtils;

import java.lang.reflect.Method;
import java.util.function.Supplier;

public final class ClientShaderCompat {
    public enum Pass { PARTICLES, LATE, SKIP }

    private static final Supplier<Pass> STATE = load(ClientShaderCompat.class.getClassLoader());

    private ClientShaderCompat() { }

    public static Pass pass() {
        return STATE.get();
    }

    // 只反射公开 API，未安装 Iris 的客户端不能链接它的内部实现。
    static Supplier<Pass> load(ClassLoader loader) {
        try {
            return bind(Class.forName("net.irisshaders.iris.api.v0.IrisApi", true, loader));
        } catch (ClassNotFoundException absent) {
            return () -> Pass.PARTICLES;
        } catch (ReflectiveOperationException | LinkageError error) {
            LogUtils.getLogger().warn("Cannot query Iris API; combat effects will use the late pass", error);
            return () -> Pass.LATE;
        }
    }

    static Supplier<Pass> bind(Class<?> apiClass) throws ReflectiveOperationException {
        Object api = apiClass.getMethod("getInstance").invoke(null);
        Method enabled = apiClass.getMethod("isShaderPackInUse");
        Method shadow = apiClass.getMethod("isRenderingShadowPass");
        return new Supplier<>() {
            private boolean warned;

            @Override
            public Pass get() {
                try {
                    if ((boolean) shadow.invoke(api)) {
                        return Pass.SKIP;
                    }
                    return (boolean) enabled.invoke(api) ? Pass.LATE : Pass.PARTICLES;
                } catch (ReflectiveOperationException | LinkageError error) {
                    if (!warned) {
                        warned = true;
                        LogUtils.getLogger().warn("Iris render state is unavailable; using the late combat pass", error);
                    }
                    return Pass.LATE;
                }
            }
        };
    }
}
