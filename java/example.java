import CoursioApi.CoursioApi;public class example{    public static void main(String args[]) throws Exception    {        CoursioApi API = new CoursioApi(YOUR_PUBLIC_KEY, YOUR_PRIVATE_KEY, YOUR_SALT);        String result = API.Exec ("dashboard", "read", null);        System.out.println (result);    }}