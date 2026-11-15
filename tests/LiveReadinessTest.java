import app.template.extension.extension.Readiness;
import java.lang.reflect.Method;
public class LiveReadinessTest {
 public static void main(String[] args)throws Exception {Method method=Readiness.class.getDeclaredMethod("check",String.class);method.setAccessible(true);System.out.println("IPP result: "+method.invoke(null,args[0]));}
}
