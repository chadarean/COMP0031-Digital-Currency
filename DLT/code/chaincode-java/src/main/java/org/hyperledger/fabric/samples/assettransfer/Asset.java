/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.assettransfer;

import java.util.Objects;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

@DataType()
public final class Asset {

    @Property()
    private final String cycleRootID;

    @Property()
    private final String cycleRootHash;

    public String getCycleRootID() {
        return cycleRootID;
    }

    public String getCycleRootHash() {
        return cycleRootHash;
    }

    public Asset(@JsonProperty("cycleRootID") final String cycleRootID,
                 @JsonProperty("cycleRootHash") final String cycleRootHash) {
        this.cycleRootID = cycleRootID;
        this.cycleRootHash = cycleRootHash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        Asset other = (Asset) obj;

        return Objects.deepEquals(
                new String[] {getCycleRootID(), getCycleRootHash()},
                new String[] {other.getCycleRootID(), other.getCycleRootHash()});
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCycleRootID(), getCycleRootHash());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [cycleRootID=" + cycleRootID
                + ", cycleRootHash=" + cycleRootHash + "]";
    }
}
