import com.example.ExampleMain;
import junit.framework.*;

/**
 * Example test class for Solution Center Java Starter Project. This class includes a basic test
 * class with a starting set-up and tear-down method.
 *
 * @author HMS Networks, MU Americas Solution Center
 * @version 1.0
 */
public class ExampleTest extends TestCase {

  /** Example value 1 for test class. */
  protected double exampleValue1;

  /** Example value 2 for test class. */
  protected double exampleValue2;

  /**
   * Main test method. Invokes all test methods with a name starting with 'test' and no required
   * parameters/arguments.
   *
   * @param args test arguments (ignored)
   */
  public static void main(String[] args) {
    junit.textui.TestRunner.run(ExampleTest.class);
  }

  /**
   * Set up required variables, classes or other resources before testing is run.
   *
   * @throws Exception if unable to perform setup
   */
  protected void setUp() throws Exception {
    exampleValue1 = 4;
    exampleValue2 = ExampleMain.getExampleValue();

    super.setUp();
  }

  /**
   * Cleanup required variables, classes, or other resources after testing has run.
   *
   * @throws Exception if unable to perform cleanup
   */
  protected void tearDown() throws Exception {
    // Clean up test variables/classes/etc
    exampleValue1 = 0;
    exampleValue2 = 0;

    super.tearDown();
  }

  /**
   * Example test method 1. This test method is automatically run because its name begins with
   * 'test' and no parameters are required.
   *
   * <p>This example test performs an example assertion on simple math addition.
   */
  public void testAdd() {
    double result = exampleValue1 + exampleValue2;
    double expectedResult = exampleValue1 + ExampleMain.getExampleValue();
    assertTrue(result == expectedResult);
  }

  /**
   * Example test method 2. This test method is automatically run because its name begins with
   * 'test' and no parameters are required.
   *
   * <p>This example test performs an example assertion on simple math multiplication.
   */
  public void testMultiply() {
    double result = exampleValue1 * exampleValue2;
    double expectedResult = exampleValue1 * ExampleMain.getExampleValue();
    assertTrue(result == expectedResult);
  }
}
