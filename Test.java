import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import static java.util.concurrent.Executors.newFixedThreadPool;

public class Test {
  private static final int TIMEOUT_SECONDS = 5;

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface Case {}

  private static class CaseRun {
    final String name;
    final List<String> problems = new ArrayList<>();
    volatile long start;
    volatile boolean finished;
    volatile boolean threw;

    CaseRun(String name) {
      this.name = name;
    }
  }

  private static final ThreadLocal<CaseRun> current = new ThreadLocal<>();

  private static ExecutorService pool;
  private static ExecutorCompletionService<CaseRun> service;
  private static List<CaseRun> runs;

  public Test() {
    init();
  }

  private static void init() {
    pool = newFixedThreadPool(3, r -> {
      Thread t = new Thread(r);
      t.setDaemon(true);
      return t;
    });
    service = new ExecutorCompletionService<>(pool);
    runs = new ArrayList<>();
  }

  @FunctionalInterface
  public interface Thrower {
    void run() throws Exception;
  }

  private static CaseRun requireRun(String method) {
    CaseRun run = current.get();
    if (run == null) {
      throw new IllegalStateException(method + " must be called from a @Test.Case method");
    }
    return run;
  }

  public static void assertTrue(Callable<Boolean> function) {
    CaseRun run = requireRun("assertTrue");

    StackTraceElement[] frames = Thread.currentThread().getStackTrace();
    String className = frames[2].getClassName();
    String assertion = String.format("%s:%d", frames[2].getFileName(), frames[2].getLineNumber());

    try {
      if (!function.call()) {
        run.problems.add("failed assertion: " + assertion);
      }
    } catch (Exception | StackOverflowError e) {
      run.problems.add("error: " + assertion + " (" + describe(e, className) + ")");
      run.threw = true;
    }
  }

  public static void assertThrows(Class<? extends Throwable> expected, Thrower function) {
    CaseRun run = requireRun("assertThrows");

    StackTraceElement[] frames = Thread.currentThread().getStackTrace();
    String assertion = String.format("%s:%d", frames[2].getFileName(), frames[2].getLineNumber());

    try {
      function.run();
      run.problems.add("failed assertion: " + assertion + " (expected " + expected.getSimpleName()
        + ", nothing thrown)");
    } catch (Throwable t) {
      if (!expected.isInstance(t)) {
        run.problems.add("failed assertion: " + assertion + " (expected " + expected.getSimpleName()
          + ", got " + t.getClass().getSimpleName() + ")");
      }
    }
  }

  private static String describe(Throwable e, String className) {
    String description = e.getClass().getSimpleName();
    for (StackTraceElement frame : e.getStackTrace()) {
      if (className.equals(frame.getClassName())) {
        return description + " from " + frame.getFileName() + ":" + frame.getLineNumber();
      }
    }
    return description;
  }

  private static boolean anyTimedOut() {
    for (CaseRun run : runs) {
      long start = run.start;
      if (start > 0 && !run.finished
          && System.nanoTime() - start > TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS)) {
        return true;
      }
    }
    return false;
  }

  public static boolean run() {
    return run(StackWalker
      .getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
      .getCallerClass());
  }

  public static boolean run(Class<?> cases) {
    if (pool == null) {
      init();
    }

    for (Method method : cases.getDeclaredMethods()) {
      if (!method.isAnnotationPresent(Case.class)) {
        continue;
      }

      CaseRun run = new CaseRun(method.getName());
      runs.add(run);

      service.submit(() -> {
        run.start = System.nanoTime();
        current.set(run);
        try {
          method.setAccessible(true);
          Object instance = null;
          if (!Modifier.isStatic(method.getModifiers())) {
            Constructor<?> constructor = cases.getDeclaredConstructor();
            constructor.setAccessible(true);
            instance = constructor.newInstance();
          }
          method.invoke(instance);
        } catch (InvocationTargetException e) {
          run.problems.add("error: " + run.name + " (" + describe(e.getCause(), cases.getName()) + ")");
          run.threw = true;
        } catch (Exception | StackOverflowError e) {
          run.problems.add("error: " + run.name + " (" + describe(e, cases.getName()) + ")");
          run.threw = true;
        } finally {
          current.remove();
          run.finished = true;
        }
        return run;
      });
    }

    int numFailed = 0, numThrew = 0, numDone = 0;
    List<String> unfinished = new ArrayList<>();

    while (numDone < runs.size()) {
      try {
        Future<CaseRun> future = service.poll(100, TimeUnit.MILLISECONDS);
        if (future == null) {
          if (!anyTimedOut()) {
            continue;
          }

          for (CaseRun run : runs) {
            if (!run.finished) {
              IO.print("U");
              unfinished.add(run.name);
            }
          }

          IO.print("\nTests stopped after " + TIMEOUT_SECONDS + "s");
          break;
        }

        CaseRun run = future.get();
        numDone++;

        if (run.threw) {
          IO.print("T");
          numThrew++;
        } else if (!run.problems.isEmpty()) {
          IO.print("F");
          numFailed++;
        } else {
          IO.print(".");
        }
      } catch (InterruptedException | ExecutionException e) {
        break;
      }
    }

    pool.shutdownNow();

    IO.println(String.format(
      "\n%d failed, %d threw, %d unfinished",
      numFailed, numThrew, unfinished.size()
    ));

    for (CaseRun run : runs) {
      if (run.finished && !run.problems.isEmpty() && !unfinished.contains(run.name)) {
        IO.println("case " + run.name + "()");
        for (String problem : run.problems) {
          IO.println("  " + problem);
        }
      }
    }

    for (String name : unfinished) {
      IO.println("unfinished case " + name + "()");
    }

    boolean passed = numFailed == 0 && numThrew == 0 && unfinished.isEmpty();

    init();
    return passed;
  }
}
