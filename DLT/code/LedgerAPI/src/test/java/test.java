import uk.ac.ucl.cs.digitalcurrency2021.dlt.LedgerAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class test {

    public static void main(String[] args) {


        LedgerAPI ledger = LedgerAPI.newInstance();
        try {
            ledger.connect();
            System.out.println(ledger.hasHash("root1"));
            System.out.println(ledger.getHash("root1"));

            List<Long> times = new ArrayList<>();

            for (int i= 0; i <= 100; i++) {
                System.out.println("Running " + i + "...");
                String testID = UUID.randomUUID().toString();

//                System.out.println(start);
                ledger.writeHash(testID, "fakeHash6");
                long start = System.currentTimeMillis();
                String get = ledger.getHash(testID);
//                System.out.println(ledger.getHash(testID));
                long end = System.currentTimeMillis();
//                System.out.println(end);
                long dif = end - start;
//                System.out.println(dif + "ms /" + dif / 1000 + "s");
                times.add(dif);
            }

            long total = 0;
            for (long t : times) {
                total += t;
            }
            long avg = total / times.size();
            System.out.println("Avg: " + avg);
            String data = times.stream().map(Object::toString).collect(Collectors.joining(", "));
            System.out.println(data);
            ledger.shutdown();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
