Like [`UnitTest.java`](https://github.com/abreen/UnitTest), but even smaller.

- Correct for newer Javas (doesn’t use `Thread.stop()`)
- No more annotations
- Just two methods: `assertTrue()` and `printResults()`
- Less than 150 lines

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

void main() {
  // slow, but not timed out
  Test.assertTrue(() -> {
    Thread.sleep(1000);
    return "".equals(smudge(""));
  });

  // slow, timed out
  Test.assertTrue(() -> {
    Thread.sleep(10000);
    return "a".equals(smudge("a"));
  });

  Test.assertTrue(() -> "aa".equals(smudge("ab")));
  Test.assertTrue(() -> "aac".equals(smudge("abc")));
  Test.assertTrue(() -> "aacc".equals(smudge("abcd")));

  // intentionally failing test
  Test.assertTrue(() -> "1234".equals(smudge("abcd")));

  Test.assertTrue(() -> {
    throw new NullPointerException();
  });

  Test.printResults();
}
```
