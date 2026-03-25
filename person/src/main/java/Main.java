
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.person.person;

public class Main {
    public static void main(String[] args) throws IOException {
        String jsonInput = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "age": 30,
                    "emailAddress": "kaizerdavid.palabay@lorma.edu",
                    "phoneNumber": "0970-120-1016",
                    "dateOfBirth": "2004-10-27",
                    "homeAddress": "67 Main St, Oklahoma City, USA",
                    "isEmployed": true,
                    "nationality": "American",
                    "gender": "Male"
                }
                """;

        Gson gson = new Gson();

        person p = gson.fromJson(jsonInput, person.class);

        System.out.printf("""
                First Name: %s
                Last Name: %s
                Age: %d
                Email Address: %s
                Phone Number: %s
                Date of Birth: %s
                Home Address: %s
                Is Employed: %b
                Nationality: %s
                Gender: %s
                """, p.getFirstName(), p.getLastName(), p.getAge(), p.getEmailAddress(), p.getPhoneNumber(), p.getDateOfBirth(), p.getHomeAddress(), p.isEmployed(), p.getNationality(), p.getGender());

                FileWriter fw = new FileWriter("person/src/data/person.json");
                gson.toJson(p, fw);
                fw.close();

                FileReader fr = new FileReader("person/src/data/person.json");

                Type personType = new TypeToken<person>(){}.getType();
                List<person> personList = gson.fromJson(fr, personType);

                personList.forEach(person -> {
                    System.out.printf("""
                    First Name: %s
                    Last Name: %s
                    Age: %d
                    Email Address: %s
                    Phone Number: %s
                    Date of Birth: %s
                    Home Address: %s
                    Is Employed: %b
                    """, person.getFirstName(), person.getLastName(), person.getAge(), person.getEmailAddress(), person.getPhoneNumber(), person.getDateOfBirth(), person.getHomeAddress(), person.isEmployed());
                });
    }
}
