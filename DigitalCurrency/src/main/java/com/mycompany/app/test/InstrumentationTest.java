package com.mycompany.app.test;


import com.mycompany.app.POP.FileKernel;
import com.mycompany.app.TODA.InstrumentationAgent;
import com.mycompany.app.TODA.MerkleProof;

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