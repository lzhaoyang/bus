/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2020 aoju.org OSHI and other contributors.                 *
 *                                                                               *
 * Permission is hereby granted, free of charge, to any person obtaining a copy  *
 * of this software and associated documentation files (the "Software"), to deal *
 * in the Software without restriction, including without limitation the rights  *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     *
 * copies of the Software, and to permit persons to whom the Software is         *
 * furnished to do so, subject to the following conditions:                      *
 *                                                                               *
 * The above copyright notice and this permission notice shall be included in    *
 * all copies or substantial portions of the Software.                           *
 *                                                                               *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, *
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN     *
 * THE SOFTWARE.                                                                 *
 ********************************************************************************/
package org.aoju.bus.health.windows.hardware;

import com.sun.jna.platform.win32.IPHlpAPI;
import com.sun.jna.platform.win32.IPHlpAPI.MIB_IFROW;
import com.sun.jna.platform.win32.IPHlpAPI.MIB_IF_ROW2;
import com.sun.jna.platform.win32.VersionHelpers;
import org.aoju.bus.core.annotation.ThreadSafe;
import org.aoju.bus.health.Builder;
import org.aoju.bus.health.builtin.hardware.AbstractNetworkIF;
import org.aoju.bus.health.builtin.hardware.NetworkIF;
import org.aoju.bus.logger.Logger;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * WindowsNetworks class.
 *
 * @author Kimi Liu
 * @version 6.1.0
 * @since JDK 1.8+
 */
@ThreadSafe
public final class WindowsNetworkIF extends AbstractNetworkIF {

    private static final boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();
    private static final byte CONNECTOR_PRESENT_BIT = 0b00100000;

    private int ifType;
    private int ndisPhysicalMediumType;
    private boolean connectorPresent;
    private long bytesRecv;
    private long bytesSent;
    private long packetsRecv;
    private long packetsSent;
    private long inErrors;
    private long outErrors;
    private long inDrops;
    private long collisions;
    private long speed;
    private long timeStamp;

    public WindowsNetworkIF(NetworkInterface netint) {
        super(netint);
        updateAttributes();
    }

    /**
     * Gets the network interfaces on this machine
     *
     * @return An {@code UnmodifiableList} of {@link NetworkIF} objects representing
     * the interfaces
     */
    public static List<NetworkIF> getNetworks() {
        return Collections.unmodifiableList(
                getNetworkInterfaces().stream().map(WindowsNetworkIF::new).collect(Collectors.toList()));
    }

    @Override
    public int getIfType() {
        return this.ifType;
    }

    @Override
    public int getNdisPhysicalMediumType() {
        return this.ndisPhysicalMediumType;
    }

    @Override
    public boolean isConnectorPresent() {
        return this.connectorPresent;
    }

    @Override
    public long getBytesRecv() {
        return this.bytesRecv;
    }

    @Override
    public long getBytesSent() {
        return this.bytesSent;
    }

    @Override
    public long getPacketsRecv() {
        return this.packetsRecv;
    }

    @Override
    public long getPacketsSent() {
        return this.packetsSent;
    }

    @Override
    public long getInErrors() {
        return this.inErrors;
    }

    @Override
    public long getOutErrors() {
        return this.outErrors;
    }

    @Override
    public long getInDrops() {
        return this.inDrops;
    }

    @Override
    public long getCollisions() {
        return this.collisions;
    }

    @Override
    public long getSpeed() {
        return this.speed;
    }

    @Override
    public long getTimeStamp() {
        return this.timeStamp;
    }

    @Override
    public boolean updateAttributes() {
        // MIB_IFROW2 requires Vista (6.0) or later.
        if (IS_VISTA_OR_GREATER) {
            // Create new MIB_IFROW2 and set index to this interface index
            MIB_IF_ROW2 ifRow = new MIB_IF_ROW2();
            ifRow.InterfaceIndex = queryNetworkInterface().getIndex();
            if (0 != IPHlpAPI.INSTANCE.GetIfEntry2(ifRow)) {
                // Error, abort
                Logger.error("Failed to retrieve data for interface {}, {}", queryNetworkInterface().getIndex(),
                        getName());
                return false;
            }
            this.ifType = ifRow.Type;
            this.ndisPhysicalMediumType = ifRow.PhysicalMediumType;
            this.connectorPresent = (ifRow.InterfaceAndOperStatusFlags & CONNECTOR_PRESENT_BIT) > 0;
            this.bytesSent = ifRow.OutOctets;
            this.bytesRecv = ifRow.InOctets;
            this.packetsSent = ifRow.OutUcastPkts;
            this.packetsRecv = ifRow.InUcastPkts;
            this.outErrors = ifRow.OutErrors;
            this.inErrors = ifRow.InErrors;
            this.collisions = ifRow.OutDiscards; // closest proxy
            this.inDrops = ifRow.InDiscards; // closest proxy
            this.speed = ifRow.ReceiveLinkSpeed;
        } else {
            // Create new MIB_IFROW and set index to this interface index
            MIB_IFROW ifRow = new MIB_IFROW();
            ifRow.dwIndex = queryNetworkInterface().getIndex();
            if (0 != IPHlpAPI.INSTANCE.GetIfEntry(ifRow)) {
                // Error, abort
                Logger.error("Failed to retrieve data for interface {}, {}", queryNetworkInterface().getIndex(),
                        getName());
                return false;
            }
            this.ifType = ifRow.dwType;
            // These are unsigned ints. Widen them to longs.
            this.bytesSent = Builder.unsignedIntToLong(ifRow.dwOutOctets);
            this.bytesRecv = Builder.unsignedIntToLong(ifRow.dwInOctets);
            this.packetsSent = Builder.unsignedIntToLong(ifRow.dwOutUcastPkts);
            this.packetsRecv = Builder.unsignedIntToLong(ifRow.dwInUcastPkts);
            this.outErrors = Builder.unsignedIntToLong(ifRow.dwOutErrors);
            this.inErrors = Builder.unsignedIntToLong(ifRow.dwInErrors);
            this.collisions = Builder.unsignedIntToLong(ifRow.dwOutDiscards); // closest proxy
            this.inDrops = Builder.unsignedIntToLong(ifRow.dwInDiscards); // closest proxy
            this.speed = Builder.unsignedIntToLong(ifRow.dwSpeed);
        }
        this.timeStamp = System.currentTimeMillis();
        return true;
    }

}
