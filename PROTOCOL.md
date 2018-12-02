# Baritone Comms Protocol

## Data Types

| Name       | Descriptor                                                | Java                        |
|------------|-----------------------------------------------------------|-----------------------------|
| coordinate | Big endian 8-byte floating point number                   | [readDouble], [writeDouble] |
| string     | unsigned short (length) followed by UTF-8 character bytes | [readUTF],    [writeUTF]    |

## Inbound

Allows the server to execute a chat command on behalf of the client's player

### Chat

| Name    | Type   |
|---------|--------|
| Message | string |

## Outbound

Update the player position with the server

### Status

| Name | Type       |
|------|------------|
| X    | coordinate |
| Y    | coordinate |
| Z    | coordinate |

<!-- External links -->
[readUTF]:     https://docs.oracle.com/javase/7/docs/api/java/io/DataInputStream.html#readUTF()
[writeUTF]:    https://docs.oracle.com/javase/7/docs/api/java/io/DataOutputStream.html#writeUTF(java.lang.String)
[readDouble]:  https://docs.oracle.com/javase/7/docs/api/java/io/DataInputStream.html#readDouble()
[writeDouble]: https://docs.oracle.com/javase/7/docs/api/java/io/DataOutputStream.html#writeDouble(double)