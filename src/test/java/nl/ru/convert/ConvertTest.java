package nl.ru.convert;


import junit.framework.JUnit4TestAdapter;
import org.junit.Test;

public class ConvertTest {

    @Test
    public void convert() {
        Args args = new Args();
        args.index = "../tiger";
    }

    public static junit.framework.Test suite() { return new JUnit4TestAdapter(ConvertTest.class); }
}
