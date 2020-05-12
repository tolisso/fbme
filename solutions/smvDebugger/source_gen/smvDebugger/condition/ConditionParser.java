package smvDebugger.condition;

/*Generated by MPS */


public class ConditionParser {
  private String str;
  private int index;

  public Expression parse(final String str) {
    this.str = cleanWhitespaces(str);
    this.index = 0;
    return disjunction();
  }

  private String cleanWhitespaces(final String str) {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < str.length(); i++) {
      final char ch = str.charAt(i);
      if (!(Character.isWhitespace(ch))) {
        builder.append(ch);
      }
    }
    return builder.toString();
  }

  private Expression disjunction() {
    Expression result = conjunction();
    while (index < str.length()) {
      if (compare("||")) {
        result = new Disjunction(result, conjunction());
      } else {
        break;
      }
    }
    return result;
  }

  private Expression conjunction() {
    Expression result = equalityOrInequality();
    while (index < str.length()) {
      if (compare("&&")) {
        result = new Conjunction(result, equalityOrInequality());
      } else {
        break;
      }
    }
    return result;
  }

  private Expression equalityOrInequality() {
    Expression result = negation();
    while (index < str.length()) {
      if (compare("==")) {
        result = new Equality(result, negation());
      } else if (compare("!=")) {
        result = new Inequality(result, negation());
      }
    }
    return result;
  }


  private Expression negation() {
    if (compare("!")) {
      return new Negation(negation());
    }
    return brackets();
  }


  private Expression brackets() {
    if (compare("(")) {
      Expression result = disjunction();
      index++;
      return result;
    }
    return argument();
  }

  private Expression argument() {
    final StringBuilder builder = new StringBuilder();
    while (index < str.length() && (Character.isLetterOrDigit(str.charAt(index)) || str.charAt(index) == '.')) {
      builder.append(str.charAt(index));
      index++;
    }
    return new Argument(builder.toString());
  }

  private boolean compare(final String op) {
    if (op.length() == 2 && str.charAt(index) == op.charAt(0)) {
      index++;
      return true;
    } else if (str.charAt(index) == op.charAt(0) && str.charAt(index) == op.charAt(1)) {
      index += 2;
      return true;
    }
    return false;
  }
}