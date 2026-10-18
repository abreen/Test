import java.util.concurrent.TimeUnit;

int failures = 0;

static class Passing {
  @Test.Case
  void first() {
    Test.assertTrue(() -> true);
  }

  @Test.Case
  void second() {
    Test.assertTrue(() -> "a".equals("a"));
  }
}

static class StaticCase {
  @Test.Case
  static void works() {
    Test.assertTrue(() -> true);
  }
}

static class Failing {
  @Test.Case
  void fails() {
    Test.assertTrue(() -> false);
  }
}

static class FailsAfterPassing {
  @Test.Case
  void mixed() {
    Test.assertTrue(() -> true);
    Test.assertTrue(() -> false);
    Test.assertTrue(() -> true);
  }
}

static class OneBadAmongGood {
  @Test.Case
  void good() {
    Test.assertTrue(() -> true);
  }

  @Test.Case
  void bad() {
    Test.assertTrue(() -> false);
  }
}

static class Throwing {
  @Test.Case
  void throwsInAssertion() {
    Test.assertTrue(() -> {
      throw new NullPointerException();
    });
  }
}

static class ThrowingCaseMethod {
  @Test.Case
  void throwsOutsideAssertion() {
    throw new IllegalStateException();
  }
}

static class Overflowing {
  int recurse(int n) {
    return recurse(n + 1) + 1;
  }

  @Test.Case
  void overflows() {
    Test.assertTrue(() -> recurse(0) > 0);
  }
}

static class Sleeping {
  @Test.Case
  void sleepsForever() throws Exception {
    Thread.sleep(30000);
  }
}

static class ThrowsExpected {
  @Test.Case
  void exact() {
    Test.assertThrows(NullPointerException.class, () -> {
      throw new NullPointerException();
    });
  }

  @Test.Case
  void subclass() {
    Test.assertThrows(RuntimeException.class, () -> {
      throw new IllegalStateException();
    });
  }

  @Test.Case
  void checked() {
    Test.assertThrows(Exception.class, () -> {
      throw new Exception();
    });
  }

  @Test.Case
  void error() {
    Test.assertThrows(StackOverflowError.class, () -> new Overflowing().recurse(0));
  }
}

static class ThrowsNothing {
  @Test.Case
  void nothing() {
    Test.assertThrows(RuntimeException.class, () -> {});
  }
}

static class ThrowsWrongType {
  @Test.Case
  void wrong() {
    Test.assertThrows(IllegalStateException.class, () -> {
      throw new NullPointerException();
    });
  }
}

static class NoCases {
  void notACase() {}
}

void check(String name, boolean ok) {
  if (ok) {
    IO.println("PASS " + name);
  } else {
    failures++;
    IO.println("FAIL " + name);
  }
}

void main() {
  check("passing cases pass", Test.run(Passing.class));
  check("static case methods run", Test.run(StaticCase.class));
  check("a failing assertion fails the run", !Test.run(Failing.class));
  check("a failure after passing assertions fails the run", !Test.run(FailsAfterPassing.class));
  check("one bad case among good ones fails the run", !Test.run(OneBadAmongGood.class));
  check("an exception in an assertion fails the run", !Test.run(Throwing.class));
  check("an exception from the case method fails the run", !Test.run(ThrowingCaseMethod.class));
  check("a stack overflow fails the run", !Test.run(Overflowing.class));
  check("expected exceptions pass", Test.run(ThrowsExpected.class));
  check("assertThrows fails when nothing is thrown", !Test.run(ThrowsNothing.class));
  check("assertThrows fails on the wrong exception type", !Test.run(ThrowsWrongType.class));
  check("a class with no cases passes", Test.run(NoCases.class));
  check("state resets between runs", !Test.run(Failing.class) && Test.run(Passing.class));

  long begin = System.nanoTime();
  boolean passed = Test.run(Sleeping.class);
  long seconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - begin);
  check("a case past the timeout fails the run without waiting for it", !passed && seconds < 15);

  boolean rejected = false;
  try {
    Test.assertTrue(() -> true);
  } catch (IllegalStateException e) {
    rejected = true;
  }
  check("assertTrue outside a case is rejected", rejected);

  rejected = false;
  try {
    Test.assertThrows(Exception.class, () -> {});
  } catch (IllegalStateException e) {
    rejected = true;
  }
  check("assertThrows outside a case is rejected", rejected);

  IO.println(failures == 0 ? "all checks passed" : failures + " checks failed");
  System.exit(failures == 0 ? 0 : 1);
}
