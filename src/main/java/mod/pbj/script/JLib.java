package mod.pbj.script;

public final class JLib {
	//    public static final Logger LOGGER = Logger.getLogger("PBJ-Scripts");
	//    static final Class<?>[] DEFAULT_IMPORTS = new Class[] {
	//    };
	//    public static void jImport(String... obj) {
	//        String startPkg = obj[0];
	//        for(Object arg : obj) {
	//            if(arg == obj[0]) continue;
	//            if (!(arg instanceof String)) {
	//                throw Context.reportRuntimeError("PBJImporter: jImport requires class names as arguments");
	//            }
	//            try {
	//                Class<?> cl = Class.forName(startPkg + "." + arg);
	//                importClass(scope, new NativeJavaClass(scope, cl));
	//            } catch (ClassNotFoundException e) {
	//                throw Context.reportRuntimeError("PBJImporter: class " + arg + " not found");
	//            }
	//        }
	//    }
	//
	//    public static <E extends Event> void ModEvent(Class<E> clazz, Consumer<E> eventConsumer) {
	//        if (eventConsumer == null) {
	//            throw Context.reportRuntimeError("PBJImporter: registerEvent requires a non-null event consumer");
	//        }
	//        PointBlankJelly.modEventBus.addListener(EventPriority.NORMAL, false, clazz, eventConsumer);
	//    }
	//    public static <E extends Event> void ForgeEvent(String className, Consumer<E> eventConsumer) throws
	//    ClassNotFoundException {
	//        if (eventConsumer == null) {
	//            throw Context.reportRuntimeError("PBJImporter: registerEvent requires a non-null event consumer");
	//        }
	//        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, (Class<E>) Class.forName(className),
	//        eventConsumer);
	//    }
	//
	//    public static void println(Object message) {
	//        System.out.println(message);
	//    }
	//    public static void print(Object message) {
	//        System.out.print(message);
	//    }
	//    public static void log(Object message) {
	//        LOGGER.info(message.toString());
	//    }
	//    public static void logError(Object message) {
	//        LOGGER.severe(message.toString());
	//    }
	//    public static void logWarning(Object message) {
	//        LOGGER.warning(message.toString());
	//    }
}
