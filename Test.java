import java.util.*;
import java.util.concurrent.*;
import static java.util.concurrent.Executors.newFixedThreadPool;

public class Test {
  private static ExecutorService pool;
  private static ExecutorCompletionService<Result> service;
  private static String lastFileName;
  private static int numAssertions;
  private static List<Result> results;
  private static List<String> unfinished;

  record Result(String assertion, boolean testPassed, String error) {}

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
    lastFileName = null;
    numAssertions = 0;
    results = new ArrayList<>(200);
    unfinished = new ArrayList<>();
  }

  public static void assertTrue(Callable<Boolean> function) {
    if (pool == null) {
      init();
    }

    StackTraceElement[] frames = Thread.currentThread().getStackTrace();
    String fileName = frames[2].getFileName();

    if (lastFileName != null && !fileName.equals(lastFileName)) {
      throw new IllegalStateException("only one file may be tested at once");
    } else {
      lastFileName = fileName;
    }

    int lineNumber = frames[2].getLineNumber();

    String assertion = String.format("%s:%d", fileName, lineNumber);
    unfinished.add(assertion);

    Callable<Result> task = () -> {
      boolean testPassed;
      String error = null;
      try {
        testPassed = function.call();
      } catch (Exception | StackOverflowError e) {
        testPassed = false;
        error = describe(e, fileName);
      }
      return new Result(assertion, testPassed, error);
    };

    service.submit(task);
    numAssertions++;
  }

  private static String describe(Throwable e, String fileName) {
    String description = e.getClass().getSimpleName();
    for (StackTraceElement frame : e.getStackTrace()) {
      if (fileName.equals(frame.getFileName())) {
        return description + " from " + fileName + ":" + frame.getLineNumber();
      }
    }
    return description;
  }

  public static void printResults() {
    int numFailed = 0, numUnfinished = 0;

    for (int i = 0; i < numAssertions; i++) {
      try {
        Future<Result> future = service.poll(5, TimeUnit.SECONDS);
        if (future == null) {
          numUnfinished = unfinished.size();
          IO.print("U".repeat(numUnfinished) + "\nTests stopped after 5s");
          break;
        }
        Result r = future.get();
        results.add(r);
        unfinished.remove(r.assertion);
        if (r.testPassed) {
          IO.print(".");
        } else {
          IO.print("F");
          numFailed++;
        }
      } catch (InterruptedException | ExecutionException e) {
        break;
      }
    }

    pool.shutdownNow();

    IO.print(String.format("\n%d failed", numFailed));
    if (numUnfinished > 0) {
      IO.print(String.format(", %d unfinished", numUnfinished));
    }
    IO.println();

    for (Result r : results) {
      if (r.error != null) {
        IO.println("error: " + r.assertion + " (" + r.error + ")");
      } else if (!r.testPassed) {
        IO.println("failed: " + r.assertion);
      }
    }

    for (String assertion : unfinished) {
      IO.println("unfinished: " + assertion);
    }

    if (numUnfinished > 0) {
      System.exit(0);
    }

    init();
  }
}
