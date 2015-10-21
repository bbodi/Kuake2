/*
 * Copyright (C) 1997-2001 Id Software, Inc.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package kuake2.sys;

import kuake2.Defines;
import kuake2.Globals;
import kuake2.game.cvar_t;
import kuake2.qcommon.Com;
import kuake2.qcommon.Cvar;
import kuake2.qcommon.netadr_t;
import kuake2.qcommon.sizebuf_t;
import kuake2.util.Lib;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public final class NET {

    private final static int MAX_LOOPBACK = 4;
    public static loopback_t loopbacks[] = new loopback_t[2];
    /**
     * Local loopback adress.
     */
    private static netadr_t net_local_adr = new netadr_t();

    ;
    private static DatagramChannel[] ip_channels = {null, null};

    ;
    private static DatagramSocket[] ip_sockets = {null, null};

    static {
        loopbacks[0] = new loopback_t();
        loopbacks[1] = new loopback_t();
    }

    /**
     * Compares ip address and port.
     */
    public static boolean CompareAdr(netadr_t a, netadr_t b) {
        return (a.ip[0] == b.ip[0] && a.ip[1] == b.ip[1] && a.ip[2] == b.ip[2]
                && a.ip[3] == b.ip[3] && a.port == b.port);
    }

    /**
     * Compares ip address without the port.
     */
    public static boolean CompareBaseAdr(netadr_t a, netadr_t b) {
        if (a.type != b.type)
            return false;

        if (a.type == Defines.NA_LOOPBACK)
            return true;

        if (a.type == Defines.NA_IP) {
            return (a.ip[0] == b.ip[0] && a.ip[1] == b.ip[1]
                    && a.ip[2] == b.ip[2] && a.ip[3] == b.ip[3]);
        }
        return false;
    }

    /**
     * Returns a string holding ip address and port like "ip0.ip1.ip2.ip3:port".
     */
    public static String AdrToString(netadr_t a) {
        StringBuffer sb = new StringBuffer();
        sb.append(a.ip[0] & 0xFF).append('.').append(a.ip[1] & 0xFF);
        sb.append('.');
        sb.append(a.ip[2] & 0xFF).append('.').append(a.ip[3] & 0xFF);
        sb.append(':').append(a.port);
        return sb.toString();
    }

    /**
     * Returns IP address without the port as string.
     */
    public static String BaseAdrToString(netadr_t a) {
        StringBuffer sb = new StringBuffer();
        sb.append(a.ip[0] & 0xFF).append('.').append(a.ip[1] & 0xFF);
        sb.append('.');
        sb.append(a.ip[2] & 0xFF).append('.').append(a.ip[3] & 0xFF);
        return sb.toString();
    }

    /**
     * Creates an netadr_t from an string.
     */
    public static boolean StringToAdr(String s, netadr_t a) {
        if (s.equalsIgnoreCase("localhost") || s.equalsIgnoreCase("loopback")) {
            a.set(net_local_adr);
            return true;
        }
        try {
            String[] address = s.split(":");
            InetAddress ia = InetAddress.getByName(address[0]);
            a.ip = ia.getAddress();
            a.type = Defines.NA_IP;
            if (address.length == 2)
                a.port = Lib.atoi(address[1]);
            return true;
        } catch (Exception e) {
            Com.Println(e.getMessage());
            return false;
        }
    }

    /**
     * Seems to return true, if the address is is on 127.0.0.1.
     */
    public static boolean IsLocalAddress(netadr_t adr) {
        return CompareAdr(adr, net_local_adr);
    }

    /**
     * Gets a packet from internal loopback.
     */
    public static boolean GetLoopPacket(int sock, netadr_t net_from,
                                        sizebuf_t net_message) {
        loopback_t loop;
        loop = loopbacks[sock];

        if (loop.send - loop.get > MAX_LOOPBACK)
            loop.get = loop.send - MAX_LOOPBACK;

        if (loop.get >= loop.send)
            return false;

        int i = loop.get & (MAX_LOOPBACK - 1);
        loop.get++;

        System.arraycopy(loop.msgs[i].data, 0, net_message.data, 0,
                loop.msgs[i].datalen);
        net_message.cursize = loop.msgs[i].datalen;

        net_from.set(net_local_adr);
        return true;
    }

    /**
     * Sends a packet via internal loopback.
     */
    public static void SendLoopPacket(int sock, int length, byte[] data,
                                      netadr_t to) {
        int i;
        loopback_t loop;

        loop = loopbacks[sock ^ 1];

        // modulo 4
        i = loop.send & (MAX_LOOPBACK - 1);
        loop.send++;

        System.arraycopy(data, 0, loop.msgs[i].data, 0, length);
        loop.msgs[i].datalen = length;
    }

    /*
     * ==================================================
     * 
     * LOOPBACK BUFFERS FOR LOCAL PLAYER
     * 
     * ==================================================
     */

    /**
     * Gets a packet from a network channel
     */
    public static boolean GetPacket(int sock, netadr_t net_from,
                                    sizebuf_t net_message) {

        if (GetLoopPacket(sock, net_from, net_message)) {
            return true;
        }

        if (ip_sockets[sock] == null)
            return false;

        try {
            ByteBuffer receiveBuffer = ByteBuffer.wrap(net_message.data);

            InetSocketAddress srcSocket = (InetSocketAddress) ip_channels[sock]
                    .receive(receiveBuffer);
            if (srcSocket == null)
                return false;

            net_from.ip = srcSocket.getAddress().getAddress();
            net_from.port = srcSocket.getPort();
            net_from.type = Defines.NA_IP;

            int packetLength = receiveBuffer.position();

            if (packetLength > net_message.maxsize) {
                Com.Println("Oversize packet from " + AdrToString(net_from));
                return false;
            }

            // set the size
            net_message.cursize = packetLength;
            // set the sentinel
            net_message.data[packetLength] = 0;
            return true;

        } catch (IOException e) {
            Com.DPrintf("NET_GetPacket: " + e + " from "
                    + AdrToString(net_from) + "\n");
            return false;
        }
    }

    /**
     * Sends a Packet.
     */
    public static void SendPacket(int sock, int length, byte[] data, netadr_t to) {
        if (to.type == Defines.NA_LOOPBACK) {
            SendLoopPacket(sock, length, data, to);
            return;
        }

        if (ip_sockets[sock] == null)
            return;

        if (to.type != Defines.NA_BROADCAST && to.type != Defines.NA_IP) {
            Com.Error(Defines.ERR_FATAL, "NET_SendPacket: bad address type");
            return;
        }

        try {
            SocketAddress dstSocket = new InetSocketAddress(to.getInetAddress(), to.port);
            ip_channels[sock].send(ByteBuffer.wrap(data, 0, length), dstSocket);
        } catch (Exception e) {
            Com.Println("NET_SendPacket ERROR: " + e + " to " + AdrToString(to));
        }
    }

    /**
     * OpenIP, creates the network sockets.
     */
    public static void OpenIP() {
        cvar_t port, ip, clientport;

        port = Cvar.Get("port", "" + Defines.PORT_SERVER, Defines.CVAR_NOSET);
        ip = Cvar.Get("ip", "localhost", Defines.CVAR_NOSET);
        clientport = Cvar.Get("clientport", "" + Defines.PORT_CLIENT, Defines.CVAR_NOSET);

        if (ip_sockets[Defines.NS_SERVER] == null)
            ip_sockets[Defines.NS_SERVER] = Socket(Defines.NS_SERVER,
                    ip.string, (int) port.value);

        if (ip_sockets[Defines.NS_CLIENT] == null)
            ip_sockets[Defines.NS_CLIENT] = Socket(Defines.NS_CLIENT,
                    ip.string, (int) clientport.value);
        if (ip_sockets[Defines.NS_CLIENT] == null)
            ip_sockets[Defines.NS_CLIENT] = Socket(Defines.NS_CLIENT,
                    ip.string, Defines.PORT_ANY);
    }

    /**
     * Config multi or singlepalyer - A single player game will only use the loopback code.
     */
    public static void Config(boolean multiplayer) {
        if (!multiplayer) {
            // shut down any existing sockets
            for (int i = 0; i < 2; i++) {
                if (ip_sockets[i] != null) {
                    ip_sockets[i].close();
                    ip_sockets[i] = null;
                }
            }
        } else {
            // open sockets
            OpenIP();
        }
    }

    /**
     * Init
     */
    public static void Init() {
        // nothing to do
    }

    /*
     * Socket
     */
    public static DatagramSocket Socket(int sock, String ip, int port) {

        DatagramSocket newsocket = null;
        try {
            if (ip_channels[sock] == null || !ip_channels[sock].isOpen())
                ip_channels[sock] = DatagramChannel.open();

            if (ip == null || ip.length() == 0 || ip.equals("localhost")) {
                if (port == Defines.PORT_ANY) {
                    newsocket = ip_channels[sock].socket();
                    newsocket.bind(new InetSocketAddress(0));
                } else {
                    newsocket = ip_channels[sock].socket();
                    newsocket.bind(new InetSocketAddress(port));
                }
            } else {
                InetAddress ia = InetAddress.getByName(ip);
                newsocket = ip_channels[sock].socket();
                newsocket.bind(new InetSocketAddress(ia, port));
            }

            // nonblocking channel
            ip_channels[sock].configureBlocking(false);
            // the socket have to be broadcastable
            newsocket.setBroadcast(true);
        } catch (Exception e) {
            Com.Println("Error: " + e.toString());
            newsocket = null;
        }
        return newsocket;
    }

    /**
     * Shutdown - closes the sockets
     */
    public static void Shutdown() {
        // close sockets
        Config(false);
    }

    /**
     * Sleeps msec or until net socket is ready.
     */
    public static void Sleep(int msec) {
        if (ip_sockets[Defines.NS_SERVER] == null
                || (Globals.dedicated != null && Globals.dedicated.value == 0))
            return; // we're not a server, just run full speed

        try {
            //TODO: check for timeout
            Thread.sleep(msec);
        } catch (InterruptedException e) {
        }
        //ip_sockets[NS_SERVER].

        // this should wait up to 100ms until a packet
        /*
         * struct timeval timeout;
         * fd_set fdset;
         * extern cvar_t *dedicated;
         * extern qboolean stdin_active;
         *
         * if (!ip_sockets[NS_SERVER] || (dedicated && !dedicated.value))
         * 		return; // we're not a server, just run full speed
         *
         * FD_ZERO(&fdset);
         *
         * if (stdin_active)
         * 		FD_SET(0, &fdset); // stdin is processed too
         *
         * FD_SET(ip_sockets[NS_SERVER], &fdset); // network socket
         *
         * timeout.tv_sec = msec/1000;
         * timeout.tv_usec = (msec%1000)*1000;
         *
         * select(ip_sockets[NS_SERVER]+1, &fdset, NULL, NULL, &timeout);
         */
    }

    public static class loopmsg_t {
        byte data[] = new byte[Defines.MAX_MSGLEN];

        int datalen;
    }

    public static class loopback_t {
        loopmsg_t msgs[];
        int get, send;

        public loopback_t() {
            msgs = new loopmsg_t[MAX_LOOPBACK];
            for (int n = 0; n < MAX_LOOPBACK; n++) {
                msgs[n] = new loopmsg_t();
            }
        }
    }
}