/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.datavyu.models;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class provides unique identifiers.
 *
 * The static generateIdentifier function provides new, unique identifiers in a thread-safe manner.
 */
public class Identifier {

    /** Lazily initialized hash code */
    private volatile int hashCode;

    /** Sequence number for the identifier */
    private final long sequenceNumber;

    /** Atomic integer that counts the number of identifiers (thread safe) */
    private static final AtomicInteger count = new AtomicInteger(0);

    /**
     * Create an identifier with a sequence number.
     *
     * @param sequenceNumber The sequence number for this identifier.
     */
    private Identifier(final long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * Get the long representation of this identifier
     *
     * @return Long representation
     */
    public long asLong() {
        return sequenceNumber;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        // For implementation details see, Josh Bloch's Effective Java, 2nd edition, page 47ff.
        if (hashCode == 0) {
            hashCode = 31 * 17 + (int) (sequenceNumber ^ (sequenceNumber >>> 32));
        }
        return hashCode;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        return (this == obj) || (obj != null && getClass() == obj.getClass()
                && sequenceNumber == ((Identifier) obj).sequenceNumber);
    }

    @Override
    public String toString() {
        return "Identifier: " + sequenceNumber;
    }

    /**
     * Generates a new, unique identifier.
     *
     * @return A new, unique identifier.
     */
    public static Identifier generateIdentifier() {
        return new Identifier(count.getAndIncrement());
    }
}
