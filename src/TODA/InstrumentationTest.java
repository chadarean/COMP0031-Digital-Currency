package src.TODA;


import java.util.ArrayList;
import java.util.List;

import src.POP.FileKernel;

public class InstrumentationTest {

    public static void printObjectSize(Object object) {
        System.out.println("Object type: " + object.getClass() + ", size: " + InstrumentationAgent.getObjectSize(object) + " bytes");
    }

    public static void main(String[] arguments) {
        MerkleProof proof = new MerkleProof();
        FileKernel fk = new FileKernel("issuedCycleRoot", "creatorAddress", "fileTypeIdentifier", "payloadHash", "encumbranceHash");
        printObjectSize(proof);
        printObjectSize(fk);
    }
}