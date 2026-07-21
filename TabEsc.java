import org.json.simple.JSONArray;
public class TabEsc {
  public static void main(String[] a) {
    JSONArray arr = new JSONArray();
    arr.add("TP53\tMDM2");
    arr.add("A\"B");
    System.out.println(arr.toJSONString());
    for (int i=0;i<arr.toJSONString().length();i++) {
      char c = arr.toJSONString().charAt(i);
      if (c < 32) System.out.println("CTRL at "+i+" code="+(int)c);
    }
  }
}
