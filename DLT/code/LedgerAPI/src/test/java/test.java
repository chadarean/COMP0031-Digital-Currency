import uk.ac.ucl.cs.digitalcurrency2021.dlt.LedgerAPI;

public class test {

    public static void main(String[] args) {


        LedgerAPI ledger = LedgerAPI.newInstance();
        try {
            ledger.connect();
            System.out.println(ledger.hasHash("root1"));
            System.out.println(ledger.getHash("root1"));
//			ledger.writeHash("root6", "fakeHash6");
            System.out.println(ledger.getHash("root6"));
            ledger.shutdown();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
