/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.type;

import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.function.LiteralParameters;
import com.facebook.presto.spi.function.ScalarOperator;
import com.facebook.presto.spi.function.SqlType;
import com.facebook.presto.spi.type.StandardTypes;
import com.google.common.net.InetAddresses;
import io.airlift.slice.Slice;
import io.airlift.slice.XxHash64;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.facebook.presto.spi.StandardErrorCode.GENERIC_INTERNAL_ERROR;
import static com.facebook.presto.spi.StandardErrorCode.INVALID_CAST_ARGUMENT;
import static com.facebook.presto.spi.function.OperatorType.BETWEEN;
import static com.facebook.presto.spi.function.OperatorType.CAST;
import static com.facebook.presto.spi.function.OperatorType.EQUAL;
import static com.facebook.presto.spi.function.OperatorType.GREATER_THAN;
import static com.facebook.presto.spi.function.OperatorType.GREATER_THAN_OR_EQUAL;
import static com.facebook.presto.spi.function.OperatorType.HASH_CODE;
import static com.facebook.presto.spi.function.OperatorType.LESS_THAN;
import static com.facebook.presto.spi.function.OperatorType.LESS_THAN_OR_EQUAL;
import static com.facebook.presto.spi.function.OperatorType.NOT_EQUAL;
import static io.airlift.slice.Slices.utf8Slice;
import static io.airlift.slice.Slices.wrappedBuffer;
import static java.lang.System.arraycopy;

public final class IpAddressOperators
{
    private IpAddressOperators()
    {
    }

    @ScalarOperator(EQUAL)
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean equal(@SqlType(StandardTypes.IPADDRESS) Slice left, @SqlType(StandardTypes.IPADDRESS) Slice right)
    {
        return left.equals(right);
    }

    @ScalarOperator(NOT_EQUAL)
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean notEqual(@SqlType(StandardTypes.IPADDRESS) Slice left, @SqlType(StandardTypes.IPADDRESS) Slice right)
    {
        return !left.equals(right);
    }

    @ScalarOperator(LESS_THAN)
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean lessThan(@SqlType(StandardTypes.IPADDRESS) Slice left, @SqlType(StandardTypes.IPADDRESS) Slice right)
    {
        return left.compareTo(right) < 0;
    }

    @ScalarOperator(LESS_THAN_OR_EQUAL)
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean lessThanOrEqual(@SqlType(StandardTypes.IPADDRESS) Slice left, @SqlType(StandardTypes.IPADDRESS) Slice right)
    {
        return left.compareTo(right) <= 0;
    }

    @ScalarOperator(GREATER_THAN)
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean greaterThan(@SqlType(StandardTypes.IPADDRESS) Slice left, @SqlType(StandardTypes.IPADDRESS) Slice right)
    {
        return left.compareTo(right) > 0;
    }

    @ScalarOperator(GREATER_THAN_OR_EQUAL)
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean greaterThanOrEqual(@SqlType(StandardTypes.IPADDRESS) Slice left, @SqlType(StandardTypes.IPADDRESS) Slice right)
    {
        return left.compareTo(right) >= 0;
    }

    @ScalarOperator(BETWEEN)
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean between(@SqlType(StandardTypes.IPADDRESS) Slice value, @SqlType(StandardTypes.IPADDRESS) Slice min, @SqlType(StandardTypes.IPADDRESS) Slice max)
    {
        return min.compareTo(value) <= 0 && value.compareTo(max) <= 0;
    }

    @ScalarOperator(HASH_CODE)
    @SqlType(StandardTypes.BIGINT)
    public static long hashCode(@SqlType(StandardTypes.IPADDRESS) Slice value)
    {
        return XxHash64.hash(value);
    }

    @LiteralParameters("x")
    @ScalarOperator(CAST)
    @SqlType(StandardTypes.IPADDRESS)
    public static Slice castFromVarcharToIpAddress(@SqlType("varchar(x)") Slice slice)
    {
        byte[] address;
        try {
            address = InetAddresses.forString(slice.toStringUtf8()).getAddress();
        }
        catch (IllegalArgumentException e) {
            throw new PrestoException(INVALID_CAST_ARGUMENT, "Cannot cast value to IPADDRESS: " + slice.toStringUtf8());
        }

        byte[] bytes;
        if (address.length == 4) {
            bytes = new byte[16];
            bytes[10] = (byte) 0xff;
            bytes[11] = (byte) 0xff;
            arraycopy(address, 0, bytes, 12, 4);
        }
        else if (address.length == 16) {
            bytes = address;
        }
        else {
            throw new PrestoException(GENERIC_INTERNAL_ERROR, "Invalid InetAddress length: " + address.length);
        }

        return wrappedBuffer(bytes);
    }

    @ScalarOperator(CAST)
    @SqlType(StandardTypes.VARCHAR)
    public static Slice castFromIpAddressToVarchar(@SqlType(StandardTypes.IPADDRESS) Slice slice)
    {
        try {
            return utf8Slice(InetAddresses.toAddrString(InetAddress.getByAddress(slice.getBytes())));
        }
        catch (UnknownHostException e) {
            throw new PrestoException(INVALID_CAST_ARGUMENT, "Invalid IP address binary length: " + slice.length(), e);
        }
    }
}
