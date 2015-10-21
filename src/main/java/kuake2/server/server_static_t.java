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

package kuake2.server;

import kuake2.Defines;
import kuake2.game.entity_state_t;
import kuake2.qcommon.sizebuf_t;

import java.io.RandomAccessFile;

public class server_static_t {
    boolean initialized; // sv_init has completed
    int realtime; // always increasing, no clamping, etc
    String mapcmd = ""; // ie: *intro.cin+base
    int spawncount; // incremented each server start
    client_t clients[]; // [maxclients->value];

    // used to check late spawns
    int num_client_entities; // maxclients->value*UPDATE_BACKUP*MAX_PACKET_ENTITIES
    int next_client_entities; // next client_entity to use
    entity_state_t client_entities[]; // [num_client_entities]
    int last_heartbeat;
    challenge_t challenges[] = new challenge_t[Defines.MAX_CHALLENGES]; // to
    // serverrecord values
    RandomAccessFile demofile;
    // prevent
    // invalid
    // IPs
    // from
    // connecting
    sizebuf_t demo_multicast = new sizebuf_t();
    byte demo_multicast_buf[] = new byte[Defines.MAX_MSGLEN];

    public server_static_t() {
        for (int n = 0; n < Defines.MAX_CHALLENGES; n++) {
            challenges[n] = new challenge_t();
        }
    }
}