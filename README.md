Like [`UnitTest.java`](https://github.com/abreen/UnitTest), but even smaller and safer.

- Annotate test cases with `@Test.Case`
- Call `Test.assertTrue()`
- When you want to run, call `Test.run()`

### Usage

```
% java Smudge.java
FT..U
Tests stopped after 5s
1 failed, 1 threw, 1 unfinished
failing
  failed: Smudge.java:44
throwing
  error: Smudge.java:49 (NullPointerException from Smudge.java:50)
unfinished: slowTimedOut
```

### `Smudge.java`

```java
String smudge(String s) {
  if (s.equals("")) {
    return "";
  }

  if (s.length() > 1) {
    String first = s.substring(0, 1);
    String rest = smudge(s.substring(2));
    return first + first + rest;
  } else {
    // s.length() == 1
    return s;
  }
}

// slow, but not timed out
@Test.Case
void slowButNotTimedOut() {
  Test.assertTrue(() -> {
    Thread.sleep(1000);
    return "".equals(smudge(""));
  });
}

// slow, timed out
@Test.Case
void slowTimedOut() {
  Test.assertTrue(() -> {
    Thread.sleep(10000);
    return "a".equals(smudge("a"));
  });
}

@Test.Case
void basic() {
  Test.assertTrue(() -> "aa".equals(smudge("ab")));
  Test.assertTrue(() -> "aac".equals(smudge("abc")));
  Test.assertTrue(() -> "aacc".equals(smudge("abcd")));
}

// intentionally failing test
@Test.Case
void failing() {
  Test.assertTrue(() -> "1234".equals(smudge("abcd")));
}

@Test.Case
void throwing() {
  Test.assertTrue(() -> {
    throw new NullPointerException();
  });
}

void main() {
  System.exit(Test.run() ? 0 : 1);
}
```