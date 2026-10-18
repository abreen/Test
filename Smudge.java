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
void throwsOnNull() {
  Test.assertThrows(NullPointerException.class, () -> smudge(null));
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