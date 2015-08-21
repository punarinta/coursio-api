using System;
using Coursio;

namespace CoursioTest
{
    class MainClass
    {
        public static void Main (string[] args)
        {
            var API = new CoursioApi (YOUR_PUBLIC_KEY, YOUR_PRIVATE_KEY, YOUR_SALT);

            string result = API.Exec ("dashboard", "read");
            Console.WriteLine (result);
        }
    }
}
